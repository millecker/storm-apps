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
package at.illecker.storm.examples.postagger;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Sentence;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * POS Tagger is based on the Stanford NLP library and the gate-EN-twitter.model
 *
 * http://nlp.stanford.edu/software/corenlp.shtml
 * http://nlp.stanford.edu:8080/corenlp/process
 * https://gate.ac.uk/wiki/twitter-postagger.html
 * 
 */
public class POSTagger {

  private static final String TAGGER_MODEL1 = "lib/tagger_models/gate-EN-twitter.model";
  private static final String TAGGER_MODEL2 = "lib/tagger_models/gate-EN-twitter-fast.model";
  private static final String TWEET_TEXT = "ikr smh he asked fir yo last name so he can add u on fb lololol";
  private static final int TEST_ROUNDS = 1;

  public static void main(String[] args) {
    // Measurements
    List<Long> tagger1TextTimes = new ArrayList<Long>();
    List<Long> tagger2TextTimes = new ArrayList<Long>();
    List<Long> tagger1SentenceTimes = new ArrayList<Long>();
    List<Long> tagger2SentenceTimes = new ArrayList<Long>();

    // Load tagger and models
    MaxentTagger tagger1 = new MaxentTagger(TAGGER_MODEL1);
    MaxentTagger tagger2 = new MaxentTagger(TAGGER_MODEL2);

    // Test tagger1 via strings
    String taggedText1 = "";
    for (int i = 0; i < TEST_ROUNDS; i++) {
      long startTime = System.currentTimeMillis();
      taggedText1 = tagger1.tagString(TWEET_TEXT);
      tagger1TextTimes.add(System.currentTimeMillis() - startTime);
    }

    // Test tagger2 via strings
    String taggedText2 = "";
    for (int i = 0; i < TEST_ROUNDS; i++) {
      long startTime = System.currentTimeMillis();
      taggedText2 = tagger2.tagString(TWEET_TEXT);
      tagger2TextTimes.add(System.currentTimeMillis() - startTime);
    }

    // Manually tokenize
    List<HasWord> sentence = Sentence.toWordList(TWEET_TEXT.split(" "));

    // Test tagger1 via sentence
    List<TaggedWord> taggedSentence1 = null;
    for (int i = 0; i < TEST_ROUNDS; i++) {
      long startTime = System.currentTimeMillis();
      taggedSentence1 = tagger1.tagSentence(sentence);
      tagger1SentenceTimes.add(System.currentTimeMillis() - startTime);
    }
    // Test tagger2 via sentence
    List<TaggedWord> taggedSentence2 = null;
    for (int i = 0; i < TEST_ROUNDS; i++) {
      long startTime = System.currentTimeMillis();
      taggedSentence2 = tagger2.tagSentence(sentence);
      tagger2SentenceTimes.add(System.currentTimeMillis() - startTime);
    }

    // Output results
    System.out.println("Input: " + TWEET_TEXT);
    System.out.println("Tagger1 Text output: " + taggedText1);
    System.out.println("Tagger2 Text output: " + taggedText2);
    System.out.println("Tagger1 Sentence output: ");
    for (TaggedWord tw : taggedSentence1) {
      System.out.println("word: " + tw.word() + " ::  tag: " + tw.tag());
      // tw.tag().startsWith("JJ")
    }
    System.out.println("Tagger2 Sentence output: ");
    for (TaggedWord tw : taggedSentence2) {
      System.out.println("word: " + tw.word() + " ::  tag: " + tw.tag());
    }

    System.out.println("Time Measurements: ");
    System.out
        .println("Tagger1 (gate-EN-twitter.model): TextTagging (incl tokenize): "
            + getAvg(tagger1TextTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2TextTimes) + " ms");
    System.out
        .println("Tagger2 (gate-EN-twitter-fast.model): TextTagging (incl tokenize): "
            + getAvg(tagger1SentenceTimes)
            + " ms SentenceTagging: "
            + getAvg(tagger2SentenceTimes) + " ms");
  }

  private static long getAvg(List<Long> list) {
    long sum = 0;
    for (Long l : list) {
      sum += l.longValue();
    }
    return (sum / list.size());
  }
}
