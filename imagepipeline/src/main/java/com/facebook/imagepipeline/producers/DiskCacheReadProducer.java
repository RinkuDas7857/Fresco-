/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;

import bolts.Continuation;
import bolts.Task;

/**
 * Disk cache read producer.
 *
 * <p>This producer looks in the disk cache for the requested image. If the image is found, then it
 * is passed to the consumer. If the image is not found, then the request is passed to the next
 * producer in the sequence. Any results that the producer returns are passed to the consumer.
 *
 * <p>This implementation delegates disk cache requests to BufferedDiskCache.
 *
 * <p>This producer is currently used only if the media variations experiment is turned on, to
 * enable another producer to sit between cache read and write.
 */
public class DiskCacheReadProducer implements Producer<EncodedImage> {
  // PRODUCER_NAME doesn't exactly match class name as it matches name in historic data instead
  @VisibleForTesting static final String PRODUCER_NAME = "DiskCacheProducer";
  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;

  private final BufferedDiskCache mDefaultBufferedDiskCache;
  private final BufferedDiskCache mSmallImageBufferedDiskCache;
  private final CacheKeyFactory mCacheKeyFactory;
  private final Producer<EncodedImage> mInputProducer;
  private final boolean mChooseCacheByImageSize;

  public DiskCacheReadProducer(
      BufferedDiskCache defaultBufferedDiskCache,
      BufferedDiskCache smallImageBufferedDiskCache,
      CacheKeyFactory cacheKeyFactory,
      Producer<EncodedImage> inputProducer,
      int forceSmallCacheThresholdBytes) {
    mDefaultBufferedDiskCache = defaultBufferedDiskCache;
    mSmallImageBufferedDiskCache = smallImageBufferedDiskCache;
    mCacheKeyFactory = cacheKeyFactory;
    mInputProducer = inputProducer;
    mChooseCacheByImageSize = (forceSmallCacheThresholdBytes > 0);
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    ImageRequest imageRequest = producerContext.getImageRequest();
    if (!imageRequest.isDiskCacheEnabled()) {
      maybeStartInputProducer(consumer, producerContext);
      return;
    }

    producerContext.getListener().onProducerStart(producerContext.getId(), PRODUCER_NAME);

    final CacheKey cacheKey =
        mCacheKeyFactory.getEncodedCacheKey(imageRequest, producerContext.getCallerContext());
    boolean isSmallRequest = (imageRequest.getCacheChoice() == ImageRequest.CacheChoice.SMALL);
    final BufferedDiskCache preferredCache = isSmallRequest ?
        mSmallImageBufferedDiskCache : mDefaultBufferedDiskCache;
    final AtomicBoolean isCancelled = new AtomicBoolean(false);
    Task<EncodedImage> diskLookupTask;
    if (mChooseCacheByImageSize) {
      boolean alreadyInSmall = mSmallImageBufferedDiskCache.containsSync(cacheKey);
      boolean alreadyInMain = mDefaultBufferedDiskCache.containsSync(cacheKey);
      final BufferedDiskCache firstCache;
      final BufferedDiskCache secondCache ;
      if (alreadyInSmall || !alreadyInMain) {
        firstCache = mSmallImageBufferedDiskCache;
        secondCache = mDefaultBufferedDiskCache;
      } else {
        firstCache = mDefaultBufferedDiskCache;
        secondCache = mSmallImageBufferedDiskCache;
      }
      diskLookupTask = firstCache.get(cacheKey, isCancelled);
      diskLookupTask = diskLookupTask.continueWithTask(
          new Continuation<EncodedImage, Task<EncodedImage>>() {
        @Override
        public Task<EncodedImage> then(Task<EncodedImage> task) throws Exception {
          if (isTaskCancelled(task) || (!task.isFaulted() && task.getResult() != null)) {
            return task;
          }
          return secondCache.get(cacheKey, isCancelled);
        }
      });
    } else {
      diskLookupTask = preferredCache.get(cacheKey, isCancelled);
    }
    Continuation<EncodedImage, Void> continuation =
        onFinishDiskReads(consumer, producerContext);
    diskLookupTask.continueWith(continuation);
    subscribeTaskForRequestCancellation(isCancelled, producerContext);
  }

  private Continuation<EncodedImage, Void> onFinishDiskReads(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext) {
    final String requestId = producerContext.getId();
    final ProducerListener listener = producerContext.getListener();
    return new Continuation<EncodedImage, Void>() {
      @Override
      public Void then(Task<EncodedImage> task)
          throws Exception {
        if (isTaskCancelled(task)) {
          listener.onProducerFinishWithCancellation(requestId, PRODUCER_NAME, null);
          consumer.onCancellation();
        } else if (task.isFaulted()) {
          listener.onProducerFinishWithFailure(requestId, PRODUCER_NAME, task.getError(), null);
          mInputProducer.produceResults(consumer, producerContext);
        } else {
          EncodedImage cachedReference = task.getResult();
          if (cachedReference != null) {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, true));
            consumer.onProgressUpdate(1);
            consumer.onNewResult(cachedReference, true);
            cachedReference.close();
          } else {
            listener.onProducerFinishWithSuccess(
                requestId,
                PRODUCER_NAME,
                getExtraMap(listener, requestId, false));
            mInputProducer.produceResults(consumer, producerContext);
          }
        }
        return null;
      }
    };
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled() ||
        (task.isFaulted() && task.getError() instanceof CancellationException);
  }

  private void maybeStartInputProducer(
      Consumer<EncodedImage> consumer,
      ProducerContext producerContext) {
    if (producerContext.getLowestPermittedRequestLevel().getValue() >=
        ImageRequest.RequestLevel.DISK_CACHE.getValue()) {
      consumer.onNewResult(null, true);
      return;
    }

    mInputProducer.produceResults(consumer, producerContext);
  }

  @VisibleForTesting
  static Map<String, String> getExtraMap(
      final ProducerListener listener,
      final String requestId,
      final boolean valueFound) {
    if (!listener.requiresExtraMap(requestId)) {
      return null;
    }
    return ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, String.valueOf(valueFound));
  }

  private void subscribeTaskForRequestCancellation(
      final AtomicBoolean isCancelled,
      ProducerContext producerContext) {
    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            isCancelled.set(true);
          }
        });
  }
}
