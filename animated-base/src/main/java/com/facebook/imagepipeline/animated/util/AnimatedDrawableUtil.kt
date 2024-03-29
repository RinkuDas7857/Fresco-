/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.animated.util

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import java.util.Arrays

/** Utility methods for AnimatedDrawable. */
class AnimatedDrawableUtil {

  /**
   * Adjusts the frame duration array to respect logic for minimum frame duration time.
   *
   * @param frameDurationMs the frame duration array
   */
  fun fixFrameDurations(frameDurationMs: IntArray) {
    // We follow Chrome's behavior which comes from Firefox.
    // Comment from Chrome's ImageSource.cpp follows:
    // We follow Firefox's behavior and use a duration of 100 ms for any frames that specify
    // a duration of <= 10 ms. See <rdar://problem/7689300> and <http://webkit.org/b/36082>
    // for more information.
    for (i in frameDurationMs.indices) {
      if (frameDurationMs[i] < MIN_FRAME_DURATION_MS) {
        frameDurationMs[i] = FRAME_DURATION_MS_FOR_MIN
      }
    }
  }

  /**
   * Gets the total duration of an image by summing up the duration of the frames.
   *
   * @param frameDurationMs the frame duration array
   * @return the total duration in milliseconds
   */
  fun getTotalDurationFromFrameDurations(frameDurationMs: IntArray): Int {
    var totalMs = 0
    for (i in frameDurationMs.indices) {
      totalMs += frameDurationMs[i]
    }
    return totalMs
  }

  /**
   * Given an array of frame durations, generate an array of timestamps corresponding to when each
   * frame beings.
   *
   * @param frameDurationsMs an array of frame durations
   * @return an array of timestamps
   */
  fun getFrameTimeStampsFromDurations(frameDurationsMs: IntArray): IntArray {
    val frameTimestampsMs = IntArray(frameDurationsMs.size)
    var accumulatedDurationMs = 0
    for (i in frameDurationsMs.indices) {
      frameTimestampsMs[i] = accumulatedDurationMs
      accumulatedDurationMs += frameDurationsMs[i]
    }
    return frameTimestampsMs
  }

  /**
   * Gets the frame index for specified timestamp.
   *
   * @param frameTimestampsMs an array of timestamps generated by [getFrameForTimestampMs)]
   * @param timestampMs the timestamp
   * @return the frame index for the timestamp or the last frame number if the timestamp is outside
   *   the duration of the entire animation
   */
  fun getFrameForTimestampMs(frameTimestampsMs: IntArray?, timestampMs: Int): Int {
    val index = Arrays.binarySearch(frameTimestampsMs, timestampMs)
    return if (index < 0) {
      -index - 1 - 1
    } else {
      index
    }
  }

  @SuppressLint("NewApi")
  fun getSizeOfBitmap(bitmap: Bitmap): Int =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        bitmap.allocationByteCount
      } else {
        bitmap.byteCount
      }

  companion object {
    // See comment in fixFrameDurations below.
    private const val MIN_FRAME_DURATION_MS = 11
    private const val FRAME_DURATION_MS_FOR_MIN = 100

    /**
     * Checks whether the specified frame number is outside the range inclusive of both start and
     * end. If start <= end, start is within, end is within, and everything in between is within. If
     * start
     * > end, start is within, end is within, everything less than start is within and everything
     * > greater than end is within. This behavior is useful for handling the wrapping case.
     *
     * @param startFrame the start frame
     * @param endFrame the end frame
     * @param frameNumber the frame number
     * @return whether the frame is outside the range of [start, end]
     */
    @JvmStatic
    fun isOutsideRange(startFrame: Int, endFrame: Int, frameNumber: Int): Boolean {
      if (startFrame == -1 || endFrame == -1) {
        // This means nothing should pass.
        return true
      }
      val outsideRange =
          if (startFrame <= endFrame) {
            frameNumber < startFrame || frameNumber > endFrame
          } else {
            // Wrapping
            frameNumber < startFrame && frameNumber > endFrame
          }
      return outsideRange
    }
  }
}
