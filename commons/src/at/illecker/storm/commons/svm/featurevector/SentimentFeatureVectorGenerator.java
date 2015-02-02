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
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.dict.SentimentDictionary;
import at.illecker.storm.commons.dict.SentimentResult;
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentFeatureVectorGenerator.class);
  private static final boolean LOGGING = Configuration.get(
      "commons.featurevectorgenerator.sentiment.logging", false);
  private static final int VECTOR_SIZE = 7;
  private SentimentDictionary m_sentimentDict;
  private int m_vectorStartId = 1;

  public SentimentFeatureVectorGenerator() {
    this.m_sentimentDict = SentimentDictionary.getInstance();
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public SentimentFeatureVectorGenerator(int vectorStartId) {
    this();
    this.m_vectorStartId = vectorStartId;
  }

  public SentimentDictionary getSentimentDictionary() {
    return m_sentimentDict;
  }

  @Override
  public int getFeatureVectorSize() {
    // VECTOR_SIZE = {PosCount, NeutralCount, NegCount, Sum, Count, MaxPos,
    // MaxNeg}
    return VECTOR_SIZE * m_sentimentDict.getSentimentWordListCount();
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedWords(
      List<TaggedWord> taggedWords) {

    Map<Integer, SentimentResult> tweetSentiments = m_sentimentDict
        .getSentenceSentimentFromTaggedWord(taggedWords);

    return generateFeatureVector(tweetSentiments);
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedTokens(
      List<TaggedToken> taggedTokens) {

    Map<Integer, SentimentResult> tweetSentiments = m_sentimentDict
        .getSentenceSentimentFromTaggedToken(taggedTokens);

    return generateFeatureVector(tweetSentiments);
  }

  private Map<Integer, Double> generateFeatureVector(
      Map<Integer, SentimentResult> tweetSentiments) {
    Map<Integer, Double> featureVector = new TreeMap<Integer, Double>();

    if (tweetSentiments != null) {
      for (Map.Entry<Integer, SentimentResult> tweetSentiment : tweetSentiments
          .entrySet()) {

        int key = tweetSentiment.getKey();
        SentimentResult sentimentResult = tweetSentiment.getValue();
        // LOG.info("TweetSentiment: " + sentimentResult);

        if (sentimentResult.getPosCount() != 0) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE),
              (double) sentimentResult.getPosCount());
        }
        if (sentimentResult.getNeutralCount() != 0) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 1,
              (double) sentimentResult.getNeutralCount());
        }
        if (sentimentResult.getNegCount() != 0) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 2,
              (double) sentimentResult.getNegCount());
        }
        if (sentimentResult.getSum() != 0) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 3,
              sentimentResult.getSum());
        }
        if (sentimentResult.getCount() != 0) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 4,
              (double) sentimentResult.getCount());
        }
        if (sentimentResult.getMaxPos() != null) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 5,
              sentimentResult.getMaxPos());
        }
        if (sentimentResult.getMaxNeg() != null) {
          featureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 6,
              sentimentResult.getMaxNeg());
        }

        if (LOGGING) {
          LOG.info("TweetSentiment: " + sentimentResult);
        }
      }
    }

    return featureVector;
  }

  public static void main(String[] args) {
    boolean useArkPOSTagger = true;
    Preprocessor preprocessor = Preprocessor.getInstance();
    SentimentFeatureVectorGenerator sfvg = new SentimentFeatureVectorGenerator();

    List<Tweet> tweets = Tweet.getTestTweets();

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

      // Sentiment Feature Vector Generation
      for (List<TaggedToken> taggedTokens : taggedTweets) {
        Map<Integer, Double> sentimentFeatureVector = sfvg
            .generateFeatureVectorFromTaggedTokens(taggedTokens);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : sentimentFeatureVector
            .entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }

        LOG.info("TaggedSentence: " + taggedTokens);
        LOG.info("SentimentFeatureVector: " + featureVectorStr);
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

      // Sentiment Feature Vector Generation
      for (List<TaggedWord> taggedWords : taggedTweets) {
        Map<Integer, Double> sentimentFeatureVector = sfvg
            .generateFeatureVectorFromTaggedWords(taggedWords);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : sentimentFeatureVector
            .entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }

        LOG.info("TaggedSentence: " + taggedWords);
        LOG.info("SentimentFeatureVector: " + featureVectorStr);
      }
    }

    sfvg.getSentimentDictionary().close();
  }
}
