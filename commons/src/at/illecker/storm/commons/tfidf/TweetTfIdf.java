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
package at.illecker.storm.commons.tfidf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.dict.StopWords;
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.StringUtils;
import at.illecker.storm.commons.wordnet.POSTag;
import at.illecker.storm.commons.wordnet.WordNet;
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.TaggedWord;

/**
 * Tweet Term Frequency - Inverse Document Frequency
 * 
 */
public class TweetTfIdf {
  private static final Logger LOG = LoggerFactory.getLogger(TweetTfIdf.class);

  private TfType m_tfType;
  private TfIdfNormalization m_tfIdfNormalization;
  private List<Map<String, Double>> m_termFreqs;
  private Map<String, Double> m_inverseDocFreq;
  private Map<String, Integer> m_termIds;
  private boolean m_usePOSTags;

  private TweetTfIdf(TfType type, TfIdfNormalization normalization,
      boolean usePOSTags) {
    this.m_tfType = type;
    this.m_tfIdfNormalization = normalization;
    this.m_usePOSTags = usePOSTags;
  }

  public TfType getTfType() {
    return m_tfType;
  }

  public TfIdfNormalization getTfIdfNormalization() {
    return m_tfIdfNormalization;
  }

  public List<Map<String, Double>> getTermFreqs() {
    return m_termFreqs;
  }

  public Map<String, Double> getInverseDocFreq() {
    return m_inverseDocFreq;
  }

  public Map<String, Integer> getTermIds() {
    return m_termIds;
  }

  public Map<String, Double> tfIdfFromTaggedWords(List<TaggedWord> tweet) {
    return TfIdf.tfIdf(tfFromTaggedWords(tweet, m_tfType, m_usePOSTags),
        m_inverseDocFreq, m_tfIdfNormalization);
  }

  public Map<String, Double> tfIdfFromTaggedTokens(List<TaggedToken> tweet) {
    return TfIdf.tfIdf(tfFromTaggedTokens(tweet, m_tfType, m_usePOSTags),
        m_inverseDocFreq, m_tfIdfNormalization);
  }

  public static TweetTfIdf createFromTaggedWords(List<List<TaggedWord>> tweets,
      boolean usePOSTags) {
    return createFromTaggedWords(tweets, TfType.RAW, TfIdfNormalization.NONE,
        usePOSTags);
  }

  public static TweetTfIdf createFromTaggedWords(List<List<TaggedWord>> tweets,
      TfType type, TfIdfNormalization normalization, boolean usePOSTags) {

    TweetTfIdf tweetTfIdf = new TweetTfIdf(type, normalization, usePOSTags);

    tweetTfIdf.m_termFreqs = tfTaggedWordTweets(tweets, type, usePOSTags);
    tweetTfIdf.m_inverseDocFreq = idf(tweetTfIdf.m_termFreqs);

    tweetTfIdf.m_termIds = new HashMap<String, Integer>();
    int i = 0;
    for (String key : tweetTfIdf.m_inverseDocFreq.keySet()) {
      tweetTfIdf.m_termIds.put(key, i);
      i++;
    }

    LOG.info("Found " + tweetTfIdf.m_inverseDocFreq.size() + " terms");
    // Debug
    // print("Term Frequency", m_termFreqs, m_inverseDocFreq);
    // print("Inverse Document Frequency", m_inverseDocFreq);
    return tweetTfIdf;
  }

  public static TweetTfIdf createFromTaggedTokens(
      List<List<TaggedToken>> tweets, boolean usePOSTags) {
    return createFromTaggedTokens(tweets, TfType.RAW, TfIdfNormalization.NONE,
        usePOSTags);
  }

  public static TweetTfIdf createFromTaggedTokens(
      List<List<TaggedToken>> tweets, TfType type,
      TfIdfNormalization normalization, boolean usePOSTags) {

    TweetTfIdf tweetTfIdf = new TweetTfIdf(type, normalization, usePOSTags);

    tweetTfIdf.m_termFreqs = tfTaggedTokenTweets(tweets, type, usePOSTags);
    tweetTfIdf.m_inverseDocFreq = idf(tweetTfIdf.m_termFreqs);

    tweetTfIdf.m_termIds = new HashMap<String, Integer>();
    int i = 0;
    for (String key : tweetTfIdf.m_inverseDocFreq.keySet()) {
      tweetTfIdf.m_termIds.put(key, i);
      i++;
    }

    LOG.info("Found " + tweetTfIdf.m_inverseDocFreq.size() + " terms");
    // Debug
    // print("Term Frequency", m_termFreqs, m_inverseDocFreq);
    // print("Inverse Document Frequency", m_inverseDocFreq);
    return tweetTfIdf;
  }

