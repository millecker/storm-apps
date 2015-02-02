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
  private final Map<Integer, Double> m_featureVector; // dense vector

  private FeaturedTweet(Tweet tweet, List<String> tokens,
      List<String> preprocessedTokens,
      List<TaggedWord> preprocessedTaggedTokens,
      List<TaggedToken> arkTaggedWords, List<TaggedWord> gateTaggedWords,
      Map<Integer, Double> featureVector) {
    m_tweet = tweet;
    m_tokens = tokens;
    m_preprocessedTokens = preprocessedTokens;
    m_preprocessedTaggedTokens = preprocessedTaggedTokens;
    m_arkTaggedTokens = arkTaggedWords;
    m_gateTaggedTokens = gateTaggedWords;
    m_featureVector = featureVector;
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

  public Map<Integer, Double> getFeatureVector() {
    return m_featureVector;
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
        preprocessedTaggedTokens, null, taggedWords, featureVector);
  }

  public static FeaturedTweet createFromTaggedWords(Tweet tweet,
      List<String> tokens, List<TaggedWord> preprocessedTaggedTokens,
      List<TaggedWord> taggedWords, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(tweet, tokens, null, preprocessedTaggedTokens,
        null, taggedWords, featureVector);
  }

  public static FeaturedTweet createFromTaggedTokens(Long id, String text,
      Double score, List<String> tokens, List<String> preprocessedTokens,
      List<TaggedToken> taggedTokens, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(new Tweet(id, text, score), tokens,
        preprocessedTokens, null, taggedTokens, null, featureVector);
  }

  public static FeaturedTweet createFromTaggedTokens(Tweet tweet,
      List<String> tokens, List<String> preprocessedTokens,
      List<TaggedToken> taggedTokens, Map<Integer, Double> featureVector) {
    return new FeaturedTweet(tweet, tokens, preprocessedTokens, null,
        taggedTokens, null, featureVector);
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

}
