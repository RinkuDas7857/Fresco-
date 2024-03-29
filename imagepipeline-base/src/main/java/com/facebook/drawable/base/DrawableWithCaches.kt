/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawable.base

/**
 * A drawable can implement this fun interface to be notified when it might be convenient to drop
 * its caches in order conserve memory. This is best effort and the Drawable should not depend on it
 * being called.
 */
interface DrawableWithCaches {
  /** Informs the Drawable to drop its caches. */
  fun dropCaches()
}
