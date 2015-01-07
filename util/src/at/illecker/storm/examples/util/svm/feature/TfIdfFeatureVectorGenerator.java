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
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
import edu.stanford.nlp.ling.TaggedWord;

public class TfIdfFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(TfIdfFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;

  private TweetTfIdf m_tweetTfIdf = null;
  private SentimentWordLists m_sentimentWordLists;
  private int m_vectorStartId = 1;

  public TfIdfFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_tweetTfIdf = tweetTfIdf;
    this.m_sentimentWordLists = SentimentWordLists.getInstance();
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public TfIdfFeatureVectorGenerator(TweetTfIdf tweetTfIdf, int vectorStartId) {
    this(tweetTfIdf);
    this.m_vectorStartId = vectorStartId;
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  @Override
  public int getFeatureVectorSize() {
    return m_tweetTfIdf.getInverseDocFreq().size();
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(Tweet tweet) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    if (m_tweetTfIdf != null) {
      Map<String, Double> idf = m_tweetTfIdf.getInverseDocFreq();
      Map<String, Integer> termIds = m_tweetTfIdf.getTermIds();
      Map<String, Double> tfIdf = m_tweetTfIdf.tfIdf(tweet);

      for (Map.Entry<String, Double> element : tfIdf.entrySet()) {
        String key = element.getKey();
        if (idf.containsKey(key)) {
          int vectorId = m_vectorStartId + termIds.get(key);
          resultFeatureVector.put(vectorId, element.getValue());
        }
      }

    }
    // LOG.info("TfIdsVector: " + resultFeatureVector);

    return resultFeatureVector;
  }

  public static void main(String[] args) {
    List<Tweet> tweets = Tweet.getTestTweets();
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();

    // prepare Tweets
    for (Tweet tweet : tweets) {
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
    }

    boolean usePOSTags = true; // use POS tags in terms
    // calculate Tf-Idf
    TweetTfIdf tweetTfIdf = new TweetTfIdf(tweets, usePOSTags);
    TfIdfFeatureVectorGenerator efvg = new TfIdfFeatureVectorGenerator(
        tweetTfIdf);

    // debug
    TweetTfIdf.print("Term Frequency", tweetTfIdf.getTermFreqs(),
        tweetTfIdf.getInverseDocFreq());
    TweetTfIdf.print("Inverse Document Frequency",
        tweetTfIdf.getInverseDocFreq());

    // Feature Vector Generation
    for (Tweet tweet : tweets) {
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : efvg.calculateFeatureVector(
          tweet).entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("FeatureVector: " + featureVectorStr);
    }

    efvg.getSentimentWordLists().close();
  }
}
