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
package at.illecker.storm.examples.util.unsupervised;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.unsupervised.sentiwordnet.SentiWordNet;
import at.illecker.storm.examples.util.unsupervised.wordnet.POSTag;
import at.illecker.storm.examples.util.unsupervised.wordnet.WordNet;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;

public class UnsupervisedSentimentAnalysis {

  public static final String SENTIMENT_WORD_LIST1 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists"
      + File.separator
      + "SentStrength_Data_Sept2011_EmotionLookupTable.txt";
  public static final String SENTIMENT_WORD_LIST2 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "AFINN-111.txt";
  private static final Logger LOG = LoggerFactory
      .getLogger(UnsupervisedSentimentAnalysis.class);

  private static UnsupervisedSentimentAnalysis instance = new UnsupervisedSentimentAnalysis();
  private WordNet m_wordnet;
  private SentiWordNet m_sentiwordnet;
  // AFINN word list (minValue -5 and maxValue +5)
  private WordListMap<Double> m_wordList1;
  // SentStrength word list (minValue -5 and maxValue +5)
  private WordListMap<Double> m_wordList2;

  private UnsupervisedSentimentAnalysis() {
    m_wordnet = WordNet.getInstance();
    m_sentiwordnet = SentiWordNet.getInstance();

    try {
      m_wordList1 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST1), -5, 5);
      m_wordList2 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST2), -5, 5);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static UnsupervisedSentimentAnalysis getInstance() {
    return instance;
  }

  public Double getWordSentiment(String word) {
    // check SentStrength word list
    Double sentimentScore = null;
    if (m_wordList1 != null) {
      // get normalized score between 0 and 1
      sentimentScore = m_wordList1.matchKey(word, true);
    }
    // check AFINN word list
    if (sentimentScore == null) {
      if (m_wordList2 != null) {
        // get normalized score between 0 and 1
        sentimentScore = m_wordList2.matchKey(word, true);
      }
    }
    LOG.info("getWordSentiment('" + word + "'): " + sentimentScore);
    return sentimentScore;
  }

  public Double getWordSentimentWithStemming(String word, POS posTag) {
    Double sentimentScore = getWordSentiment(word);
    // use word stemming
    if (sentimentScore == null) {
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
    return sentimentScore;
  }

  public double getSentenceSentiment(List<HasWord> sentence) {
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
    return (count > 0) ? sentenceScore / count : 0;
  }

  public double getTaggedSentenceSentiment(List<TaggedWord> sentence) {
    double sentenceScore = 0;
    int count = 0;
    for (TaggedWord w : sentence) {
      String word = w.word().toLowerCase().trim();
      Double wordSentiment = getWordSentimentWithStemming(word,
          POSTag.convertString(w.tag()));
      if (wordSentiment != null) {
        sentenceScore += wordSentiment;
        count++;
      }
    }
    return (count > 0) ? sentenceScore / count : 0;
  }

  public double getTweetSentiment(Tweet tweet) {
    double totalScore = 0;
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      totalScore += getTaggedSentenceSentiment(sentence);
    }
    return totalScore;
  }

  public void close() {
    m_sentiwordnet.close();
  }

  public static void main(String[] args) {
    UnsupervisedSentimentAnalysis analysis = UnsupervisedSentimentAnalysis
        .getInstance();

    analysis.close();
  }
}
