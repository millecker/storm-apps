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
package at.illecker.storm.examples.util.unsupervised.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.unsupervised.util.wordnet.POSTag;
import at.illecker.storm.examples.util.unsupervised.util.wordnet.WordNet;
import edu.mit.jwi.item.POS;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;

public class SentimentWordLists {
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
      .getLogger(SentimentWordLists.class);
  private static final SentimentWordLists instance = new SentimentWordLists();

  private WordNet m_wordnet;
  // SentStrength word list (minValue -5 and maxValue +5)
  private WordListMap<Double> m_wordList1;
  // AFINN word list (minValue -5 and maxValue +5)
  private WordListMap<Double> m_wordList2;

  private SentimentWordLists() {
    m_wordnet = WordNet.getInstance();

    try {
      LOG.info("Load SentStrength word list from: " + SENTIMENT_WORD_LIST1);
      m_wordList1 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST1), -5, 5);

      LOG.info("Load AFINN word list from: " + SENTIMENT_WORD_LIST1);
      m_wordList2 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST2), -5, 5);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static SentimentWordLists getInstance() {
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
    // LOG.info("getWordSentimentWithStemming('" + word + "'\'" + posTag +
    // "'): "
    // + sentimentScore);
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
    double tweetScore = 0;
    int count = 0;
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      double sentenceScore = getTaggedSentenceSentiment(sentence);
      if (sentenceScore != 0) {
        tweetScore += sentenceScore;
        count++;
      }
    }
    return (count > 0) ? tweetScore / count : 0;
  }

  public static void main(String[] args) {
    String text = "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)";

    POSTagger posTagger = POSTagger.getInstance();
    List<String> tokens = Tokenizer.tokenize(text);
    List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);

    SentimentWordLists sentimentWordLists = SentimentWordLists.getInstance();

    System.out.println("text: '" + text + "'");
    double sentimentScore = sentimentWordLists
        .getTaggedSentenceSentiment(taggedSentence);
    System.out.println("sentimentScore: " + sentimentScore);
  }
}