  public static List<Map<String, Double>> tfTaggedWordTweets(
      List<List<TaggedWord>> tweets, TfType type, boolean usePOSTags) {
    List<Map<String, Double>> termFreqs = new ArrayList<Map<String, Double>>();
    for (List<TaggedWord> tweet : tweets) {
      termFreqs.add(tfFromTaggedWords(tweet, type, usePOSTags));
    }
    return termFreqs;
  }

  public static List<Map<String, Double>> tfTaggedTokenTweets(
      List<List<TaggedToken>> tweets, TfType type, boolean usePOSTags) {
    List<Map<String, Double>> termFreqs = new ArrayList<Map<String, Double>>();
    for (List<TaggedToken> tweet : tweets) {
      termFreqs.add(tfFromTaggedTokens(tweet, type, usePOSTags));
    }
    return termFreqs;
  }

  public static Map<String, Double> tfFromTaggedWords(List<TaggedWord> tweet,
      TfType type, boolean usePOSTags) {
    Map<String, Double> termFreq = new LinkedHashMap<String, Double>();
    WordNet wordNet = WordNet.getInstance();
    StopWords stopWords = StopWords.getInstance();

    List<String> words = new ArrayList<String>();
    for (TaggedWord taggedWord : tweet) {
      String word = taggedWord.word().toLowerCase();
      String pennTag = taggedWord.tag();

      if ((!pennTag.equals(".")) && (!pennTag.equals(","))
          && (!pennTag.equals(":")) && (!pennTag.equals("''"))
          && (!pennTag.equals("(")) && (!pennTag.equals(")"))
          && (!pennTag.equals("URL")) && (!pennTag.equals("USR"))
          && (!pennTag.equals("CC")) && (!pennTag.equals("CD"))
          && (!pennTag.equals("SYM")) && (!pennTag.equals("POS"))
          && (!stopWords.isStopWord(word))) {

        // Remove hashtag
        if (pennTag.equals("HT")) {
          word = word.substring(1);
        }

        // Check if word consists of punctuations
        // if (StringUtils.consitsOfPunctuations(word)
        // && (!pennTag.equals("POS"))) {
        // continue;
        // }

        // Check if word starts with an alphabet
        if (!StringUtils.startsWithAlphabeticChar(word)) {
          continue;
        }

        POS posTag = POSTag.convertPTB(pennTag);
        // LOG.info("word: '" + word + "' pennTag: '" + pennTag + "' tag: '"
        // + posTag + "'");

        // word stemming
        List<String> stems = wordNet.findStems(word, posTag);
        if (!stems.isEmpty()) {
          word = stems.get(0);
        }

        // add word to term frequency
        if (usePOSTags) {
          words.add(word
              + ((posTag != null) ? "#" + POSTag.toString(posTag) : ""));
        } else {
          words.add(word);
        }
      }
    }
    termFreq = TfIdf.tf(termFreq, words);
    termFreq = TfIdf.normalizeTf(termFreq, type);
    return termFreq;
  }

  public static Map<String, Double> tfFromTaggedTokens(List<TaggedToken> tweet,
      TfType type, boolean usePOSTags) {
    Map<String, Double> termFreq = new LinkedHashMap<String, Double>();
    WordNet wordNet = WordNet.getInstance();
    StopWords stopWords = StopWords.getInstance();

    List<String> words = new ArrayList<String>();
    for (TaggedToken taggedToken : tweet) {
      String word = taggedToken.token.toLowerCase();
      String arkTag = taggedToken.tag;

      // http://www.ark.cs.cmu.edu/TweetNLP/annot_guidelines.pdf
      if ((!arkTag.equals(",")) && (!arkTag.equals("$"))
          && (!arkTag.equals("G")) && (!arkTag.equals("@"))
          && (!arkTag.equals("~")) && (!arkTag.equals("U"))
          && (!stopWords.isStopWord(word))) {

        // Remove hashtag
        if (arkTag.equals("#")) {
          word = word.substring(1);
        }

        // Check if word consists of punctuations
        // if (StringUtils.consitsOfPunctuations(word)
        // && (!pennTag.equals("POS"))) {
        // continue;
        // }

        // Check if word starts with an alphabet
        if (!StringUtils.startsWithAlphabeticChar(word)) {
          continue;
        }

        POS posTag = POSTag.convertArk(arkTag);
        // LOG.info("word: '" + word + "' pennTag: '" + pennTag + "' tag: '"
        // + posTag + "'");

        // word stemming
        List<String> stems = wordNet.findStems(word, posTag);
        if (!stems.isEmpty()) {
          word = stems.get(0);
        }

        // add word to term frequency
        if (usePOSTags) {
          words.add(word
              + ((posTag != null) ? "#" + POSTag.toString(posTag) : ""));
        } else {
          words.add(word);
        }
      }
    }
    termFreq = TfIdf.tf(termFreq, words);
    termFreq = TfIdf.normalizeTf(termFreq, type);
    return termFreq;
  }

