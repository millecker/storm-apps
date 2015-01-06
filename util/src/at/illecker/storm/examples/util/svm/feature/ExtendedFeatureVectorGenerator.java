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

import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
import edu.stanford.nlp.ling.TaggedWord;

public class ExtendedFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(ExtendedFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;

  private SimpleFeatureVectorGenerator m_sfvg;
  private SentimentWordLists m_sentimentWordLists;
  private TweetTfIdf m_tweetTfIdf = null;

  public ExtendedFeatureVectorGenerator(TweetTfIdf tweetTfIdf) {
    this.m_tweetTfIdf = tweetTfIdf;
    this.m_sfvg = SimpleFeatureVectorGenerator.getInstance();
    this.m_sentimentWordLists = m_sfvg.getSentimentWordLists();
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  @Override
  public int getFeatureVectorSize() {
    return m_sfvg.getFeatureVectorSize()
        + m_tweetTfIdf.getInverseDocFreq().size();
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(Tweet tweet) {
    Map<Integer, Double> resultFeatureVector = m_sfvg
        .calculateFeatureVector(tweet);

    if (m_tweetTfIdf != null) {
      Map<String, Double> idf = m_tweetTfIdf.getInverseDocFreq();
      Map<String, Integer> termIds = m_tweetTfIdf.getTermIds();
      Map<String, Double> tfIdf = m_tweetTfIdf.tfIdf(tweet);

      for (Map.Entry<String, Double> element : tfIdf.entrySet()) {
        String key = element.getKey();
        if (idf.containsKey(key)) {
          int id = m_sfvg.getFeatureVectorSize() + 1 + termIds.get(key);
          resultFeatureVector.put(id, element.getValue());
        }
      }

    }
    // LOG.info("TfIdsVector: " + resultFeatureVector);

    return resultFeatureVector;
  }

  public static void main(String[] args) {
    List<Tweet> tweets = SimpleFeatureVectorGenerator.getTestTweets();
    POSTagger posTagger = POSTagger.getInstance();
    boolean tfIdfusePOSTags = true;

    // prepare Tweets
    for (Tweet tweet : tweets) {
      // tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // POS tagging
      List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);
      tweet.addTaggedSentence(taggedSentence);
    }

    // calculate tfidf
    TweetTfIdf tweetTfIdf = new TweetTfIdf(tweets, tfIdfusePOSTags);
    ExtendedFeatureVectorGenerator efvg = new ExtendedFeatureVectorGenerator(
        tweetTfIdf);

    // debug
    TweetTfIdf.print("Term Frequency", tweetTfIdf.getTermFreqs(),
        tweetTfIdf.getInverseDocFreq());
    TweetTfIdf.print("Inverse Document Frequency",
        tweetTfIdf.getInverseDocFreq());

    // generate Feature Vector
    for (Tweet tweet : tweets) {
      System.out.println("Tweet: " + tweet);
      System.out.print("FeatureVector:");
      for (Map.Entry<Integer, Double> feature : efvg.calculateFeatureVector(
          tweet).entrySet()) {
        System.out.print(" " + feature.getKey() + ":" + feature.getValue());
      }
      System.out.println();
    }

    efvg.getSentimentWordLists().close();
  }
}
