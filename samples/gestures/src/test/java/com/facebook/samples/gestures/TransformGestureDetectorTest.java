/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.gestures;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.view.MotionEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests for {@link MultiPointerGestureDetector} */
@RunWith(RobolectricTestRunner.class)
public class TransformGestureDetectorTest {

  private TransformGestureDetector.Listener mListener;
  private MultiPointerGestureDetector mMultiPointerGestureDetector;
  private TransformGestureDetector mGestureDetector;

  @Before
  public void setup() {
    mListener = mock(TransformGestureDetector.Listener.class);
    mMultiPointerGestureDetector = mock(MultiPointerGestureDetector.class);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    when(mMultiPointerGestureDetector.getStartX()).thenReturn(new float[] {100f, 200f});
    when(mMultiPointerGestureDetector.getStartY()).thenReturn(new float[] {500f, 600f});
    when(mMultiPointerGestureDetector.getCurrentX()).thenReturn(new float[] {10f, 20f});
    when(mMultiPointerGestureDetector.getCurrentY()).thenReturn(new float[] {50f, 40f});
    mGestureDetector = new TransformGestureDetector(mMultiPointerGestureDetector);
    mGestureDetector.setListener(mListener);
  }

  @Test
  public void testInitialstate() {
    assertEquals(false, mGestureDetector.isGestureInProgress());
    verify(mMultiPointerGestureDetector).setListener(mGestureDetector);
  }

  @Test
  public void testReset() {
    mGestureDetector.reset();
    verify(mMultiPointerGestureDetector).reset();
  }

  @Test
  public void testOnTouchEvent() {
    MotionEvent motionEvent = mock(MotionEvent.class);
    mGestureDetector.onTouchEvent(motionEvent);
    verify(mMultiPointerGestureDetector).onTouchEvent(motionEvent);
  }

  @Test
  public void testOnGestureBegin() {
    mGestureDetector.onGestureBegin(mMultiPointerGestureDetector);
    verify(mListener).onGestureBegin(mGestureDetector);
  }

  @Test
  public void testOnGestureUpdate() {
    mGestureDetector.onGestureUpdate(mMultiPointerGestureDetector);
    verify(mListener).onGestureUpdate(mGestureDetector);
  }

  @Test
  public void testOnGestureEnd() {
    mGestureDetector.onGestureEnd(mMultiPointerGestureDetector);
    verify(mListener).onGestureEnd(mGestureDetector);
  }

  @Test
  public void testIsGestureInProgress() {
    when(mMultiPointerGestureDetector.isGestureInProgress()).thenReturn(true);
    assertEquals(true, mGestureDetector.isGestureInProgress());
    verify(mMultiPointerGestureDetector, times(1)).isGestureInProgress();
    when(mMultiPointerGestureDetector.isGestureInProgress()).thenReturn(false);
    assertEquals(false, mGestureDetector.isGestureInProgress());
    verify(mMultiPointerGestureDetector, times(2)).isGestureInProgress();
  }

  @Test
  public void testPivot() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertEquals(0, mGestureDetector.getPivotX(), 0);
    assertEquals(0, mGestureDetector.getPivotY(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertEquals(100, mGestureDetector.getPivotX(), 0);
    assertEquals(500, mGestureDetector.getPivotY(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertEquals(150, mGestureDetector.getPivotX(), 0);
    assertEquals(550, mGestureDetector.getPivotY(), 0);
  }

  @Test
  public void testTranslation() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertEquals(0, mGestureDetector.getTranslationX(), 0);
    assertEquals(0, mGestureDetector.getTranslationY(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertEquals(-90, mGestureDetector.getTranslationX(), 0);
    assertEquals(-450, mGestureDetector.getTranslationY(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertEquals(-135, mGestureDetector.getTranslationX(), 0);
    assertEquals(-505, mGestureDetector.getTranslationY(), 0);
  }

  @Test
  public void testScale() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertEquals(1, mGestureDetector.getScale(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertEquals(1, mGestureDetector.getScale(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertEquals(0.1f, mGestureDetector.getScale(), 1e-6);
  }

  @Test
  public void testRotation() {
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(0);
    assertEquals(0, mGestureDetector.getRotation(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(1);
    assertEquals(0, mGestureDetector.getRotation(), 0);
    when(mMultiPointerGestureDetector.getPointerCount()).thenReturn(2);
    assertEquals((float) -Math.PI / 2, mGestureDetector.getRotation(), 1e-6);
  }
}
