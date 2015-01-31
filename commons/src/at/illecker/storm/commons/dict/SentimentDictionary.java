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
package at.illecker.storm.commons.dict;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.StringUtils;
import at.illecker.storm.commons.util.io.FileUtils;
import at.illecker.storm.commons.wordnet.POSTag;
import at.illecker.storm.commons.wordnet.WordNet;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentDictionary {
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentDictionary.class);
  private static final boolean LOGGING = false;
  private static final SentimentDictionary INSTANCE = new SentimentDictionary();

  private WordNet m_wordnet;
  private List<Map<String, Double>> m_wordLists = null;
  private List<WordListMap<Double>> m_wordListMaps = null;

  private SentimentDictionary() {
    m_wordnet = WordNet.getInstance();
    m_wordLists = new ArrayList<Map<String, Double>>();
    m_wordListMaps = new ArrayList<WordListMap<Double>>();

    List<Map> wordLists = Configuration.getSentimentWordlists();
    for (Map wordListEntry : wordLists) {
      String file = (String) wordListEntry.get("path");
      String separator = (String) wordListEntry.get("delimiter");
      boolean containsPOSTags = (Boolean) wordListEntry.get("containsPOSTags");
      boolean containsRegex = (Boolean) wordListEntry.get("containsRegex");
      boolean featureScaling = (Boolean) wordListEntry.get("featureScaling");
      double minValue = (Double) wordListEntry.get("minValue");
      double maxValue = (Double) wordListEntry.get("maxValue");
      boolean isEnabled = (Boolean) wordListEntry.get("enabled");
      if (isEnabled) {
        if (containsRegex) {
          LOG.info("Load WordListMap including Regex from: " + file);
          m_wordListMaps.add(FileUtils.readWordListMap(file, separator,
              containsPOSTags, featureScaling, minValue, maxValue));
        } else {
          LOG.info("Load WordList from: " + file);
          m_wordLists.add(FileUtils.readFile(file, separator, containsPOSTags,
              featureScaling, minValue, maxValue));
        }
      }
    }
  }

  public static SentimentDictionary getInstance() {
    return INSTANCE;
  }

  public void close() {
    m_wordnet.close();
  }

  public int getSentimentWordListCount() {
    return m_wordLists.size() + m_wordListMaps.size();
  }

  public Map<Integer, Double> getWordSentiments(String word) {
    Map<Integer, Double> sentimentScores = new HashMap<Integer, Double>();

    // 1) check wordLists
    for (int i = 0; i < m_wordLists.size(); i++) {
      Double sentimentScore = m_wordLists.get(i).get(word);
      if (sentimentScore != null) {
        sentimentScores.put(i, sentimentScore);
      }
    }

    // 2) check wordListMaps including regex
    int wordListMapOffset = m_wordLists.size();
    for (int i = 0; i < m_wordListMaps.size(); i++) {
      Double sentimentScore = m_wordListMaps.get(i).matchKey(word);
      if (sentimentScore != null) {
        sentimentScores.put(i + wordListMapOffset, sentimentScore);
      }
    }

    if (LOGGING) {
      LOG.info("getWordSentiment('" + word + "'): " + sentimentScores);
    }

    return (sentimentScores.size() > 0) ? sentimentScores : null;
  }

  public Map<Integer, Double> getWordSentimentWithStemming(String word,
      String pennTag) {
    // convert pennTag to POS (NOUN, VERB, ADJECTIVE, ADVERB)
    POS posTag = POSTag.convertString(pennTag);

    // check for Hashtags
    if (pennTag.equals("HT") && (word.length() > 1)) {
      if (word.indexOf('@') == 1) {
        word = word.substring(2); // check for #@ HASHTAG_USER
      } else {
        word = word.substring(1);
      }
    } else if ((!pennTag.equals("UH"))
        && StringUtils.consitsOfPunctuations(word)) {
      // ignore all punctuations except emoticons
      return null;
    } else if (StringUtils.consitsOfUnderscores(word)) {
      // ignore tokens with one or more underscores
      return null;
    }

    if (!pennTag.equals("UH")) {
      word = word.toLowerCase();
    }

    Map<Integer, Double> sentimentScores = getWordSentiments(word);
    // use word stemming if sentimentScore is null
    if (sentimentScores == null) {
      if (LOGGING) {
        LOG.info("findStems for (" + word + "," + posTag + ")");
      }
      List<String> stemmedWords = m_wordnet.findStems(word, posTag);
      for (String stemmedWord : stemmedWords) {
        if (!stemmedWord.equals(word)) {
          sentimentScores = getWordSentiments(stemmedWord);
        }
        if (sentimentScores != null) {
          break;
        }
      }
    }

    if (LOGGING) {
      LOG.info("getWordSentimentWithStemming('" + word + "'\'" + posTag
          + "'): " + sentimentScores);
    }
    return sentimentScores;
  }

  public Map<Integer, SentimentResult> getSentenceSentiment(
      List<TaggedWord> sentence) {
    Map<Integer, SentimentResult> sentenceSentiments = new HashMap<Integer, SentimentResult>();
    if (LOGGING) {
      LOG.info("TaggedSentence: " + sentence.toString());
    }
    for (TaggedWord w : sentence) {
      Map<Integer, Double> wordSentiments = getWordSentimentWithStemming(
          w.word(), w.tag());

      if (wordSentiments != null) {
        for (Map.Entry<Integer, Double> wordSentiment : wordSentiments
            .entrySet()) {

          int key = wordSentiment.getKey();
          double sentimentScore = wordSentiment.getValue();

          SentimentResult sentimentResult = sentenceSentiments.get(key);
          if (sentimentResult == null) {
            sentimentResult = new SentimentResult();
          }

          // add score value
          sentimentResult.addScore(sentimentScore);

          // update sentimentResult
          sentenceSentiments.put(key, sentimentResult);
        }
      }
    }
    if (LOGGING) {
      LOG.info("Sentiment: " + sentenceSentiments);
    }
    return (sentenceSentiments.size() > 0) ? sentenceSentiments : null;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    SentimentDictionary sentimentWordLists = SentimentDictionary.getInstance();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      List<TaggedWord> preprocessedTokens = preprocessor.preprocess(tokens);

      // POS Tagging
      List<TaggedWord> taggedTokens = posTagger.tagSentence(preprocessedTokens);

      // Calculate Sentiment
      Map<Integer, SentimentResult> sentimentResult = sentimentWordLists
          .getSentenceSentiment(taggedTokens);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Sentiment: " + sentimentResult);
    }

    sentimentWordLists.close();
  }

}
