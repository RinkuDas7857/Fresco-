/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageutils;

import static com.facebook.imageutils.JfifTestUtils.*;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

/** Tests {@link JfifUtil} */
@RunWith(RobolectricTestRunner.class)
public class JfifUtilTest {

  // Test cases without APP1 block
  final String NO_ORI_IMAGE_1 = SOI + APP0 + APP2 + DQT + DHT + DRI + SOF + SOS + EOI;
  final String NO_ORI_IMAGE_2 = SOI + DQT + DHT + SOF + SOS + EOI;

  @Test
  public void testGetOrientation_NoAPP1() {
    assertEquals(0, JfifUtil.getOrientation(hexStringToByteArray(NO_ORI_IMAGE_1)));
    assertEquals(0, JfifUtil.getOrientation(hexStringToByteArray(NO_ORI_IMAGE_2)));
  }

  @Test
  public void testGetOrientation_BigEndian() {
    testGetOrientation_WithEndian(false);
  }

  @Test
  public void testGetOrientation_LittleEndian() {
    testGetOrientation_WithEndian(true);
  }

  private void testGetOrientation_WithEndian(boolean littleEnd) {
    final String IFD_ENTRY_1 = makeIfdEntry(IFD_ENTRY_TAG_1, TYPE_SHORT, 1, 255, 2, littleEnd);
    final String IFD_ENTRY_2 = makeIfdEntry(IFD_ENTRY_TAG_2, TYPE_SHORT, 1, 255, 2, littleEnd);
    final String IFD_ENTRY_3 = makeIfdEntry(IFD_ENTRY_TAG_3, TYPE_SHORT, 1, 255, 2, littleEnd);
    final String TIFF_IFD_0 =
        makeIfd(new String[] {IFD_ENTRY_1, IFD_ENTRY_2, IFD_ENTRY_3}, 0, littleEnd);
    final String TIFF_IFD_1 =
        makeIfd(
            new String[] {makeOrientationEntry(1, littleEnd), IFD_ENTRY_1, IFD_ENTRY_2},
            0,
            littleEnd);
    final String TIFF_IFD_3 =
        makeIfd(
            new String[] {makeOrientationEntry(3, littleEnd), IFD_ENTRY_1, IFD_ENTRY_2},
            0,
            littleEnd);
    final String TIFF_IFD_6A =
        makeIfd(
            new String[] {makeOrientationEntry(6, littleEnd), IFD_ENTRY_1, IFD_ENTRY_2},
            0,
            littleEnd);
    final String TIFF_IFD_6B =
        makeIfd(
            new String[] {IFD_ENTRY_1, makeOrientationEntry(6, littleEnd), IFD_ENTRY_2},
            0,
            littleEnd);
    final String TIFF_IFD_6C =
        makeIfd(
            new String[] {IFD_ENTRY_1, IFD_ENTRY_2, makeOrientationEntry(6, littleEnd)},
            0,
            littleEnd);
    final String TIFF_IFD_8 =
        makeIfd(
            new String[] {makeOrientationEntry(8, littleEnd), IFD_ENTRY_1, IFD_ENTRY_2},
            0,
            littleEnd);

    final String APP1_0 = makeAPP1_EXIF(makeTiff(TIFF_IFD_0, littleEnd));
    final String APP1_1 = makeAPP1_EXIF(makeTiff(TIFF_IFD_1, littleEnd));
    final String APP1_3 = makeAPP1_EXIF(makeTiff(TIFF_IFD_3, littleEnd));
    final String APP1_6A = makeAPP1_EXIF(makeTiff(TIFF_IFD_6A, littleEnd));
    final String APP1_6B = makeAPP1_EXIF(makeTiff(TIFF_IFD_6B, littleEnd));
    final String APP1_6C = makeAPP1_EXIF(makeTiff(TIFF_IFD_6C, littleEnd));
    final String APP1_8 = makeAPP1_EXIF(makeTiff(TIFF_IFD_8, littleEnd));

    assertEquals(0, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_0))));
    assertEquals(1, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_1))));
    assertEquals(3, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_3))));
    assertEquals(6, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_6A))));
    assertEquals(6, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_6B))));
    assertEquals(6, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_6C))));
    assertEquals(8, JfifUtil.getOrientation(hexStringToByteArray(makeTestImageWithAPP1(APP1_8))));

    testGetOrientation_VariousAPP1Location(APP1_3, 3);
  }

  private void testGetOrientation_VariousAPP1Location(String APP1, int expectOri) {
    final String IMAGE_WITH_STRUCT_1 = SOI + APP1 + DQT + DHT + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_2 = SOI + DQT + APP1 + DHT + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_3 = SOI + DQT + DHT + APP1 + DRI + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_4 = SOI + DQT + DHT + DRI + APP1 + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_5 = SOI + DQT + DHT + DRI + SOF + APP1 + SOS + EOI;
    final String IMAGE_WITH_STRUCT_6 = SOI + DQT + DHT + SOF + APP1 + SOS + EOI;
    final String IMAGE_WITH_STRUCT_7 = SOI + APP0 + APP2 + APP1 + DQT + DHT + DRI + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_8 = SOI + APP0 + APP1 + APP2 + DQT + DHT + DRI + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_9 = SOI + APP1 + APP2 + DQT + DHT + DRI + SOF + SOS + EOI;
    final String IMAGE_WITH_STRUCT_10 = SOI + APP1 + SOS + EOI;
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_1)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_2)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_3)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_4)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_5)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_6)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_7)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_8)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_9)));
    assertEquals(expectOri, JfifUtil.getOrientation(hexStringToByteArray(IMAGE_WITH_STRUCT_10)));
  }
}
