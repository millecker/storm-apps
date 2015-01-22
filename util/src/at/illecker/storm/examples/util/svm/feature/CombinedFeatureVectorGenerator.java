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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TfIdfNormalization;
import at.illecker.storm.examples.util.tfidf.TfType;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.PreprocessedTweet;
import at.illecker.storm.examples.util.tweet.TaggedTweet;
import at.illecker.storm.examples.util.tweet.TokenizedTweet;
import at.illecker.storm.examples.util.tweet.Tweet;

public class CombinedFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(CombinedFeatureVectorGenerator.class);

  private SentimentFeatureVectorGenerator m_sentimentFeatureVectorGenerator = null;
  private TfIdfFeatureVectorGenerator m_tfidfFeatureVectorGenerator = null;
  private POSFeatureVectorGenerator m_POSFeatureVectorGenerator = null;

  public CombinedFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_sentimentFeatureVectorGenerator = new SentimentFeatureVectorGenerator(
        1);

    this.m_POSFeatureVectorGenerator = new POSFeatureVectorGenerator(
        m_sentimentFeatureVectorGenerator.getFeatureVectorSize() + 1);

    this.m_tfidfFeatureVectorGenerator = new TfIdfFeatureVectorGenerator(
        tweetTfIdf, m_sentimentFeatureVectorGenerator.getFeatureVectorSize()
            + m_POSFeatureVectorGenerator.getFeatureVectorSize() + 1);

    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  @Override
  public int getFeatureVectorSize() {
    return m_sentimentFeatureVectorGenerator.getFeatureVectorSize()
        + m_POSFeatureVectorGenerator.getFeatureVectorSize()
        + m_tfidfFeatureVectorGenerator.getFeatureVectorSize();
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(TaggedTweet tweet) {

    Map<Integer, Double> featureVector = m_sentimentFeatureVectorGenerator
        .calculateFeatureVector(tweet);

    featureVector.putAll(m_POSFeatureVectorGenerator
        .calculateFeatureVector(tweet));

    featureVector.putAll(m_tfidfFeatureVectorGenerator
        .calculateFeatureVector(tweet));

    return featureVector;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    List<Tweet> tweets = null;
    boolean extendedTest = true;

    // load tweets
    if (extendedTest) {
      // SemEval2013
      Dataset dataset = Configuration.getDataSetSemEval2013();
      tweets = dataset.getTrainTweets(true);
    } else { // test tweets
      tweets = Tweet.getTestTweets();
    }

    // prepare Tweets
    long startTime = System.currentTimeMillis();
    // Tokenize tweets
    List<TokenizedTweet> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    // Preprocess
    List<PreprocessedTweet> preprocessedTweets = preprocessor
        .preprocessTweets(tokenizedTweets);

    // POS Tagging
    List<TaggedTweet> taggedTweets = posTagger.tagTweets(preprocessedTweets);
    LOG.info("Preparation of Tweets finished after "
        + (System.currentTimeMillis() - startTime) + " ms");

    // Calculate Tf-Idf
    boolean usePOSTags = true; // use POS tags in terms
    TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTweets, TfType.RAW,
        TfIdfNormalization.COS, usePOSTags);
    CombinedFeatureVectorGenerator cfvg = new CombinedFeatureVectorGenerator(
        tweetTfIdf);

    // Debug
    if (!extendedTest) {
      TweetTfIdf.print("Term Frequency", tweetTfIdf.getTermFreqs(),
          tweetTfIdf.getInverseDocFreq());
      TweetTfIdf.print("Inverse Document Frequency",
          tweetTfIdf.getInverseDocFreq());
    }

    // Feature Vector Generation
    for (TaggedTweet tweet : taggedTweets) {
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : cfvg.calculateFeatureVector(
          tweet).entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }
      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("FeatureVector: " + featureVectorStr);
    }
  }
}
