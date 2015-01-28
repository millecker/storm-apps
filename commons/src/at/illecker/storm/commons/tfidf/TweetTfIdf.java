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
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.dict.StopWords;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.PreprocessedTweet;
import at.illecker.storm.commons.tweet.TaggedTweet;
import at.illecker.storm.commons.tweet.TokenizedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.StringUtils;
import at.illecker.storm.commons.wordnet.POSTag;
import at.illecker.storm.commons.wordnet.WordNet;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.TaggedWord;

/**
 * Tweet Term Frequency - Inverse Document Frequency
 * 
 */
public class TweetTfIdf {
  private static final Logger LOG = LoggerFactory.getLogger(TweetTfIdf.class);

  private TfType m_type;
  private TfIdfNormalization m_normalization;
  private Map<TaggedTweet, Map<String, Double>> m_termFreqs;
  private Map<String, Double> m_inverseDocFreq;
  private Map<String, Integer> m_termIds;

  private boolean m_usePOSTags;

  public TweetTfIdf(List<TaggedTweet> tweets, boolean usePOSTags) {
    this(tweets, TfType.RAW, TfIdfNormalization.NONE, usePOSTags);
  }

  public TweetTfIdf(List<TaggedTweet> tweets, TfType type,
      TfIdfNormalization normalization, boolean usePOSTags) {
    this.m_type = type;
    this.m_normalization = normalization;
    this.m_usePOSTags = usePOSTags;

    this.m_termFreqs = tf(tweets, type, m_usePOSTags);
    this.m_inverseDocFreq = idf(m_termFreqs);

    this.m_termIds = new HashMap<String, Integer>();
    int i = 0;
    for (String key : m_inverseDocFreq.keySet()) {
      this.m_termIds.put(key, i);
      i++;
    }

    LOG.info("Found " + m_inverseDocFreq.size() + " terms");
    // Debug
    // print("Term Frequency", m_termFreqs, m_inverseDocFreq);
    // print("Inverse Document Frequency", m_inverseDocFreq);
  }

  public Map<TaggedTweet, Map<String, Double>> getTermFreqs() {
    return m_termFreqs;
  }

  public Map<String, Double> getInverseDocFreq() {
    return m_inverseDocFreq;
  }

  public Map<String, Integer> getTermIds() {
    return m_termIds;
  }

  public Map<String, Double> tfIdf(TaggedTweet tweet) {
    return TfIdf.tfIdf(tf(tweet, m_type, m_usePOSTags), m_inverseDocFreq,
        m_normalization);
  }

  public static Map<String, Double> tf(TaggedTweet tweet, TfType type,
      boolean usePOSTags) {
    Map<String, Double> termFreq = new HashMap<String, Double>();
    WordNet wordNet = WordNet.getInstance();
    StopWords stopWords = StopWords.getInstance();

    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      List<String> words = new ArrayList<String>();

      for (TaggedWord taggedWord : sentence) {
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

          POS posTag = POSTag.convertString(pennTag);
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
    }

    termFreq = TfIdf.normalizeTf(termFreq, type);
    return termFreq;
  }

  public static Map<TaggedTweet, Map<String, Double>> tf(
      List<TaggedTweet> tweets, TfType type, boolean usePOSTags) {
    Map<TaggedTweet, Map<String, Double>> termFreqs = new HashMap<TaggedTweet, Map<String, Double>>();
    for (TaggedTweet tweet : tweets) {
      termFreqs.put(tweet, tf(tweet, type, usePOSTags));
    }
    return termFreqs;
  }

  public static Map<String, Double> idf(
      Map<TaggedTweet, Map<String, Double>> termFreq) {
    return TfIdf.idf(termFreq);
  }

  public static Map<TaggedTweet, Map<String, Double>> tfIdf(
      Map<TaggedTweet, Map<String, Double>> termFreqs,
      Map<String, Double> inverseDocFreq, TfIdfNormalization normalization) {

    Map<TaggedTweet, Map<String, Double>> tfIdf = new HashMap<TaggedTweet, Map<String, Double>>();
    // compute tfIdf for each document
    for (Map.Entry<TaggedTweet, Map<String, Double>> doc : termFreqs.entrySet()) {
      tfIdf.put(doc.getKey(),
          TfIdf.tfIdf(doc.getValue(), inverseDocFreq, normalization));
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

  public static void print(String title,
      Map<TaggedTweet, Map<String, Double>> tweetData,
      Map<String, Double> inverseDocFreq) {
    // print title
    LOG.info(String.format("=== %s ===", title));

    // print header
    String line = String.format("%15s", " ");
    for (Map.Entry<TaggedTweet, Map<String, Double>> tweet : tweetData
        .entrySet()) {
      line += String.format("%8s", "Tweet " + tweet.getKey().getId());
    }
    LOG.info(line);

    // print values
    for (Map.Entry<String, Double> term : inverseDocFreq.entrySet()) {
      line = String.format("%15s", term.getKey());
      for (Map.Entry<TaggedTweet, Map<String, Double>> tweet : tweetData
          .entrySet()) {
        line += String.format("%8.4f", tweet.getValue().get(term.getKey()));
      }
      LOG.info(line);
    }
  }

  public static void main(String[] args) {
    List<Tweet> tweets = Tweet.getTestTweets();
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();

    // Tokenize tweets
    List<TokenizedTweet> tokenizedTweets = Tokenizer.tokenizeTweets(tweets);

    // Preprocess
    List<PreprocessedTweet> preprocessedTweets = preprocessor
        .preprocessTweets(tokenizedTweets);

    // POS Tagging
    List<TaggedTweet> taggedTweets = posTagger.tagTweets(preprocessedTweets);

    boolean usePOSTags = true; // use POS tags in terms
    Map<TaggedTweet, Map<String, Double>> termFreqs = TweetTfIdf.tf(
        taggedTweets, TfType.RAW, usePOSTags);
    Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);

    Map<TaggedTweet, Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
        inverseDocFreq, TfIdfNormalization.NONE);

    LOG.info("Found " + inverseDocFreq.size() + " terms");
    print("Term Frequency", termFreqs, inverseDocFreq);
    print("Inverse Document Frequency", inverseDocFreq);
    print("Tf-Idf", tfIdf, inverseDocFreq);
  }
}
