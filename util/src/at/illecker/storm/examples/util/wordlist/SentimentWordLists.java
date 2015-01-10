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
package at.illecker.storm.examples.util.wordlist;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.StringUtils;
import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordnet.POSTag;
import at.illecker.storm.examples.util.wordnet.WordNet;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentWordLists {
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentWordLists.class);
  private static final boolean LOGGING = false;
  private static final SentimentWordLists instance = new SentimentWordLists();

  private WordNet m_wordnet;
  private Map<String, Double> m_wordList = null;
  private WordListMap<Double> m_wordListMap = null;

  private SentimentWordLists() {
    m_wordnet = WordNet.getInstance();
    Map<String, Properties> wordLists = Configuration.getSentimentWordlists();
    for (Map.Entry<String, Properties> wordListEntry : wordLists.entrySet()) {
      String file = wordListEntry.getKey();
      Properties props = wordListEntry.getValue();
      String separator = props.getProperty("separator");
      boolean containsRegex = (Boolean) props.get("containsRegex");
      boolean featureScaling = (Boolean) props.get("featureScaling");
      double minValue = (Double) props.get("minValue");
      double maxValue = (Double) props.get("maxValue");

      if (containsRegex) {
        LOG.info("Load WordListMap including Regex from: " + file);
        if (m_wordListMap == null) {
          m_wordListMap = FileUtils.readWordListMap(file, separator,
              featureScaling, minValue, maxValue);
        } else {
          WordListMap<Double> wordListMap = FileUtils.readWordListMap(file,
              separator, featureScaling, minValue, maxValue);
          for (Map.Entry<String, Double> entry : wordListMap.entrySet()) {
            if (!m_wordListMap.containsKey(entry.getKey())) {
              m_wordListMap.put(entry.getKey(), entry.getValue());
            }
          }
        }
      } else {
        LOG.info("Load WordList from: " + file);
        if (m_wordList == null) {
          m_wordList = FileUtils.readFile(file, separator, featureScaling,
              minValue, maxValue);
        } else {
          Map<String, Double> wordList = FileUtils.readFile(file, separator,
              featureScaling, minValue, maxValue);
          for (Map.Entry<String, Double> entry : wordList.entrySet()) {
            if (!m_wordList.containsKey(entry.getKey())) {
              m_wordList.put(entry.getKey(), entry.getValue());
            }
          }
        }
      }
    }
  }

  public static SentimentWordLists getInstance() {
    return instance;
  }

  public void close() {
    m_wordnet.close();
  }

  public Double getWordSentiment(String word) {
    Double sentimentScore = null;
    // First check word lists
    if (m_wordList != null) {
      sentimentScore = m_wordList.get(word);
    }

    // Second check word list maps including regex
    if ((sentimentScore == null) && (m_wordListMap != null)) {
      sentimentScore = m_wordListMap.matchKey(word);
    }

    if (LOGGING) {
      LOG.info("getWordSentiment('" + word + "'): " + sentimentScore);
    }
    return sentimentScore;
  }

  public Double getWordSentimentWithStemming(String word, String pennTag) {
    // convert pennTag to POS (NOUN, VERB, ADJECTIVE, ADVERB)
    POS posTag = POSTag.convertString(pennTag);

    // check for Hashtags
    if (pennTag.equals("HT") && (word.length() > 1)) {
      if (word.indexOf('@') == 1) {
        word = word.substring(2); // check for #@ HASHTAG_USER
      } else {
        word = word.substring(1);
      }
    } else if (pennTag.equals("UH")) {
      if (word.length() == 1) {
        return null; // ignore single char interjection
      }
    } else if ((posTag == null) || (StringUtils.consitsOfPunctuations(word))) {
      // ignore punctuation and all non valid POS tags
      return null;
    }

    if (!pennTag.equals("UH")) {
      word = word.toLowerCase();
    }

    Double sentimentScore = getWordSentiment(word);
    // use word stemming if sentimentScore is null
    if (sentimentScore == null) {
      if (LOGGING) {
        LOG.info(" findStems for (" + word + "," + posTag + ")");
      }
      List<String> stemmedWords = m_wordnet.findStems(word, posTag);
      for (String stemmedWord : stemmedWords) {
        if (!stemmedWord.equals(word)) {
          sentimentScore = getWordSentiment(stemmedWord);
        }
        if (sentimentScore != null) {
          break;
        }
      }
    }
    if (LOGGING) {
      LOG.info("getWordSentimentWithStemming('" + word + "'\'" + posTag
          + "'): " + sentimentScore);
    }
    return sentimentScore;
  }

  public Double getSentenceSentiment(List<HasWord> sentence) {
    double sentenceScore = 0;
    int count = 0;
    for (HasWord w : sentence) {
      String word = w.word().toLowerCase().trim();
      Double wordSentiment = getWordSentiment(word);
      if (wordSentiment != null) {
        sentenceScore += wordSentiment;
        count++;
      }
    }
    return (count > 0) ? sentenceScore / count : null;
  }

  public SentimentResult getTaggedSentenceSentiment(List<TaggedWord> sentence) {
    SentimentResult sentimentResult = new SentimentResult(0, 0.5);
    for (TaggedWord w : sentence) {
      Double wordSentiment = getWordSentimentWithStemming(w.word(), w.tag());
      if (wordSentiment != null) {
        if (wordSentiment < 0.45) { // NEGATIV
          sentimentResult.incNegCount();
          if (wordSentiment < sentimentResult.getMaxNeg()) { // MAX_NEG_SCORE
            sentimentResult.setMaxNeg(wordSentiment);
          }
        } else if (wordSentiment > 0.55) { // POSITIV
          sentimentResult.incPosCount();
          if (wordSentiment > sentimentResult.getMaxPos()) { // MAX_POS_SCORE
            sentimentResult.setMaxPos(wordSentiment);
          }
        } else if ((wordSentiment <= 0.55) && (wordSentiment >= 0.45)) {
          sentimentResult.incNeutralCount(); // NEUTRAL
        }
        // add score value
        sentimentResult.addScore(wordSentiment);
      }
    }

    return (sentimentResult.getCount() > 0) ? sentimentResult : null;
  }

  public SentimentResult getTweetSentiment(Tweet tweet) {
    SentimentResult tweetSentiment = new SentimentResult();
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      if (LOGGING) {
        LOG.info("taggedSentence: " + sentence.toString());
      }
      SentimentResult sentenceSentiment = getTaggedSentenceSentiment(sentence);
      if (sentenceSentiment != null) {
        tweetSentiment.add(sentenceSentiment);
      }
    }
    return (tweetSentiment.getCount() > 0) ? tweetSentiment : null;
  }

  public static void main(String[] args) {
    Preprocessor preprocessor = Preprocessor.getInstance();
    POSTagger posTagger = POSTagger.getInstance();
    SentimentWordLists sentimentWordLists = SentimentWordLists.getInstance();

    for (Tweet tweet : Tweet.getTestTweets()) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);

      // Preprocess
      List<String> preprocessedTokens = preprocessor.preprocess(tokens);
      tweet.addPreprocessedSentence(preprocessedTokens);

      // POS Tagging
      List<TaggedWord> taggedSentence = posTagger
          .tagSentence(preprocessedTokens);
      tweet.addTaggedSentence(taggedSentence);

      // Calculate Sentiment
      SentimentResult sentimentResult = sentimentWordLists
          .getTaggedSentenceSentiment(taggedSentence);

      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Sentiment: " + sentimentResult);
    }

    sentimentWordLists.close();
  }
}
