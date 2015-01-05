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
package at.illecker.storm.examples.util.tfidf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import at.illecker.storm.examples.util.wordlist.StopWords;
import at.illecker.storm.examples.util.wordnet.POSTag;
import at.illecker.storm.examples.util.wordnet.WordNet;
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
  private Map<Tweet, Map<String, Double>> m_termFreqs;
  private Map<String, Double> m_inverseDocFreq;
  private boolean m_usePOSTags;

  public TweetTfIdf(List<Tweet> tweets, boolean usePOSTags) {
    this(tweets, TfType.RAW, TfIdfNormalization.NONE, usePOSTags);
  }

  public TweetTfIdf(List<Tweet> tweets, TfType type,
      TfIdfNormalization normalization, boolean usePOSTags) {
    this.m_type = type;
    this.m_normalization = normalization;
    this.m_usePOSTags = usePOSTags;

    this.m_termFreqs = tf(tweets, type, m_usePOSTags);
    this.m_inverseDocFreq = idf(m_termFreqs);

    // Debug
    // print("Term Frequency", m_termFreqs, m_inverseDocFreq);
    // print("Inverse Document Frequency", m_inverseDocFreq);
  }

  public Map<Tweet, Map<String, Double>> getTermFreqs() {
    return m_termFreqs;
  }

  public Map<String, Double> getInverseDocFreq() {
    return m_inverseDocFreq;
  }

  public Map<String, Double> tfIdf(Tweet tweet) {
    return TfIdf.tfIdf(tf(tweet, m_type, m_usePOSTags), m_inverseDocFreq,
        m_normalization);
  }

  public static Map<String, Double> tf(Tweet tweet, TfType type,
      boolean usePOSTags) {
    Map<String, Double> termFreq = new HashMap<String, Double>();

    if (usePOSTags) {
      WordNet wordNet = WordNet.getInstance();
      StopWords stopWords = StopWords.getInstance();
      // regex for one or more punctuations
      Pattern p = Pattern.compile("^\\p{Punct}+$");

      for (List<TaggedWord> sentence : tweet.getTaggedSentences()) {
        List<String> words = new ArrayList<String>();

        for (TaggedWord taggedWord : sentence) {
          String word = taggedWord.word().toLowerCase();
          String pennTag = taggedWord.tag();

          if ((!pennTag.equals(".")) && (!pennTag.equals(","))
              && (!pennTag.equals(":")) && (!pennTag.equals("''"))
              && (!pennTag.equals("(")) && (!pennTag.equals(")"))
              && (!pennTag.equals("URL")) && (!pennTag.equals("HT"))
              && (!pennTag.equals("USR")) && (!pennTag.equals("CC"))
              && (!pennTag.equals("CD")) && (!pennTag.equals("SYM"))
              && (!pennTag.equals("POS")) && (!stopWords.isStopWord(word))) {

            if ((p.matcher(word).find()) && (!pennTag.equals("POS"))) {
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

            words.add(word
                + ((posTag != null) ? "#" + POSTag.toString(posTag) : ""));
          }
        }
        termFreq = TfIdf.tf(termFreq, words);
      }
    } else {
      for (List<String> sentence : tweet.getSentences()) {
        termFreq = TfIdf.tf(termFreq, sentence);
      }
    }
    termFreq = TfIdf.normalizeTf(termFreq, type);
    return termFreq;
  }

  public static Map<Tweet, Map<String, Double>> tf(List<Tweet> tweets,
      TfType type, boolean usePOSTags) {
    Map<Tweet, Map<String, Double>> termFreqs = new HashMap<Tweet, Map<String, Double>>();
    for (Tweet tweet : tweets) {
      termFreqs.put(tweet, tf(tweet, type, usePOSTags));
    }
    return termFreqs;
  }

  public static Map<String, Double> idf(Map<Tweet, Map<String, Double>> termFreq) {
    return TfIdf.idf(termFreq);
  }

  public static Map<Tweet, Map<String, Double>> tfIdf(
      Map<Tweet, Map<String, Double>> termFreqs,
      Map<String, Double> inverseDocFreq, TfIdfNormalization normalization) {

    Map<Tweet, Map<String, Double>> tfIdf = new HashMap<Tweet, Map<String, Double>>();
    // compute tfIdf for each document
    for (Map.Entry<Tweet, Map<String, Double>> doc : termFreqs.entrySet()) {
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
      Map<Tweet, Map<String, Double>> tweetData,
      Map<String, Double> inverseDocFreq) {
    // print title
    LOG.info(String.format("=== %s ===", title));

    // print header
    String line = String.format("%15s", " ");
    for (Map.Entry<Tweet, Map<String, Double>> tweet : tweetData.entrySet()) {
      line += String.format("%8s", "Tweet " + tweet.getKey().getId());
    }
    LOG.info(line);

    // print values
    for (Map.Entry<String, Double> term : inverseDocFreq.entrySet()) {
      line = String.format("%15s", term.getKey());
      for (Map.Entry<Tweet, Map<String, Double>> tweet : tweetData.entrySet()) {
        line += String.format("%8.4f", tweet.getValue().get(term.getKey()));
      }
      LOG.info(line);
    }
  }

  public static void main(String[] args) {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets
        .add(new Tweet(
            1,
            "Excuse the connectivity of this live stream , from Baba Amr , so many activists using only one Sat Modem . LIVE http://t.co/U283IhZ5 #Homs",
            0));
    tweets
        .add(new Tweet(
            2,
            "Show your LOVE for your local field & it might win an award ! Gallagher Park #Bedlington current 4th in National Award http://t.co/WeiMDtQt",
            0));
    tweets
        .add(new Tweet(
            3,
            "@firecore Can you tell me when an update for the Apple TV 3rd gen becomes available ? The missing update holds me back from buying #appletv3",
            0));
    tweets
        .add(new Tweet(
            4,
            "My #cre blog Oklahoma Per Square Foot returns to the @JournalRecord blog hub tomorrow . I will have some interesting local data to share .",
            0));
    tweets
        .add(new Tweet(
            5,
            "\" @bbcburnsy : Loads from SB ; talks with Chester continue ; no deals 4 out of contract players ' til Jan ; Dev t Roth , Coops to Chest'ld #hcafc \"",
            0));

    Preprocessor preprocessor = Preprocessor.getInstance();
    for (Tweet tweet : tweets) {
      // Tokenize
      List<String> tokens = Tokenizer.tokenize(tweet.getText());

      // Preprocess
      tweet.addSentence(preprocessor.preprocess(tokens));
    }

    boolean usePOSTags = true;
    // POS Tagging
    if (usePOSTags) {
      POSTagger posTagger = POSTagger.getInstance();
      for (Tweet tweet : tweets) {
        for (List<String> sentence : tweet.getSentences()) {
          List<TaggedWord> taggedSentence = posTagger.tagSentence(sentence);
          tweet.addTaggedSentence(taggedSentence);
        }
      }
    }

    Map<Tweet, Map<String, Double>> termFreqs = TweetTfIdf.tf(tweets,
        TfType.RAW, usePOSTags);
    Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);

    Map<Tweet, Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
        inverseDocFreq, TfIdfNormalization.NONE);

    print("Term Frequency", termFreqs, inverseDocFreq);
    print("Inverse Document Frequency", inverseDocFreq);
    print("tf-idf", tfIdf, inverseDocFreq);
  }
}
