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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.ArraysUtils;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TfIdfNormalization;
import at.illecker.storm.examples.util.tfidf.TfType;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.SentimentWordLists;
import edu.stanford.nlp.ling.TaggedWord;

public class ExtendedFeatureVectorGenerator implements FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(ExtendedFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;
  private static final ExtendedFeatureVectorGenerator instance = new ExtendedFeatureVectorGenerator();
  private SimpleFeatureVectorGenerator m_sfvg;
  private SentimentWordLists m_sentimentWordLists;

  private ExtendedFeatureVectorGenerator() {
    m_sfvg = SimpleFeatureVectorGenerator.getInstance();
    m_sentimentWordLists = m_sfvg.getSentimentWordLists();
  }

  public static ExtendedFeatureVectorGenerator getInstance() {
    return instance;
  }

  public SentimentWordLists getSentimentWordLists() {
    return m_sentimentWordLists;
  }

  public double[] getTfIdsVector(Tweet tweet) {

    Map<Tweet, Map<String, Double>> termFreqs = TweetTfIdf.tf(tweets,
        TfType.RAW);
    
    Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);

    Map<Tweet, Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
        inverseDocFreq, TfIdfNormalization.NONE);
  }
      
  @Override
  public double[] calculateFeatureVector(Tweet tweet) {
    // [POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, MAX_POS_SCORE,
    // MAX_NEG_SCORE]
    // [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAGS]
    double[] resultFeatureVector = m_sfvg.calculateFeatureVector(tweet);

    // TODO
    resultFeatureVector = ArraysUtils.concat(resultFeatureVector, tfIdsVector);

    return resultFeatureVector;
  }

  public static void main(String[] args) {
    POSTagger posTagger = POSTagger.getInstance();
    ExtendedFeatureVectorGenerator efvg = ExtendedFeatureVectorGenerator
        .getInstance();

    for (Tweet tweet : SimpleFeatureVectorGenerator.getTestTweets()) {
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);

      tweet.addTaggedSentence(taggedSentence);

      System.out.println("Tweet: " + tweet);
      System.out.println("FeatureVector: "
          + Arrays.toString(efvg.calculateFeatureVector(tweet)));
    }

    efvg.getSentimentWordLists().close();
  }
}
