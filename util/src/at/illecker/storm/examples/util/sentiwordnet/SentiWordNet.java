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
package at.illecker.storm.examples.util.sentiwordnet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.wordnet.POSTag;
import at.illecker.storm.examples.util.wordnet.WordNet;
import edu.mit.jwi.item.POS;

public class SentiWordNet {

  public static final String SENTI_WORD_NET_DICT_PATH = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "SentiWordNet_3.0.0_20130122.txt";
  private static final Logger LOG = LoggerFactory.getLogger(SentiWordNet.class);

  private static SentiWordNet instance = new SentiWordNet(); // singleton
  private static WordNet m_wordnet;
  private Map<String, Double> m_dictionary;

  private SentiWordNet() {
    m_wordnet = WordNet.getInstance();

    LOG.info("loadDictionary: " + SENTI_WORD_NET_DICT_PATH);
    try {
      m_dictionary = loadDict(SENTI_WORD_NET_DICT_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static SentiWordNet getInstance() {
    return instance;
  }

  private Map<String, Double> loadDict(String sentiWordNetDictPath)
      throws IOException {
    // This is our main dictionary representation
    Map<String, Double> dictionary = new HashMap<String, Double>();

    // From String to list of doubles.
    HashMap<String, HashMap<Integer, Double>> tempDictionary = new HashMap<String, HashMap<Integer, Double>>();

    BufferedReader br = null;
    try {
      br = new BufferedReader(new FileReader(sentiWordNetDictPath));

      int lineNumber = 0;
      String line;
      // POS ID PosScore NegScore SynsetTerms Desc
      // a \t 00003131 \t 0 \t 0 \t adductive#1 adducting#1 adducent#1 \t desc
      while ((line = br.readLine()) != null) {
        line = line.trim();
        lineNumber++;

        if ((line.length() > 0) && (line.charAt(0) != '#')) {
          String[] data = line.split("\t");
          // check tab format
          if (data.length != 6) {
            LOG.error("Incorrect tabulation format in file "
                + sentiWordNetDictPath + ", line: " + lineNumber);
          }

          POS posTag = POSTag.parseString(data[0]);
          double posScore = Double.parseDouble(data[2]);
          double negScore = Double.parseDouble(data[3]);
          Double synsetScore = posScore - negScore;
          String synonyms = data[4];

          if (synsetScore != 0.0) {
            for (String synonymToken : synonyms.split(" ")) {
              // word#position
              String[] synonym = synonymToken.split("#");
              String word = synonym[0];
              int position = Integer.parseInt(synonym[1]);

              String synTerm = word + "#" + posTag.getTag();

              // Add map to term if it doesn't have one
              if (!tempDictionary.containsKey(synTerm)) {
                tempDictionary.put(synTerm, new HashMap<Integer, Double>());
              }

              // Add map to term if it doesn't have one
              if (!tempDictionary.containsKey(synTerm)) {
                tempDictionary.put(synTerm, new HashMap<Integer, Double>());
              }

              // Add synset link to synterm
              tempDictionary.get(synTerm).put(position, synsetScore);

              List<String> stemmedWords = m_wordnet.findStems(word, posTag);
              for (String stemmedWord : stemmedWords) {
                // TODO
              }
            }
          }
        }
      }

      // Go through all the terms.
      for (Map.Entry<String, HashMap<Integer, Double>> entry : tempDictionary
          .entrySet()) {

        String word = entry.getKey();
        Map<Integer, Double> synSetScoreMap = entry.getValue();

        // Calculate weighted average. Weight the synsets according to
        // their rank.
        // Score= 1/2*first + 1/3*second + 1/4*third ..... etc.
        // Sum = 1/1 + 1/2 + 1/3 ...
        double score = 0.0;
        double sum = 0.0;
        for (Map.Entry<Integer, Double> setScore : synSetScoreMap.entrySet()) {
          score += setScore.getValue() / (double) setScore.getKey();
          sum += 1.0 / (double) setScore.getKey();
        }
        score /= sum;

        dictionary.put(word, score);
      }

      return dictionary;

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        br.close();
      }
    }

    return null;
  }

  public void close() {
    try {
      m_wordnet.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public Double getScore(String word, String posTag) {
    return m_dictionary.get(word + "#" + posTag);
  }

  public static void main(String[] args) {
    SentiWordNet sentiwordnet = SentiWordNet.getInstance();

    System.out.println("good#a " + sentiwordnet.getScore("good", "a"));
    System.out.println("bad#a " + sentiwordnet.getScore("bad", "a"));
    System.out.println("blue#a " + sentiwordnet.getScore("blue", "a"));
    System.out.println("blue#n " + sentiwordnet.getScore("blue", "n"));

    sentiwordnet.close();
  }
}
