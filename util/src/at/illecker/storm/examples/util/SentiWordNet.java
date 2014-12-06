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
package at.illecker.storm.examples.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SentiWordNet {

  public static final String SENTI_WORD_NET_DICT_PATH = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "SentiWordNet_3.0.0_20130122.txt";
  private static final Logger LOG = LoggerFactory.getLogger(SentiWordNet.class);

  private static SentiWordNet instance = new SentiWordNet(); // singleton
  private Map<String, Double> m_dictionary;

  private SentiWordNet() {
    LOG.info("sentiWordNetDictionary: " + SENTI_WORD_NET_DICT_PATH);
    try {
      m_dictionary = loadDict(SENTI_WORD_NET_DICT_PATH);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static SentiWordNet getInstance() {
    return instance;
  }

  public Map<String, Double> loadDict(String pathToSWN) throws IOException {
    // This is our main dictionary representation
    Map<String, Double> dictionary = new HashMap<String, Double>();

    // From String to list of doubles.
    HashMap<String, HashMap<Integer, Double>> tempDictionary = new HashMap<String, HashMap<Integer, Double>>();

    BufferedReader csv = null;
    try {
      csv = new BufferedReader(new FileReader(pathToSWN));
      int lineNumber = 0;

      String line;
      while ((line = csv.readLine()) != null) {
        lineNumber++;

        if (!line.trim().startsWith("#")) {
          String[] data = line.split("\t");
          // Example line
          // POS ID PosScore NegScore SynsetTerms Desc
          // a\t00003131\t0\t0\tadductive#1 adducting#1 adducent#1\tespecially
          // of muscles;

          // Is it a valid line? Otherwise, through exception.
          if (data.length != 6) {
            throw new IllegalArgumentException(
                "Incorrect tabulation format in file, line: " + lineNumber);
          }

          String wordTypeMarker = data[0];

          // Calculate synset score as score = PosS - NegS
          Double synsetScore = Double.parseDouble(data[2])
              - Double.parseDouble(data[3]);

          // Get all Synset terms
          String[] synTermsSplit = data[4].split(" ");
          for (String synTermSplit : synTermsSplit) {
            // Get synterm and synterm rank
            String[] synTermAndRank = synTermSplit.split("#");
            String synTerm = synTermAndRank[0] + "#" + wordTypeMarker;

            int synTermRank = Integer.parseInt(synTermAndRank[1]);
            // What we get here is a map of the type:
            // term -> {score of synset#1, score of synset#2...}

            // Add map to term if it doesn't have one
            if (!tempDictionary.containsKey(synTerm)) {
              tempDictionary.put(synTerm, new HashMap<Integer, Double>());
            }

            // Add synset link to synterm
            tempDictionary.get(synTerm).put(synTermRank, synsetScore);
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

    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (csv != null) {
        csv.close();
      }
    }
    return null;
  }

  public double extract(String word, String pos) {
    return m_dictionary.get(word + "#" + pos);
  }

  public static void main(String[] args) {
    SentiWordNet sentiwordnet = SentiWordNet.getInstance();

    System.out.println("good#a " + sentiwordnet.extract("good", "a"));
    System.out.println("bad#a " + sentiwordnet.extract("bad", "a"));
    System.out.println("blue#a " + sentiwordnet.extract("blue", "a"));
    System.out.println("blue#n " + sentiwordnet.extract("blue", "n"));
  }
}
