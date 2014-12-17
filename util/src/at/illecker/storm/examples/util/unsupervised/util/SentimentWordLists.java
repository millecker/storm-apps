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
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.io.FileUtil;
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

  public static final String SENTIMENT_WORD_LIST3 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists"
      + File.separator
      + "SentStrength_Data_Sept2011_EmoticonLookupTable.txt";

  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentWordLists.class);
  private static final SentimentWordLists instance = new SentimentWordLists();

  private WordNet m_wordnet;
  // SentStrength word list (minValue -5 and maxValue +5)
  private WordListMap<Double> m_wordList1;
  // AFINN word list (minValue -5 and maxValue +5)
  private Map<String, Double> m_wordList2;
  // SentStrength emoticons (minValue -1 and maxValue +1)
  private Map<String, Double> m_wordList3;

  private SentimentWordLists() {
    m_wordnet = WordNet.getInstance();

    try {
      LOG.info("Load SentStrength word list from: " + SENTIMENT_WORD_LIST1);
      m_wordList1 = FileUtil.readWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST1), "\t", -5, 5);

      LOG.info("Load AFINN word list from: " + SENTIMENT_WORD_LIST1);
      m_wordList2 = FileUtil.readFile(
          new FileInputStream(SENTIMENT_WORD_LIST2), "\t", true, -5, 5);

      LOG.info("Load SentStrength emoticons from: " + SENTIMENT_WORD_LIST3);
      m_wordList3 = FileUtil.readFile(
          new FileInputStream(SENTIMENT_WORD_LIST3), "\t", true, -1, 1);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static SentimentWordLists getInstance() {
    return instance;
  }

  public void close() {
    try {
      m_wordnet.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Double getWordSentiment(String word) {
    Double sentimentScore = null;
    // check AFINN word list
    sentimentScore = m_wordList2.get(word);

    // check Emoticons
    if (sentimentScore == null) {
      sentimentScore = m_wordList3.get(word);
    }

    // check SentStrength word list
    if (sentimentScore == null) {
      sentimentScore = m_wordList1.matchKey(word, true);
      // if (sentimentScore != null) {
      // LOG.info("hit in SentStrength word list...");
      // }
    }

    LOG.info("getWordSentiment('" + word + "'): " + sentimentScore);
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
    } else if ((posTag == null) || (Pattern.matches("^\\p{Punct}+$", word))) {
      // ignore punctuation and all non valid POS tags
      return null;
    }

    if (!pennTag.equals("UH")) {
      word = word.toLowerCase();
    }

    Double sentimentScore = getWordSentiment(word);
    // use word stemming if sentimentScore is null
    if (sentimentScore == null) {
      // LOG.info(" findStems for (" + word + "," + posTag + ")");
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

  public double[] getTaggedSentenceSentiment(List<TaggedWord> sentence) {
    // [POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, MAX_POS_SCORE,
    // MAX_NEG_SCORE]
    double[] sentenceScore = new double[] { 0, 0, 0, 0, 0, 0, 0.5 };
    for (TaggedWord w : sentence) {
      Double wordSentiment = getWordSentimentWithStemming(w.word(), w.tag());
      if (wordSentiment != null) {
        // POS, NEUTRAL and NEG COUNT
        if (wordSentiment < 0.45) {
          sentenceScore[2]++; // NEGATIV
          if (wordSentiment < sentenceScore[6]) {
            sentenceScore[6] = wordSentiment; // MAX_NEG_SCORE
          }
        } else if (wordSentiment > 0.55) {
          sentenceScore[0]++; // POSITIV
          if (wordSentiment > sentenceScore[5]) {
            sentenceScore[5] = wordSentiment; // MAX_POS_SCORE
          }
        } else if ((wordSentiment <= 0.55) && (wordSentiment >= 0.45)) {
          sentenceScore[1]++; // NEUTRAL
        }
        // SUM
        sentenceScore[3] += wordSentiment;
        // COUNT
        sentenceScore[4]++;
      }
    }

    return (sentenceScore[4] > 0) ? sentenceScore : null;
  }

  public double[] getTweetSentiment(Tweet tweet) {
    // [POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, AVG, MAX_POS_SCORE,
    // MAX_NEG_SCORE]
    double[] tweetScore = new double[] { 0, 0, 0, 0, 0, 0, 0, 0 };
    for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
      LOG.info("taggedSentence: " + sentence.toString());
      double[] sentenceScore = getTaggedSentenceSentiment(sentence);
      if (sentenceScore != null) {
        tweetScore[0] += sentenceScore[0]; // POS_COUNT
        tweetScore[1] += sentenceScore[1]; // NEUTRAL_COUNT
        tweetScore[2] += sentenceScore[2]; // NEG_COUNT
        tweetScore[3] += sentenceScore[3]; // SUM
        tweetScore[4] += sentenceScore[4]; // COUNT
        tweetScore[5] = tweetScore[3] / tweetScore[4]; // AVG
        tweetScore[6] = sentenceScore[5]; // MAX_POS_SCORE
        tweetScore[7] = sentenceScore[6]; // MAX_NEG_SCORE
      }
    }
    return (tweetScore[4] > 0) ? tweetScore : null;
  }

  public static void main(String[] args) {
    String text = "Gas by my monster house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)";

    List<String> tokens = Tokenizer.tokenize(text);

    POSTagger posTagger = POSTagger.getInstance();
    SentimentWordLists sentimentWordLists = SentimentWordLists.getInstance();

    List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);

    System.out.println("text: '" + text + "'");
    double sentimentScore[] = sentimentWordLists
        .getTaggedSentenceSentiment(taggedSentence);
    System.out
        .println("sentimentScore[POS_COUNT, NEUTRAL_COUNT, NEG_COUNT, SUM, COUNT, MAX_POS_SCORE, MAX_NEG_SCORE]"
            + Arrays.toString(sentimentScore));

    sentimentWordLists.close();
  }
}
