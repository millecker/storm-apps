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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

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
  public Map<Integer, Double> generateFeatureVector(List<TaggedWord> tweet) {

    Map<Integer, Double> featureVector = m_sentimentFeatureVectorGenerator
        .generateFeatureVector(tweet);

    featureVector.putAll(m_POSFeatureVectorGenerator
        .generateFeatureVector(tweet));

    featureVector.putAll(m_tfidfFeatureVectorGenerator
        .generateFeatureVector(tweet));

    return featureVector;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    List<Tweet> tweets = null;
    boolean extendedTest = false;

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
    // Tokenize
    List<List<String>> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    // Preprocess
    List<List<TaggedWord>> preprocessedTweets = preprocessor
        .preprocessTweets(tokenizedTweets);

    // POS Tagging
    List<List<TaggedWord>> taggedTweets = posTagger
        .tagTweets(preprocessedTweets);
    LOG.info("Preparation of Tweets finished after "
        + (System.currentTimeMillis() - startTime) + " ms");

    // Generate CombinedFeatureVectorGenerator
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
      TweetTfIdf.print(
          "Tf-Idf",
          TweetTfIdf.tfIdf(tweetTfIdf.getTermFreqs(),
              tweetTfIdf.getInverseDocFreq(),
              tweetTfIdf.getTfIdfNormalization()),
          tweetTfIdf.getInverseDocFreq());
    }

    // Combined Feature Vector Generation
    for (List<TaggedWord> taggedTweet : taggedTweets) {
      Map<Integer, Double> combinedFeatureVector = cfvg
          .generateFeatureVector(taggedTweet);

      // Build feature vector string
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : combinedFeatureVector
          .entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }

      LOG.info("Tweet: '" + taggedTweet + "'");
      LOG.info("CombinedFeatureVector: " + featureVectorStr);
    }
  }

}
