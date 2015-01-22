/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.illecker.storm.examples.util.svm.feature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tweet.FeaturedTweet;
import at.illecker.storm.examples.util.tweet.TaggedTweet;

public abstract class FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(FeatureVectorGenerator.class);

  public abstract int getFeatureVectorSize();

  public abstract Map<Integer, Double> calculateFeatureVector(TaggedTweet tweet);

  public List<FeaturedTweet> generateFeatureVectors(List<TaggedTweet> tweets,
      boolean logging) {
    List<FeaturedTweet> featuredTweets = new ArrayList<FeaturedTweet>();
    for (TaggedTweet tweet : tweets) {
      Map<Integer, Double> featureVector = this.calculateFeatureVector(tweet);
      if (logging) {
        LOG.info("Tweet: " + tweet);
        LOG.info("FeatureVector: " + featureVector);
      }
      featuredTweets.add(new FeaturedTweet(tweet.getId(), tweet.getText(),
          tweet.getScore(), featureVector));
    }
    return featuredTweets;
  }

  public List<FeaturedTweet> generateFeatureVectors(List<TaggedTweet> tweets) {
    return generateFeatureVectors(tweets, false);
  }
}
