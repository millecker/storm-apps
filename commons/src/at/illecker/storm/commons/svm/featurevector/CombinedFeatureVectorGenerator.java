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
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public class CombinedFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(CombinedFeatureVectorGenerator.class);

  private final boolean m_useTaggedWords;
  private SentimentFeatureVectorGenerator m_sentimentFeatureVectorGenerator = null;
  private TfIdfFeatureVectorGenerator m_tfidfFeatureVectorGenerator = null;
  private POSFeatureVectorGenerator m_POSFeatureVectorGenerator = null;

  public CombinedFeatureVectorGenerator(boolean useTaggedWords,
      boolean normalizePOSCounts, TweetTfIdf tweetTfIdf) {
    m_useTaggedWords = useTaggedWords;

    m_sentimentFeatureVectorGenerator = new SentimentFeatureVectorGenerator(1);

    m_POSFeatureVectorGenerator = new POSFeatureVectorGenerator(useTaggedWords,
        normalizePOSCounts,
        m_sentimentFeatureVectorGenerator.getFeatureVectorSize() + 1);

    m_tfidfFeatureVectorGenerator = new TfIdfFeatureVectorGenerator(tweetTfIdf,
        m_sentimentFeatureVectorGenerator.getFeatureVectorSize()
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
  public Map<Integer, Double> generateFeatureVectorFromTaggedWords(
      List<TaggedWord> tweet) {
    if (!m_useTaggedWords) {
      throw new RuntimeException(
          "Use TaggedWords was set to false! generateFeatureVectorFromTaggedWords is not applicable!");
    }

    Map<Integer, Double> featureVector = m_sentimentFeatureVectorGenerator
        .generateFeatureVectorFromTaggedWords(tweet);

    featureVector.putAll(m_POSFeatureVectorGenerator
        .generateFeatureVectorFromTaggedWords(tweet));

    featureVector.putAll(m_tfidfFeatureVectorGenerator
        .generateFeatureVectorFromTaggedWords(tweet));

    return featureVector;
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedTokens(
      List<TaggedToken> tweet) {
    if (m_useTaggedWords) {
      throw new RuntimeException(
          "Use TaggedWords was set to true! generateFeatureVectorFromTaggedTokens is not applicable!");
    }

    Map<Integer, Double> featureVector = m_sentimentFeatureVectorGenerator
        .generateFeatureVectorFromTaggedTokens(tweet);

    featureVector.putAll(m_POSFeatureVectorGenerator
        .generateFeatureVectorFromTaggedTokens(tweet));

    featureVector.putAll(m_tfidfFeatureVectorGenerator
        .generateFeatureVectorFromTaggedTokens(tweet));

    return featureVector;
  }

  public static void main(String[] args) {
    boolean extendedTest = false;
    boolean useArkPOSTagger = true;
    boolean usePOSTags = true; // use POS tags in terms
    Preprocessor preprocessor = Preprocessor.getInstance();

    // load tweets
    List<Tweet> tweets = null;
    if (extendedTest) {
      // SemEval2013
      Dataset dataset = Configuration.getDataSetSemEval2013();
      tweets = dataset.getTrainTweets(true);
    } else { // test tweets
      tweets = Tweet.getTestTweets();
    }

    // Tokenize
    List<List<String>> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    if (useArkPOSTagger) {
      ArkPOSTagger arkPOSTagger = ArkPOSTagger.getInstance();

      // Preprocess only
      long startTime = System.currentTimeMillis();
      List<List<String>> preprocessedTweets = preprocessor
          .preprocessTweets(tokenizedTweets);
      LOG.info("Preprocess finished after "
          + (System.currentTimeMillis() - startTime) + " ms");

      // Ark POS Tagging
      startTime = System.currentTimeMillis();
      List<List<TaggedToken>> taggedTweets = arkPOSTagger
          .tagTweets(preprocessedTweets);
      LOG.info("Ark POS Tagger finished after "
          + (System.currentTimeMillis() - startTime) + " ms");

      // Generate CombinedFeatureVectorGenerator
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedTokens(taggedTweets,
          TfType.RAW, TfIdfNormalization.COS, usePOSTags);
      CombinedFeatureVectorGenerator cfvg = new CombinedFeatureVectorGenerator(
          false, true, tweetTfIdf);

      // Combined Feature Vector Generation
      for (List<TaggedToken> taggedTokens : taggedTweets) {
        Map<Integer, Double> combinedFeatureVector = cfvg
            .generateFeatureVectorFromTaggedTokens(taggedTokens);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : combinedFeatureVector
            .entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }
        LOG.info("Tweet: '" + taggedTokens + "'");
        LOG.info("CombinedFeatureVector: " + featureVectorStr);
      }

    } else {
      GatePOSTagger gatePOSTagger = GatePOSTagger.getInstance();

      // Preprocess and tag
      long startTime = System.currentTimeMillis();
      List<List<TaggedWord>> preprocessedTweets = preprocessor
          .preprocessAndTagTweets(tokenizedTweets);
      LOG.info("PreprocessAndTag finished after "
          + (System.currentTimeMillis() - startTime) + " ms");

      // Gate POS Tagging
      startTime = System.currentTimeMillis();
      List<List<TaggedWord>> taggedTweets = gatePOSTagger
          .tagTweets(preprocessedTweets);
      LOG.info("Gate POS Tagger finished after "
          + (System.currentTimeMillis() - startTime) + " ms");

      // Generate CombinedFeatureVectorGenerator
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedWords(taggedTweets,
          TfType.RAW, TfIdfNormalization.COS, usePOSTags);
      CombinedFeatureVectorGenerator cfvg = new CombinedFeatureVectorGenerator(
          true, true, tweetTfIdf);

      // Combined Feature Vector Generation
      for (List<TaggedWord> taggedWords : taggedTweets) {
        Map<Integer, Double> combinedFeatureVector = cfvg
            .generateFeatureVectorFromTaggedWords(taggedWords);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : combinedFeatureVector
            .entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }
        LOG.info("Tweet: '" + taggedWords + "'");
        LOG.info("CombinedFeatureVector: " + featureVectorStr);
      }
    }
  }

}
