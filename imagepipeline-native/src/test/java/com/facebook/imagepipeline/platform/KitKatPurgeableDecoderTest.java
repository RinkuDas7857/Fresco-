/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.common.TooManyBitmapsException;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapCounter;
import com.facebook.imagepipeline.memory.BitmapCounterProvider;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.nativecode.Bitmaps;
import com.facebook.imagepipeline.nativecode.DalvikPurgeableDecoder;
import com.facebook.imagepipeline.testing.MockBitmapFactory;
import com.facebook.imagepipeline.testing.TestNativeLoader;
import com.facebook.imagepipeline.testing.TrivialPooledByteBuffer;
import java.util.ConcurrentModificationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/** Tests for {@link KitKatPurgeableDecoder}. */
@RunWith(RobolectricTestRunner.class)
@PrepareOnlyThisForTest({
  BitmapCounterProvider.class,
  BitmapFactory.class,
  DalvikPurgeableDecoder.class,
  Bitmaps.class
})
@Config(sdk = Build.VERSION_CODES.KITKAT)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
public class KitKatPurgeableDecoderTest {

  protected static final Bitmap.Config DEFAULT_BITMAP_CONFIG = Bitmap.Config.ARGB_8888;
  protected FlexByteArrayPool mFlexByteArrayPool;

  @Rule public PowerMockRule rule = new PowerMockRule();

  static {
    TestNativeLoader.init();
  }

  protected static final int IMAGE_SIZE = 5;
  protected static final int LENGTH = 10;
  protected static final long POINTER = 1000L;
  protected static final int MAX_BITMAP_COUNT = 2;
  protected static final int MAX_BITMAP_SIZE =
      MAX_BITMAP_COUNT * MockBitmapFactory.DEFAULT_BITMAP_SIZE;

  protected KitKatPurgeableDecoder mKitKatPurgeableDecoder;
  protected CloseableReference<PooledByteBuffer> mByteBufferRef;
  protected EncodedImage mEncodedImage;
  protected byte[] mInputBuf;
  protected byte[] mDecodeBuf;
  protected CloseableReference<byte[]> mDecodeBufRef;
  protected Bitmap mBitmap;
  protected BitmapCounter mBitmapCounter;

  @Before
  public void setUp() {
    mFlexByteArrayPool = mock(FlexByteArrayPool.class);

    mBitmap = MockBitmapFactory.create();
    mBitmapCounter = new BitmapCounter(MAX_BITMAP_COUNT, MAX_BITMAP_SIZE);

    mockStatic(DalvikPurgeableDecoder.class);
    when(DalvikPurgeableDecoder.getBitmapFactoryOptions(anyInt(), any(Bitmap.Config.class)))
        .thenCallRealMethod();
    when(DalvikPurgeableDecoder.endsWithEOI(any(CloseableReference.class), anyInt()))
        .thenCallRealMethod();
    mockStatic(BitmapCounterProvider.class);
    when(BitmapCounterProvider.get()).thenReturn(mBitmapCounter);

    mockStatic(BitmapFactory.class);
    when(BitmapFactory.decodeByteArray(
            any(byte[].class), anyInt(), anyInt(), any(BitmapFactory.Options.class)))
        .thenReturn(mBitmap);

    mInputBuf = new byte[LENGTH];
    PooledByteBuffer input = new TrivialPooledByteBuffer(mInputBuf, POINTER);
    mByteBufferRef = CloseableReference.of(input);
    mEncodedImage = new EncodedImage(mByteBufferRef);

    mDecodeBuf = new byte[LENGTH + 2];
    mDecodeBufRef = CloseableReference.of(mDecodeBuf, mock(ResourceReleaser.class));
    when(mFlexByteArrayPool.get(Integer.valueOf(LENGTH))).thenReturn(mDecodeBufRef);

    mockStatic(Bitmaps.class);
    mKitKatPurgeableDecoder = new KitKatPurgeableDecoder(mFlexByteArrayPool);
  }

  @Test
  public void testDecode_Jpeg_Detailed() {
    assumeNotNull(mKitKatPurgeableDecoder);
    setUpJpegDecode();
    CloseableReference<Bitmap> result =
        mKitKatPurgeableDecoder.decodeJPEGFromEncodedImage(
            mEncodedImage, DEFAULT_BITMAP_CONFIG, null, IMAGE_SIZE);
    verify(mFlexByteArrayPool).get(IMAGE_SIZE + 2);
    verifyStatic(BitmapFactory.class);
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf), eq(0), eq(IMAGE_SIZE), argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
  }

  @Test
  public void testDecodeJpeg_incomplete() {
    assumeNotNull(mKitKatPurgeableDecoder);
    when(mFlexByteArrayPool.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
    CloseableReference<Bitmap> result =
        mKitKatPurgeableDecoder.decodeJPEGFromEncodedImage(
            mEncodedImage, DEFAULT_BITMAP_CONFIG, null, IMAGE_SIZE);
    verify(mFlexByteArrayPool).get(IMAGE_SIZE + 2);
    verifyStatic(BitmapFactory.class);
    BitmapFactory.decodeByteArray(
        same(mDecodeBuf), eq(0), eq(IMAGE_SIZE + 2), argThat(new BitmapFactoryOptionsMatcher()));
    assertEquals((byte) 0xff, mDecodeBuf[5]);
    assertEquals((byte) 0xd9, mDecodeBuf[6]);
    assertEquals(2, mByteBufferRef.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(mBitmap, result.get());
    assertTrue(result.isValid());
    assertEquals(1, mBitmapCounter.getCount());
    assertEquals(MockBitmapFactory.DEFAULT_BITMAP_SIZE, mBitmapCounter.getSize());
  }

  @Test(expected = TooManyBitmapsException.class)
  public void testHitBitmapLimit_static() {
    assumeNotNull(mKitKatPurgeableDecoder);
    mBitmapCounter.increase(
        MockBitmapFactory.createForSize(MAX_BITMAP_SIZE, DEFAULT_BITMAP_CONFIG));
    try {
      mKitKatPurgeableDecoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(1, mBitmapCounter.getCount());
      assertEquals(MAX_BITMAP_SIZE, mBitmapCounter.getSize());
    }
  }

  @Test(expected = ConcurrentModificationException.class)
  public void testPinBitmapFailure() {
    KitKatPurgeableDecoder decoder = spy(mKitKatPurgeableDecoder);
    doThrow(new ConcurrentModificationException()).when(decoder).pinBitmap((Bitmap) anyObject());
    decoder.pinBitmap(any(Bitmap.class));
    try {
      decoder.decodeFromEncodedImage(mEncodedImage, DEFAULT_BITMAP_CONFIG, null);
    } finally {
      verify(mBitmap).recycle();
      assertEquals(0, mBitmapCounter.getCount());
      assertEquals(0, mBitmapCounter.getSize());
    }
  }

  private void setUpJpegDecode() {
    mInputBuf[3] = (byte) 0xff;
    mInputBuf[4] = (byte) 0xd9;
    when(mFlexByteArrayPool.get(IMAGE_SIZE + 2)).thenReturn(mDecodeBufRef);
  }

  private static class BitmapFactoryOptionsMatcher
      implements ArgumentMatcher<BitmapFactory.Options> {
    @Override
    public boolean matches(BitmapFactory.Options options) {
      return options.inDither && options.inPurgeable;
    }
  }
}
