/*
 * Copyright 2020 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.spanner.publisher.it;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.ServiceOptions;
import com.google.cloud.pubsub.v1.SubscriptionAdminClient;
import com.google.cloud.pubsub.v1.SubscriptionAdminSettings;
import com.google.cloud.pubsub.v1.TopicAdminClient;
import com.google.cloud.pubsub.v1.TopicAdminSettings;
import com.google.cloud.spanner.watcher.it.SpannerTestHelper;
import com.google.cloud.spanner.watcher.it.SpannerTestHelper.ITSpannerEnv;
import com.google.pubsub.v1.PushConfig;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Random;
import java.util.logging.Logger;

/** Helper class for for change publisher integration tests. */
public final class PubsubTestHelper {
  public static class ITPubsubEnv extends ITSpannerEnv {
    public final String topicId =
        System.getProperty(
            "pubsub.topic", String.format("scw-test-topic-%08d", RND.nextInt(100000000)));
    public final String subscriptionId =
        System.getProperty(
            "pubsub.subscription",
            String.format("scw-test-subscription-%08d", RND.nextInt(100000000)));
    final TopicAdminClient topicAdminClient;
    final SubscriptionAdminClient subAdminClient;

    public ITPubsubEnv() {
      try {
        topicAdminClient =
            TopicAdminClient.create(
                TopicAdminSettings.newBuilder()
                    .setCredentialsProvider(
                        FixedCredentialsProvider.create(PubsubTestHelper.getPubSubCredentials()))
                    .build());
        subAdminClient =
            SubscriptionAdminClient.create(
                SubscriptionAdminSettings.newBuilder()
                    .setCredentialsProvider(
                        FixedCredentialsProvider.create(PubsubTestHelper.getPubSubCredentials()))
                    .build());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final Logger logger = Logger.getLogger(SpannerTestHelper.class.getName());
  private static final Random RND = new Random();
  public static final String PUBSUB_PROJECT_ID =
      System.getProperty("pubsub.project", ServiceOptions.getDefaultProjectId());
  private static final String PUBSUB_CREDENTIALS_FILE = System.getProperty("pubsub.credentials");

  public static void createTestTopic(ITPubsubEnv env) throws Exception {
    env.topicAdminClient.createTopic(
        String.format("projects/%s/topics/%s", PubsubTestHelper.PUBSUB_PROJECT_ID, env.topicId));
    logger.info(String.format("Created topic %s", env.topicId));
  }

  public static void createTestSubscription(ITPubsubEnv env) {
    env.subAdminClient.createSubscription(
        String.format(
            "projects/%s/subscriptions/%s", PubsubTestHelper.PUBSUB_PROJECT_ID, env.subscriptionId),
        String.format("projects/%s/topics/%s", PubsubTestHelper.PUBSUB_PROJECT_ID, env.topicId),
        PushConfig.getDefaultInstance(),
        10);
    logger.info(String.format("Created subscription %s", env.subscriptionId));
  }

  public static void deleteTestTopic(ITPubsubEnv env) {
    env.subAdminClient.deleteSubscription(
        String.format(
            "projects/%s/subscriptions/%s",
            PubsubTestHelper.PUBSUB_PROJECT_ID, env.subscriptionId));
    logger.info("Dropped test subscription");
  }

  public static void deleteTestSubscription(ITPubsubEnv env) {
    env.topicAdminClient.deleteTopic(
        String.format("projects/%s/topics/%s", PubsubTestHelper.PUBSUB_PROJECT_ID, env.topicId));
    logger.info("Dropped test topic");
  }

  public static Credentials getPubSubCredentials() throws IOException {
    if (PUBSUB_CREDENTIALS_FILE != null) {
      return GoogleCredentials.fromStream(new FileInputStream(PUBSUB_CREDENTIALS_FILE));
    }
    return GoogleCredentials.getApplicationDefault();
  }
}
