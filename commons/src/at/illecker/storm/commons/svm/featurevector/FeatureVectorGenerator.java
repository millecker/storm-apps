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
package at.illecker.storm.commons.svm.featurevector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public abstract class FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(FeatureVectorGenerator.class);

  public abstract int getFeatureVectorSize();

  public abstract Map<Integer, Double> generateFeatureVectorFromTaggedWords(
      List<TaggedWord> tweet);

  public abstract Map<Integer, Double> generateFeatureVectorFromTaggedTokens(
      List<TaggedToken> tweet);

  public List<Map<Integer, Double>> generateFeatureVectorsFromTaggedWords(
      List<List<TaggedWord>> tweets) {
    return generateFeatureVectorsFromTaggedWords(tweets, false);
  }

  public List<Map<Integer, Double>> generateFeatureVectorsFromTaggedWords(
      List<List<TaggedWord>> taggedTweets, boolean logging) {
    List<Map<Integer, Double>> featuredVectors = new ArrayList<Map<Integer, Double>>();
    for (List<TaggedWord> tweet : taggedTweets) {
      Map<Integer, Double> featureVector = generateFeatureVectorFromTaggedWords(tweet);
      if (logging) {
        LOG.info("Tweet: " + tweet);
        LOG.info("FeatureVector: " + featureVector);
      }
      featuredVectors.add(featureVector);
    }
    return featuredVectors;
  }

  public List<Map<Integer, Double>> generateFeatureVectorsFromTaggedTokens(
      List<List<TaggedToken>> tweets) {
    return generateFeatureVectorsFromTaggedTokens(tweets, false);
  }

  public List<Map<Integer, Double>> generateFeatureVectorsFromTaggedTokens(
      List<List<TaggedToken>> taggedTweets, boolean logging) {
    List<Map<Integer, Double>> featuredVectors = new ArrayList<Map<Integer, Double>>();
    for (List<TaggedToken> tweet : taggedTweets) {
      Map<Integer, Double> featureVector = generateFeatureVectorFromTaggedTokens(tweet);
      if (logging) {
        LOG.info("Tweet: " + tweet);
        LOG.info("FeatureVector: " + featureVector);
      }
      featuredVectors.add(featureVector);
    }
    return featuredVectors;
  }

}
