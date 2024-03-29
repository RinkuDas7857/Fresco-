/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;
import android.util.Base64;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Producer for data URIs.
 *
 * <p>Data URIs embed the data in the URI itself. They don't point to a file location; the URI is
 * the data. Data can be encoded in either base-64 or escaped ASCII. See the <a
 * href="http://tools.ietf.org/html/rfc2397">spec</a> for full details.
 *
 * <p>Data URIs are intended for small pieces of data only, since the URI lives on the Java heap.
 * For large data, use a another URI type.
 *
 * <p>Charsets specified in the URI are ignored. Only UTF-8 encoding is currently supported.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class DataFetchProducer extends LocalFetchProducer {

  public static final String PRODUCER_NAME = "DataFetchProducer";

  public DataFetchProducer(PooledByteBufferFactory pooledByteBufferFactory) {
    super(CallerThreadExecutor.getInstance(), pooledByteBufferFactory);
  }

  @Override
  protected EncodedImage getEncodedImage(ImageRequest imageRequest) throws IOException {
    byte[] data = getData(imageRequest.getSourceUri().toString());
    return getByteBufferBackedEncodedImage(new ByteArrayInputStream(data), data.length);
  }

  @Override
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  @VisibleForTesting
  static byte[] getData(String uri) {
    /*
     * Format of a data URL:
     * data:mime/type;param=value;param=value;base64,actual_data
     * everything is optional except the actual data, which is either
     * base-64 or escaped ASCII encoded.
     */
    Preconditions.checkArgument(uri.substring(0, 5).equals("data:"));
    int commaPos = uri.indexOf(',');

    String dataStr = uri.substring(commaPos + 1, uri.length());
    if (isBase64(uri.substring(0, commaPos))) {
      return Base64.decode(dataStr, Base64.DEFAULT);
    } else {
      String str = Uri.decode(dataStr);
      Preconditions.checkNotNull(str);
      byte[] b = str.getBytes();
      return b;
    }
  }

  @VisibleForTesting
  static boolean isBase64(String prefix) {
    if (!prefix.contains(";")) {
      return false;
    }
    String[] parameters = prefix.split(";");
    return parameters[parameters.length - 1].equals("base64");
  }
}
