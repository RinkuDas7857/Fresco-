/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.comparison.configs.imagepipeline;

import android.content.Context;
import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.common.internal.Supplier;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.cache.MemoryCacheParams;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.listener.RequestLoggingListener;
import com.facebook.samples.comparison.configs.ConfigConstants;
import com.facebook.stetho.okhttp3.StethoInterceptor;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;

/** Creates ImagePipeline configuration for the sample app */
public class ImagePipelineConfigFactory {
  private static final String IMAGE_PIPELINE_CACHE_DIR = "imagepipeline_cache";

  private static ImagePipelineConfig sImagePipelineConfig;
  private static ImagePipelineConfig sOkHttpImagePipelineConfig;

  /** Creates config using android http stack as network backend. */
  public static ImagePipelineConfig getImagePipelineConfig(Context context) {
    if (sImagePipelineConfig == null) {
      ImagePipelineConfig.Builder configBuilder = ImagePipelineConfig.newBuilder(context);
      configureCaches(configBuilder, context);
      configureLoggingListeners(configBuilder);
      configureOptions(configBuilder);
      sImagePipelineConfig = configBuilder.build();
    }
    return sImagePipelineConfig;
  }

  /** Creates config using OkHttp as network backed. */
  public static ImagePipelineConfig getOkHttpImagePipelineConfig(Context context) {
    if (sOkHttpImagePipelineConfig == null) {
      OkHttpClient okHttpClient =
          new OkHttpClient.Builder().addNetworkInterceptor(new StethoInterceptor()).build();
      ImagePipelineConfig.Builder configBuilder =
          OkHttpImagePipelineConfigFactory.newBuilder(context, okHttpClient);
      configureCaches(configBuilder, context);
      configureLoggingListeners(configBuilder);
      sOkHttpImagePipelineConfig = configBuilder.build();
    }
    return sOkHttpImagePipelineConfig;
  }

  /** Configures disk and memory cache not to exceed common limits */
  private static void configureCaches(ImagePipelineConfig.Builder configBuilder, Context context) {
    final MemoryCacheParams bitmapCacheParams =
        new MemoryCacheParams(
            ConfigConstants.MAX_MEMORY_CACHE_SIZE, // Max total size of elements in the cache
            Integer.MAX_VALUE, // Max entries in the cache
            ConfigConstants.MAX_MEMORY_CACHE_SIZE, // Max total size of elements in eviction queue
            Integer.MAX_VALUE, // Max length of eviction queue
            Integer.MAX_VALUE, // Max cache entry size
            TimeUnit.MINUTES.toMillis(5)); // Interval for checking cache parameters
    configBuilder
        .setBitmapMemoryCacheParamsSupplier(
            new Supplier<MemoryCacheParams>() {
              public MemoryCacheParams get() {
                return bitmapCacheParams;
              }
            })
        .setMainDiskCacheConfig(
            DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryPath(context.getApplicationContext().getCacheDir())
                .setBaseDirectoryName(IMAGE_PIPELINE_CACHE_DIR)
                .setMaxCacheSize(ConfigConstants.MAX_DISK_CACHE_SIZE)
                .build());
  }

  private static void configureLoggingListeners(ImagePipelineConfig.Builder configBuilder) {
    Set<RequestListener> requestListeners = new HashSet<>();
    requestListeners.add(new RequestLoggingListener());
    configBuilder.setRequestListeners(requestListeners);
  }

  private static void configureOptions(ImagePipelineConfig.Builder configBuilder) {
    configBuilder.setDownsampleEnabled(true);
  }
}
