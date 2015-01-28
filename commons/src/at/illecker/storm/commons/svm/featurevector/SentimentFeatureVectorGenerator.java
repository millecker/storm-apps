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
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.dict.SentimentResult;
import at.illecker.storm.commons.dict.SentimentWordLists;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.TaggedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;
  private static final int VECTOR_SIZE = 7;
  private SentimentWordLists m_sentimentWordLists;
  private int m_vectorStartId = 1;

  public SentimentFeatureVectorGenerator() {
    this.m_sentimentWordLists = SentimentWordLists.getInstance();
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public SentimentFeatureVectorGenerator(int vectorStartId) {
    this();
    this.m_vectorStartId = vectorStartId;
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  @Override
  public int getFeatureVectorSize() {
    // PosCount, NeutralCount, NegCount, Sum, Count, MaxPos, MaxNeg
    return VECTOR_SIZE * m_sentimentWordLists.getSentimentWordListCount();
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(TaggedTweet tweet) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    Map<Integer, SentimentResult> tweetSentiments = m_sentimentWordLists
        .getTweetSentiment(tweet);

    if (tweetSentiments != null) {
      for (Map.Entry<Integer, SentimentResult> tweetSentiment : tweetSentiments
          .entrySet()) {

        int key = tweetSentiment.getKey();
        SentimentResult sentimentResult = tweetSentiment.getValue();
        // LOG.info("TweetSentiment: " + sentimentResult);

        if (sentimentResult.getPosCount() != 0) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE),
              (double) sentimentResult.getPosCount());
        }
        if (sentimentResult.getNeutralCount() != 0) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 1,
              (double) sentimentResult.getNeutralCount());
        }
        if (sentimentResult.getNegCount() != 0) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 2,
              (double) sentimentResult.getNegCount());
        }
        if (sentimentResult.getSum() != 0) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 3,
              sentimentResult.getSum());
        }
        if (sentimentResult.getCount() != 0) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 4,
              (double) sentimentResult.getCount());
        }
        if (sentimentResult.getMaxPos() != null) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 5,
              sentimentResult.getMaxPos());
        }
        if (sentimentResult.getMaxNeg() != null) {
          resultFeatureVector.put(m_vectorStartId + (key * VECTOR_SIZE) + 6,
              sentimentResult.getMaxNeg());
        }

        if (LOGGING) {
          LOG.info("TweetSentiment: " + sentimentResult);
        }
      }
    }

    return resultFeatureVector;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    SentimentFeatureVectorGenerator sfvg = new SentimentFeatureVectorGenerator();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      List<TaggedWord> preprocessedTokens = preprocessor.preprocess(tokens);

      // POS Tagging
      List<List<TaggedWord>> taggedSentences = new ArrayList<List<TaggedWord>>();
      taggedSentences.add(posTagger.tagSentence(preprocessedTokens));

      // Feature Vector Generation
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : sfvg.calculateFeatureVector(
          new TaggedTweet(tweet.getId(), tweet.getText(), tweet.getScore(),
              taggedSentences)).entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedSentence: " + taggedSentences);
      LOG.info("FeatureVector: " + featureVectorStr);
    }

    sfvg.getSentimentWordLists().close();
  }
}