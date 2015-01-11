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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tweet.Tweet;

public class CombinedFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(CombinedFeatureVectorGenerator.class);

  private SentimentFeatureVectorGenerator m_sentimentFeatureVectorGenerator = null;
  private TfIdfFeatureVectorGenerator m_tfidfFeatureVectorGenerator = null;

  public CombinedFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_sentimentFeatureVectorGenerator = new SentimentFeatureVectorGenerator(
        1);
    this.m_tfidfFeatureVectorGenerator = new TfIdfFeatureVectorGenerator(
        tweetTfIdf,
        m_sentimentFeatureVectorGenerator.getFeatureVectorSize() + 1);
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  @Override
  public int getFeatureVectorSize() {
    return m_sentimentFeatureVectorGenerator.getFeatureVectorSize()
        + m_tfidfFeatureVectorGenerator.getFeatureVectorSize();
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(Tweet tweet) {

    Map<Integer, Double> featureVector = m_sentimentFeatureVectorGenerator
        .calculateFeatureVector(tweet);

    featureVector.putAll(m_tfidfFeatureVectorGenerator
        .calculateFeatureVector(tweet));

    return featureVector;
  }
}
