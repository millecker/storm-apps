package at.illecker.storm.commons.dict;

import java.util.Set;

import org.apache.storm.guava.collect.Sets;
import org.slf4j.Logger;
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
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.util.io.FileUtils;

public class StopWords {
  // default stop words from nltk.corpus
  public static final String[] STOP_WORDS = new String[] { "i", "me", "my",
      "myself", "we", "our", "ours", "ourselves", "you", "your", "yours",
      "yourself", "yourselves", "he", "him", "his", "himself", "she", "her",
      "hers", "herself", "it", "its", "itself", "they", "them", "their",
      "theirs", "themselves", "what", "which", "who", "whom", "this", "that",
      "these", "those", "am", "is", "are", "was", "were", "be", "been",
      "being", "have", "has", "had", "having", "do", "does", "did", "doing",
      "a", "an", "the", "and", "but", "if", "or", "because", "as", "until",
      "while", "of", "at", "by", "for", "with", "about", "against", "between",
      "into", "through", "during", "before", "after", "above", "below", "to",
      "from", "up", "down", "in", "out", "on", "off", "over", "under", "again",
      "further", "then", "once", "here", "there", "when", "where", "why",
      "how", "all", "any", "both", "each", "few", "more", "most", "other",
      "some", "such", "no", "nor", "not", "only", "own", "same", "so", "than",
      "too", "very", "s", "t", "can", "will", "just", "don", "should", "now" };

  private static final Logger LOG = LoggerFactory.getLogger(NameEntities.class);
  private static final StopWords instance = new StopWords();

  private Set<String> m_stopwords = null;

  private StopWords() {
    m_stopwords = Sets.newHashSet(STOP_WORDS);

    Set<String> files = Configuration.getStopWords();
    if (files != null) {
      for (String file : Configuration.getStopWords()) {
        LOG.info("Load StopWords from: " + file);
        m_stopwords.addAll(FileUtils.readFile(file));
      }
    }
  }

  public static StopWords getInstance() {
    return instance;
  }

  public boolean isStopWord(String value) {
    return m_stopwords.contains(value.toLowerCase());
  }

  public static void main(String[] args) {
    StopWords stopWords = StopWords.getInstance();
    System.out.println("isStopWord(i): " + stopWords.isStopWord("i"));
    System.out.println("isStopWord(q): " + stopWords.isStopWord("q"));
  }
}
