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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

public class POSFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(POSFeatureVectorGenerator.class);
  private static final boolean LOGGING = false;
  private static final int VECTOR_SIZE = 7;
  private int m_vectorStartId = 1;

  public POSFeatureVectorGenerator() {
    this.m_vectorStartId = 1;
    LOG.info("VectorSize: " + getFeatureVectorSize());
  }

  public POSFeatureVectorGenerator(int vectorStartId) {
    this();
    this.m_vectorStartId = vectorStartId;
  }

  @Override
  public int getFeatureVectorSize() {
    // VECTOR_SIZE = {Nouns/wc, Verbs/wc, Adjectives/wc, Adverbs/wc,
    // Interjections/wc, Punctuations/wc, Hashtags/wc}
    return VECTOR_SIZE;
  }

  @Override
  public Map<Integer, Double> calculateFeatureVector(
      List<TaggedWord> taggedTokens) {
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();

    double[] posTags = countPOSTags(taggedTokens);
    if (posTags != null) {
      if (posTags[0] != 0) // nouns / wordCount
        resultFeatureVector.put(m_vectorStartId, posTags[0]);
      if (posTags[1] != 0) // verbs / wordCount
        resultFeatureVector.put(m_vectorStartId + 1, posTags[1]);
      if (posTags[2] != 0) // adjectives / wordCount
        resultFeatureVector.put(m_vectorStartId + 2, posTags[2]);
      if (posTags[3] != 0) // adverbs / wordCount
        resultFeatureVector.put(m_vectorStartId + 3, posTags[3]);
      if (posTags[4] != 0) // interjections / wordCount
        resultFeatureVector.put(m_vectorStartId + 4, posTags[4]);
      if (posTags[5] != 0) // punctuations / wordCount
        resultFeatureVector.put(m_vectorStartId + 5, posTags[5]);
      if (posTags[6] != 0) // hashtags / wordCount
        resultFeatureVector.put(m_vectorStartId + 6, posTags[6]);
    }

    if (LOGGING) {
      LOG.info("POStags: " + Arrays.toString(posTags));
    }

    return resultFeatureVector;
  }

  private double[] countPOSTags(List<TaggedWord> taggedTokens) {
    // [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAG]
    double[] posTags = new double[] { 0d, 0d, 0d, 0d, 0d, 0d, 0d };
    int wordCount = 0;
    for (TaggedWord word : taggedTokens) {
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
    // normalize
    for (int i = 0; i < posTags.length; i++) {
      posTags[i] /= wordCount;
    }
    return posTags;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    FeatureVectorGenerator fvg = new POSFeatureVectorGenerator();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      List<TaggedWord> preprocessedTokens = preprocessor.preprocess(tokens);

      // POS Tagging
      List<TaggedWord> taggedTokens = posTagger.tagSentence(preprocessedTokens);

      // POS Feature Vector Generation
      Map<Integer, Double> posFeatureVector = fvg
          .calculateFeatureVector(taggedTokens);

      // Build feature vector string
      String featureVectorStr = "";
      for (Map.Entry<Integer, Double> feature : posFeatureVector.entrySet()) {
        featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
      }

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("TaggedSentence: " + taggedTokens);
      LOG.info("POSFeatureVector: " + featureVectorStr);
    }
  }

}
