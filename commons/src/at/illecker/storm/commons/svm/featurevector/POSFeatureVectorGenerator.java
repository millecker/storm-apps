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

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public class POSFeatureVectorGenerator extends FeatureVectorGenerator {
  private static final Logger LOG = LoggerFactory
      .getLogger(POSFeatureVectorGenerator.class);
  private static final boolean LOGGING = Configuration.get(
      "commons.featurevectorgenerator.pos.logging", false);
  private int m_vectorStartId = 1;
  private final boolean m_useTaggedWords;
  private final boolean m_normalize;
  private final int m_vectorSize;

  public POSFeatureVectorGenerator(boolean useTaggedWords, boolean normalize) {
    m_useTaggedWords = useTaggedWords;
    m_normalize = normalize;
    m_vectorStartId = 1;
    if (useTaggedWords) {
      m_vectorSize = 7;
    } else {
      m_vectorSize = 8;
    }
    LOG.info("VectorSize: " + m_vectorSize);
  }

  public POSFeatureVectorGenerator(boolean useTaggedWords, boolean normalize,
      int vectorStartId) {
    this(useTaggedWords, normalize);
    this.m_vectorStartId = vectorStartId;
  }

  @Override
  public int getFeatureVectorSize() {
    return m_vectorSize;
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedWords(
      List<TaggedWord> taggedWords) {
    if (!m_useTaggedWords) {
      throw new RuntimeException(
          "Use TaggedWords was set to false! generateFeatureVectorFromTaggedWords is not applicable!");
    }
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();
    double[] posTags = countPOSTagsFromTaggedWords(taggedWords, m_normalize);
    if (posTags != null) {
      if (posTags[0] != 0) // nouns
        resultFeatureVector.put(m_vectorStartId, posTags[0]);
      if (posTags[1] != 0) // verbs
        resultFeatureVector.put(m_vectorStartId + 1, posTags[1]);
      if (posTags[2] != 0) // adjectives
        resultFeatureVector.put(m_vectorStartId + 2, posTags[2]);
      if (posTags[3] != 0) // adverbs
        resultFeatureVector.put(m_vectorStartId + 3, posTags[3]);
      if (posTags[4] != 0) // interjections
        resultFeatureVector.put(m_vectorStartId + 4, posTags[4]);
      if (posTags[5] != 0) // punctuations
        resultFeatureVector.put(m_vectorStartId + 5, posTags[5]);
      if (posTags[6] != 0) // hashtags
        resultFeatureVector.put(m_vectorStartId + 6, posTags[6]);
    }
    if (LOGGING) {
      LOG.info("POStags: " + Arrays.toString(posTags));
    }
    return resultFeatureVector;
  }

  @Override
  public Map<Integer, Double> generateFeatureVectorFromTaggedTokens(
      List<TaggedToken> taggedTokens) {
    if (m_useTaggedWords) {
      throw new RuntimeException(
          "Use TaggedWords was set to true! generateFeatureVectorFromTaggedTokens is not applicable!");
    }
    Map<Integer, Double> resultFeatureVector = new TreeMap<Integer, Double>();
    double[] posTags = countPOSTagsFromTaggedTokens(taggedTokens, m_normalize);
    if (posTags != null) {
      if (posTags[0] != 0) // nouns
        resultFeatureVector.put(m_vectorStartId, posTags[0]);
      if (posTags[1] != 0) // verb
        resultFeatureVector.put(m_vectorStartId + 1, posTags[1]);
      if (posTags[2] != 0) // adjective
        resultFeatureVector.put(m_vectorStartId + 2, posTags[2]);
      if (posTags[3] != 0) // adverb
        resultFeatureVector.put(m_vectorStartId + 3, posTags[3]);
      if (posTags[4] != 0) // interjection
        resultFeatureVector.put(m_vectorStartId + 4, posTags[4]);
      if (posTags[5] != 0) // punctuation
        resultFeatureVector.put(m_vectorStartId + 5, posTags[5]);
      if (posTags[6] != 0) // hashtag
        resultFeatureVector.put(m_vectorStartId + 6, posTags[6]);
      if (posTags[7] != 0) // emoticon
        resultFeatureVector.put(m_vectorStartId + 7, posTags[7]);
    }
    if (LOGGING) {
      LOG.info("POStags: " + Arrays.toString(posTags));
    }
    return resultFeatureVector;
  }

  private double[] countPOSTagsFromTaggedWords(List<TaggedWord> taggedWords,
      boolean normalize) {
    // 7 = [NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION, PUNCTUATION, HASHTAG]
    double[] posTags = new double[] { 0d, 0d, 0d, 0d, 0d, 0d, 0d };
    int wordCount = 0;
    for (TaggedWord word : taggedWords) {
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
    if (normalize) {
      for (int i = 0; i < posTags.length; i++) {
        posTags[i] /= wordCount;
      }
    }
    return posTags;
  }

  private double[] countPOSTagsFromTaggedTokens(List<TaggedToken> taggedTokens,
      boolean normalize) {
    // 10 = [NOUN, PRONOUN, PROPER_NOUN, VERB, ADJECTIVE, ADVERB, INTERJECTION,
    // PUNCTUATION, HASHTAG, EMOTICON]
    double[] posTags = new double[] { 0d, 0d, 0d, 0d, 0d, 0d, 0d, 0d };
    int wordCount = 0;
    for (TaggedToken word : taggedTokens) {
      wordCount++;
      String arkTag = word.tag;
      // http://www.ark.cs.cmu.edu/TweetNLP/annot_guidelines.pdf
      if (arkTag.equals("N") || arkTag.equals("O") || arkTag.equals("ˆ")
          || arkTag.equals("Z")) {
        posTags[0]++;
      } else if (arkTag.equals("V") || arkTag.equals("T")) {
        posTags[1]++;
      } else if (arkTag.equals("A")) {
        posTags[2]++;
      } else if (arkTag.equals("R")) {
        posTags[3]++;
      } else if (arkTag.equals("!")) {
        posTags[4]++;
      } else if (arkTag.equals(",")) {
        posTags[5]++;
      } else if (arkTag.equals("#")) {
        posTags[6]++;
      } else if (arkTag.equals("E")) {
        posTags[7]++;
      }
    }
    if (normalize) {
      for (int i = 0; i < posTags.length; i++) {
        posTags[i] /= wordCount;
      }
    }
    return posTags;
  }

  public static void main(String[] args) {
    boolean useArkPOSTagger = true;
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

      // POS Feature Vector Generation
      FeatureVectorGenerator fvg = new POSFeatureVectorGenerator(false, true);
      for (List<TaggedToken> taggedTokens : taggedTweets) {
        Map<Integer, Double> posFeatureVector = fvg
            .generateFeatureVectorFromTaggedTokens(taggedTokens);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : posFeatureVector.entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }
        LOG.info("Tweet: '" + taggedTokens + "'");
        LOG.info("POSFeatureVector: " + featureVectorStr);
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

      // POS Feature Vector Generation
      FeatureVectorGenerator fvg = new POSFeatureVectorGenerator(true, true);
      for (List<TaggedWord> taggedWords : taggedTweets) {
        Map<Integer, Double> posFeatureVector = fvg
            .generateFeatureVectorFromTaggedWords(taggedWords);

        // Build feature vector string
        String featureVectorStr = "";
        for (Map.Entry<Integer, Double> feature : posFeatureVector.entrySet()) {
          featureVectorStr += " " + feature.getKey() + ":" + feature.getValue();
        }
        LOG.info("Tweet: '" + taggedWords + "'");
        LOG.info("POSFeatureVector: " + featureVectorStr);
      }
    }
  }

}
