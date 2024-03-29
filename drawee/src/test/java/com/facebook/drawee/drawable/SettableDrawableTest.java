/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import static org.mockito.Mockito.*;

import android.graphics.drawable.Drawable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SettableDrawableTest {
  private Drawable mUnderlyingDrawable0;
  private Drawable mUnderlyingDrawable1;
  private Drawable mUnderlyingDrawable2;
  private Drawable mUnderlyingDrawable3;
  private ForwardingDrawable mSettableDrawable;

  @Before
  public void setUp() {
    mUnderlyingDrawable0 = mock(Drawable.class);
    mUnderlyingDrawable1 = mock(Drawable.class);
    mUnderlyingDrawable2 = mock(Drawable.class);
    mUnderlyingDrawable3 = mock(Drawable.class);
    mSettableDrawable = new ForwardingDrawable(mUnderlyingDrawable0);
  }

  @Test
  public void testIntrinsicDimensions() {
    when(mUnderlyingDrawable0.getIntrinsicWidth()).thenReturn(100);
    when(mUnderlyingDrawable0.getIntrinsicHeight()).thenReturn(200);
    when(mUnderlyingDrawable1.getIntrinsicWidth()).thenReturn(300);
    when(mUnderlyingDrawable1.getIntrinsicHeight()).thenReturn(400);
    Assert.assertEquals(100, mSettableDrawable.getIntrinsicWidth());
    Assert.assertEquals(200, mSettableDrawable.getIntrinsicHeight());
    mSettableDrawable.setDrawable(mUnderlyingDrawable1);
    Assert.assertEquals(300, mSettableDrawable.getIntrinsicWidth());
    Assert.assertEquals(400, mSettableDrawable.getIntrinsicHeight());
  }

  @Test
  public void testGetCurrent() {
    // initial drawable is mUnderlyingDrawable0
    Assert.assertEquals(mUnderlyingDrawable0, mSettableDrawable.getCurrent());
    mSettableDrawable.setDrawable(mUnderlyingDrawable1);
    Assert.assertEquals(mUnderlyingDrawable1, mSettableDrawable.getCurrent());
    mSettableDrawable.setDrawable(mUnderlyingDrawable2);
    Assert.assertEquals(mUnderlyingDrawable2, mSettableDrawable.getCurrent());
    mSettableDrawable.setDrawable(mUnderlyingDrawable3);
    Assert.assertEquals(mUnderlyingDrawable3, mSettableDrawable.getCurrent());
  }

  @Test
  public void testSetCurrent() {
    Drawable.Callback callback = mock(Drawable.Callback.class);
    mSettableDrawable.setCallback(callback);
    mSettableDrawable.setDrawable(mUnderlyingDrawable1);
    verify(mUnderlyingDrawable0).setCallback(null);
    verify(mUnderlyingDrawable1).setCallback(isNotNull(Drawable.Callback.class));
    verify(callback).invalidateDrawable(mSettableDrawable);
  }
}