  public static Map<String, Double> idf(List<Map<String, Double>> termFreq) {
    return TfIdf.idf(termFreq);
  }

  public static List<Map<String, Double>> tfIdf(
      List<Map<String, Double>> termFreqs, Map<String, Double> inverseDocFreq,
      TfIdfNormalization normalization) {

    List<Map<String, Double>> tfIdf = new ArrayList<Map<String, Double>>();
    // compute tfIdf for each document
    for (Map<String, Double> doc : termFreqs) {
      tfIdf.add(TfIdf.tfIdf(doc, inverseDocFreq, normalization));
    }

    return tfIdf;
  }

  public static void print(String title, Map<String, Double> inverseDocFreq) {
    // print title
    LOG.info(String.format("=== %s ===", title));
    // print values
    for (Map.Entry<String, Double> term : inverseDocFreq.entrySet()) {
      String line = String.format("%15s", term.getKey());
      line += String.format("%8.4f", term.getValue());
      LOG.info(line);
    }
  }

  public static void print(String title, List<Map<String, Double>> termFreqs,
      Map<String, Double> inverseDocFreq) {
    // print title
    LOG.info(String.format("=== %s ===", title));

    // print header
    String line = String.format("%15s", " ");
    for (int i = 0; i < termFreqs.size(); i++) {
      line += String.format("%8s", " " + i);
    }
    LOG.info(line);

    // print values
    for (Map.Entry<String, Double> term : inverseDocFreq.entrySet()) {
      line = String.format("%15s", term.getKey());
      for (Map<String, Double> termFreq : termFreqs) {
        line += String.format("%8.4f", termFreq.get(term.getKey()));
      }
      LOG.info(line);
    }
  }

  public static void main(String[] args) {
    boolean extendedTest = false;
    boolean useArkPOSTagger = true;
    boolean usePOSTags = true; // use POS tags in terms

    Preprocessor preprocessor = Preprocessor.getInstance();
    GatePOSTagger gatePOSTagger = null;
    ArkPOSTagger arkPOSTagger = null;

    if (useArkPOSTagger) {
      arkPOSTagger = ArkPOSTagger.getInstance();
    } else {
      gatePOSTagger = GatePOSTagger.getInstance();
    }

    // load tweets
    List<Tweet> tweets = null;
    if (extendedTest) {
      // SemEval2013
      Dataset dataset = Configuration.getDataSetSemEval2013();
      tweets = dataset.getTrainTweets(true);
    } else {
      tweets = Tweet.getTestTweets();
    }

    // Tokenize
    List<List<String>> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    if (useArkPOSTagger) {
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

      // Ark TF-IDF
      List<Map<String, Double>> termFreqs = TweetTfIdf.tfTaggedTokenTweets(
          taggedTweets, TfType.LOG, usePOSTags);
      Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);
      List<Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
          inverseDocFreq, TfIdfNormalization.COS);

      LOG.info("Found " + inverseDocFreq.size() + " terms");
      print("Term Frequency", termFreqs, inverseDocFreq);
      print("Inverse Document Frequency", inverseDocFreq);
      print("Tf-Idf", tfIdf, inverseDocFreq);

    } else {
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

      // Gate TF-IDF
      List<Map<String, Double>> termFreqs = TweetTfIdf.tfTaggedWordTweets(
          taggedTweets, TfType.LOG, usePOSTags);
      Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);
      List<Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
          inverseDocFreq, TfIdfNormalization.COS);

      LOG.info("Found " + inverseDocFreq.size() + " terms");
      print("Term Frequency", termFreqs, inverseDocFreq);
      print("Inverse Document Frequency", inverseDocFreq);
      print("Tf-Idf", tfIdf, inverseDocFreq);
    }
  }

}
