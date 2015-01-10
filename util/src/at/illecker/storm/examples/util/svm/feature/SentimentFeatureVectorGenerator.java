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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.dictionaries.SentimentResult;
import at.illecker.storm.examples.util.dictionaries.SentimentWordLists;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;

  private SentimentWordLists m_sentimentWordLists;
  private int m_vectorStartId = 1;

  public SentimentFeatureVectorGenerator() {
    this.m_sentimentWordLists = SentimentWordLists.getInstance();
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
    // Nouns/wc, Verbs/wc, Adjectives/wc, Adverbs/wc, Interjections/wc,
    // Punctuations/wc, Hashtags/wc
    return 14;
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(Tweet tweet) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    SentimentResult tweetSentiment = m_sentimentWordLists
        .getTweetSentiment(tweet);
    if (tweetSentiment != null) {
      // LOG.info("tweetSentiment: " + tweetSentiment);
      if (tweetSentiment.getPosCount() != 0)
        resultFeatureVector.put(m_vectorStartId,
            (double) tweetSentiment.getPosCount());
      if (tweetSentiment.getNeutralCount() != 0)
        resultFeatureVector.put(m_vectorStartId + 1,
            (double) tweetSentiment.getNeutralCount());
      if (tweetSentiment.getNegCount() != 0)
        resultFeatureVector.put(m_vectorStartId + 2,
            (double) tweetSentiment.getNegCount());
      if (tweetSentiment.getSum() != 0)
        resultFeatureVector.put(m_vectorStartId + 3, tweetSentiment.getSum());
      if (tweetSentiment.getCount() != 0)
        resultFeatureVector.put(m_vectorStartId + 4,
            (double) tweetSentiment.getCount());
      if (tweetSentiment.getMaxPos() != 0)
        resultFeatureVector
            .put(m_vectorStartId + 5, tweetSentiment.getMaxPos());
      if (tweetSentiment.getMaxNeg() != 0)
        resultFeatureVector
            .put(m_vectorStartId + 6, tweetSentiment.getMaxNeg());
    }

    double[] posTags = countPOSTags(tweet);
    if (posTags != null) {
      if (posTags[0] != 0) // nouns / wordCount
        resultFeatureVector.put(m_vectorStartId + 7, posTags[0]);
      if (posTags[1] != 0) // verbs / wordCount
        resultFeatureVector.put(m_vectorStartId + 8, posTags[1]);
      if (posTags[2] != 0) // adjectives / wordCount
        resultFeatureVector.put(m_vectorStartId + 9, posTags[2]);
      if (posTags[3] != 0) // adverbs / wordCount
        resultFeatureVector.put(m_vectorStartId + 10, posTags[3]);
      if (posTags[4] != 0) // interjections / wordCount
        resultFeatureVector.put(m_vectorStartId + 11, posTags[4]);
      if (posTags[5] != 0) // punctuations / wordCount
        resultFeatureVector.put(m_vectorStartId + 12, posTags[5]);
      if (posTags[6] != 0) // hashtags / wordCount
        resultFeatureVector.put(m_vectorStartId + 13, posTags[6]);
    }

    if (LOGGING) {
      LOG.info("TweetSentiment: " + tweetSentiment);
      LOG.info("POStags: " + Arrays.toString(posTags));
    }

    return resultFeatureVector;
  }

  private double[] countPOSTags(Tweet tweet) {
    // [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAG]
    double[] posTags = new double[] { 0d, 0d, 0d, 0d, 0d, 0d, 0d };
    int wordCount = 0;
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      for (TaggedWord word : sentence) {
        wordCount++;
        String pennTag = word.tag();
        if (pennTag.startsWith("NN")) {
          posTags[0]++;
        } else if (pennTag.startsWith("VB")) {
          posTags[1]++;
        } else if (pennTag.startsWith("JJ")) {
          posTags[2]++;
        } else if (pennTag.startsWith("RB")) {
          posTags[3]++;
        } else if (pennTag.startsWith("UH")) {
          posTags[4]++;
        } else if ((pennTag.equals(".")) || (pennTag.equals(":"))) {
          posTags[5]++;
        } else if (pennTag.startsWith("HT")) {
          posTags[6]++;
        }
      }
    }
    // normalize
    for (int i = 0; i < posTags.length; i++) {
      posTags[i] /= wordCount;
    }
    return posTags;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    SentimentFeatureVectorGenerator sfvg = new SentimentFeatureVectorGenerator();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);
      tweet.addPreprocessedSentence(preprocessedTokens);

      // POS Tagging
      List<TaggedWord> taggedSentence = posTagger
          .tagSentence(preprocessedTokens);
      tweet.addTaggedSentence(taggedSentence);

      // Feature Vector Generation
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : sfvg.calculateFeatureVector(
          tweet).entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedSentence: " + taggedSentence);
      LOG.info("FeatureVector: " + featureVectorStr);
    }

    sfvg.getSentimentWordLists().close();
  }
}
