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
package at.illecker.storm.commons.tweet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.svm.featurevector.CombinedFeatureVectorGenerator;
import at.illecker.storm.commons.svm.featurevector.FeatureVectorGenerator;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.util.io.SerializationUtils;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public final class FeaturedTweet implements Serializable {
  private static final long serialVersionUID = -2662190284006213666L;
  private final Tweet m_tweet;
  private final List<String> m_tokens;
  private final List<String> m_preprocessedTokens;
  private final List<TaggedWord> m_preprocessedTaggedTokens;
  private final List<TaggedToken> m_arkTaggedTokens;
  private final List<TaggedWord> m_gateTaggedTokens;
  private final Map<Integer, Double> m_arkFeatureVector; // dense vector
  private final Map<Integer, Double> m_gateFeatureVector; // dense vector

  private FeaturedTweet(Tweet tweet, List<String> tokens,
      List<String> preprocessedTokens,
      List<TaggedWord> preprocessedTaggedTokens,
      List<TaggedToken> arkTaggedWords, List<TaggedWord> gateTaggedWords,
      Map<Integer, Double> arkFeatureVector,
      Map<Integer, Double> gateFeatureVector) {
    m_tweet = tweet;
    m_tokens = tokens;
    m_preprocessedTokens = preprocessedTokens;
    m_preprocessedTaggedTokens = preprocessedTaggedTokens;
    m_arkTaggedTokens = arkTaggedWords;
    m_gateTaggedTokens = gateTaggedWords;
    m_arkFeatureVector = arkFeatureVector;
    m_gateFeatureVector = gateFeatureVector;
  }

  public Long getId() {
    return m_tweet.getId();
  }

  public String getText() {
    return m_tweet.getText();
  }

  public Double getScore() {
    return m_tweet.getScore();
  }

  public List<String> getTokens() {
    return m_tokens;
  }

  public List<String> getPreprocessedTokens() {
    return m_preprocessedTokens;
  }

  public List<TaggedWord> getPreprocessedTaggedTokens() {
    return m_preprocessedTaggedTokens;
  }

  public List<TaggedToken> getArkTaggedTokens() {
    return m_arkTaggedTokens;
  }

  public List<TaggedWord> getGateTaggedTokens() {
    return m_gateTaggedTokens;
  }

  public Map<Integer, Double> getArkFeatureVector() {
    return m_arkFeatureVector;
  }

  public Map<Integer, Double> getGateFeatureVector() {
    return m_gateFeatureVector;
  }

  @Override
  public boolean equals(Object obj) {
    return m_tweet.equals(obj);
  }

  @Override
  public String toString() {
    return m_tweet.toString();
  }

  public static FeaturedTweet createFromTaggedWords(Long id, String text,
      Double score, List<String> tokens,
      List<TaggedWord> preprocessedTaggedTokens, List<TaggedWord> taggedWords,
      Map<Integer, Double> featureVector) {
    return new FeaturedTweet(new Tweet(id, text, score), tokens, null,
        preprocessedTaggedTokens, null, taggedWords, null, featureVector);
  }

  public static FeaturedTweet createFromTaggedWords(Tweet tweet,
      List<String> tokens, List<TaggedWord> preprocessedTaggedTokens,
      List<TaggedWord> taggedWords, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(tweet, tokens, null, preprocessedTaggedTokens,
        null, taggedWords, null, featureVector);
  }

  public static FeaturedTweet createFromTaggedTokens(Long id, String text,
      Double score, List<String> tokens, List<String> preprocessedTokens,
      List<TaggedToken> taggedTokens, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(new Tweet(id, text, score), tokens,
        preprocessedTokens, null, taggedTokens, null, featureVector, null);
  }

  public static FeaturedTweet createFromTaggedTokens(Tweet tweet,
      List<String> tokens, List<String> preprocessedTokens,
      List<TaggedToken> taggedTokens, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(tweet, tokens, preprocessedTokens, null,
        taggedTokens, null, featureVector, null);
  }

  public static FeaturedTweet create(Tweet tweet, List<String> tokens,
      List<String> preprocessedTokens,
      List<TaggedWord> preprocessedTaggedTokens,
      List<TaggedToken> taggedTokens, List<TaggedWord> taggedWords,
      Map<Integer, Double> arkFeatureVector,
      Map<Integer, Double> gateFeatureVector) {
    return new FeaturedTweet(tweet, tokens, preprocessedTokens,
        preprocessedTaggedTokens, taggedTokens, taggedWords, arkFeatureVector,
        gateFeatureVector);
  }

  public static final List<List<TaggedWord>> getTaggedWordsFromTweets(
      List<FeaturedTweet> featuredTweets) {
    List<List<TaggedWord>> taggedTweets = new ArrayList<List<TaggedWord>>();
    for (FeaturedTweet tweet : featuredTweets) {
      taggedTweets.add(tweet.getGateTaggedTokens());
    }
    return taggedTweets;
  }

  public static final List<List<TaggedToken>> getTaggedTokensFromTweets(
      List<FeaturedTweet> featuredTweets) {
    List<List<TaggedToken>> taggedTweets = new ArrayList<List<TaggedToken>>();
    for (FeaturedTweet tweet : featuredTweets) {
      taggedTweets.add(tweet.getArkTaggedTokens());
    }
    return taggedTweets;
  }

  public static final void serializeTrainTweets(Dataset dataset,
      boolean includeDevTweets) {

    final Preprocessor preprocessor = Preprocessor.getInstance();
    final ArkPOSTagger arkPOSTagger = ArkPOSTagger.getInstance();
    final GatePOSTagger gatePOSTagger = GatePOSTagger.getInstance();

    List<Tweet> tweets = dataset.getTrainTweets(includeDevTweets);

    // Tokenize
    List<List<String>> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    // Preprocess only
    List<List<String>> preprocessedTweets = preprocessor
        .preprocessTweets(tokenizedTweets);

    // Preprocess and tag
    List<List<TaggedWord>> preprocessedTaggedTweets = preprocessor
        .preprocessAndTagTweets(tokenizedTweets);

    // Ark POS Tagging
    List<List<TaggedToken>> arkTaggedTweets = arkPOSTagger
        .tagTweets(preprocessedTweets);

    // Gate POS Tagging
    List<List<TaggedWord>> gateTaggedTweets = gatePOSTagger
        .tagTweets(preprocessedTaggedTweets);

    // Ark Feature Vector Generator
    TweetTfIdf arkTweetTfIdf = TweetTfIdf.createFromTaggedTokens(
        arkTaggedTweets, TfType.RAW, TfIdfNormalization.COS, true);
    FeatureVectorGenerator arkFvg = new CombinedFeatureVectorGenerator(false,
        true, arkTweetTfIdf);

    // Gate Feature Vector Generator
    TweetTfIdf gateTweetTfIdf = TweetTfIdf.createFromTaggedWords(
        gateTaggedTweets, TfType.RAW, TfIdfNormalization.COS, true);
    FeatureVectorGenerator gateFvg = new CombinedFeatureVectorGenerator(true,
        true, gateTweetTfIdf);

    // Feature Vector Generation
    List<FeaturedTweet> featuredTrainTweets = new ArrayList<FeaturedTweet>();
    for (int i = 0; i < tweets.size(); i++) {
      // Ark Feature Vector Generation
      List<TaggedToken> arkTaggedTweet = arkTaggedTweets.get(i);
      Map<Integer, Double> arkFeatureVector = arkFvg
          .generateFeatureVectorFromTaggedTokens(arkTaggedTweet);

      // Gate Feature Vector Generation
      List<TaggedWord> gateTaggedTweet = gateTaggedTweets.get(i);
      Map<Integer, Double> gateFeatureVector = gateFvg
          .generateFeatureVectorFromTaggedWords(gateTaggedTweet);

      featuredTrainTweets.add(FeaturedTweet.create(tweets.get(i),
          tokenizedTweets.get(i), preprocessedTweets.get(i),
          preprocessedTaggedTweets.get(i), arkTaggedTweet, gateTaggedTweet,
          arkFeatureVector, gateFeatureVector));
    }

    SerializationUtils.serializeList(featuredTrainTweets,
        dataset.getTrainDataSerializationFile());
  }

  public static void main(String[] args) {
    serializeTrainTweets(Configuration.getDataSetSemEval2013(), true);
  }

}
