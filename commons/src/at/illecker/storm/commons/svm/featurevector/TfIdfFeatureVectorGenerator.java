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

public class TfIdfFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(TfIdfFeatureVectorGenerator.class);
  private static final boolean LOGGING = Configuration.get(
      "commons.featurevectorgenerator.tfidf.logging", false);

  private TweetTfIdf m_tweetTfIdf = null;
  private SentimentDictionary m_sentimentDict;
  private int m_vectorStartId = 1;

  public TfIdfFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_tweetTfIdf = tweetTfIdf;
    this.m_sentimentDict = SentimentDictionary.getInstance();
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public TfIdfFeatureVectorGenerator(TweetTfIdf tweetTfIdf, int vectorStartId) {
    this(tweetTfIdf);
    this.m_vectorStartId = vectorStartId;
  }

  public SentimentDictionary getSentimentDictionary() {
    return m_sentimentDict;
  }

  @Override
  public int getFeatureVectorSize() {
    return m_tweetTfIdf.getInverseDocFreq().size();
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedWords(
      List<TaggedWord> tweet) {
    return generateFeatureVector(m_tweetTfIdf.tfIdfTaggedWord(tweet));
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedTokens(
      List<TaggedToken> tweet) {
    return generateFeatureVector(m_tweetTfIdf.tfIdfTaggedToken(tweet));
  }

  public Map<Integer, Double> generateFeatureVector(Map<String, Double> tfIdf) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    if (m_tweetTfIdf != null) {
      // Map<String, Double> idf = m_tweetTfIdf.getInverseDocFreq();
      Map<String, Integer> termIds = m_tweetTfIdf.getTermIds();

      for (Map.Entry<String, Double> element : tfIdf.entrySet()) {
        String key = element.getKey();
        if (termIds.containsKey(key)) {
          int vectorId = m_vectorStartId + termIds.get(key);
          resultFeatureVector.put(vectorId, element.getValue());
        }
      }
    }
    if (LOGGING) {
      LOG.info("TfIdsFeatureVector: " + resultFeatureVector);
    }
    return resultFeatureVector;
  }

  public static void main(String[] args) {
    boolean useArkPOSTagger = true;
    boolean usePOSTags = true; // use POS tags in terms
    Preprocessor preprocessor = Preprocessor.getInstance();

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

      // Generate TfIdfFeatureVectorGenerator
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedTokens(taggedTweets,
          TfType.RAW, TfIdfNormalization.COS, usePOSTags);
      TfIdfFeatureVectorGenerator efvg = new TfIdfFeatureVectorGenerator(
          tweetTfIdf);

      // Debug
      if (LOGGING) {
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

      // TF-IDF Feature Vector Generation
      for (List<TaggedToken> taggedTokens : taggedTweets) {
        Map<Integer, Double> tfIdfFeatureVector = efvg
            .generateFeatureVectorFromTaggedTokens(taggedTokens);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : tfIdfFeatureVector.entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }

        LOG.info("Tweet: '" + taggedTokens + "'");
        LOG.info("TF-IDF FeatureVector: " + featureVectorStr);
      }

      efvg.getSentimentDictionary().close();

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

      // Generate TfIdfFeatureVectorGenerator
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedWords(taggedTweets,
          TfType.RAW, TfIdfNormalization.COS, usePOSTags);
      TfIdfFeatureVectorGenerator efvg = new TfIdfFeatureVectorGenerator(
          tweetTfIdf);

      // Debug
      if (LOGGING) {
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

      // TF-IDF Feature Vector Generation
      for (List<TaggedWord> taggedWords : taggedTweets) {
        Map<Integer, Double> tfIdfFeatureVector = efvg
            .generateFeatureVectorFromTaggedWords(taggedWords);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : tfIdfFeatureVector.entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }

        LOG.info("Tweet: '" + taggedWords + "'");
        LOG.info("TF-IDF FeatureVector: " + featureVectorStr);
      }

      efvg.getSentimentDictionary().close();
    }
  }

}
