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
package at.illecker.storm.commons.sentiwordnet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.util.io.IOUtils;
import at.illecker.storm.commons.wordnet.POSTag;
import at.illecker.storm.commons.wordnet.WordNet;
import edu.mit.jwi.item.POS;

public class SentiWordNet {
  private static final Logger LOG = LoggerFactory.getLogger(SentiWordNet.class);
  private static final SentiWordNet INSTANCE = new SentiWordNet();

  private WordNet m_wordnet;
  private Map<String, HashMap<Integer, SentiValue>> m_dict;
  private Map<String, Double> m_dictWeighted;

  private SentiWordNet() {
    m_wordnet = WordNet.getInstance();

    m_dict = loadSentiWordNetDict();
    m_dictWeighted = calcAvgWeightScores();
  }

  public static SentiWordNet getInstance() {
    return INSTANCE;
  }

  private Map<String, HashMap<Integer, SentiValue>> loadSentiWordNetDict() {
    String sentiWordNetDict = Configuration.getSentiWordNetDict();
    LOG.info("loadDictionary: " + sentiWordNetDict);
    InputStream in = IOUtils.getInputStream(sentiWordNetDict);

    Map<String, HashMap<Integer, SentiValue>> dict = new HashMap<String, HashMap<Integer, SentiValue>>();
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(in));
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
            LOG.error("Incorrect tabulation format, line: " + lineNumber);
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

              if (!dict.containsKey(synTerm)) {
                dict.put(synTerm, new HashMap<Integer, SentiValue>());
              }

              dict.get(synTerm).put(position,
                  new SentiValue(posScore, negScore));

              // TODO
              /*
               * List<String> stemmedWords = m_wordnet.findStems(word, posTag);
               * for (String stemmedWord : stemmedWords) { }
               */
            }
          }
        }
      }

      return dict;

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return null;
  }

  public Map<String, Double> calcAvgWeightScores() {
    Map<String, Double> dictWeighted = new HashMap<String, Double>();

    for (Map.Entry<String, HashMap<Integer, SentiValue>> entry : m_dict
        .entrySet()) {
      String synTerm = entry.getKey();
      Map<Integer, SentiValue> synSetScoreMap = entry.getValue();

      // Calculate weighted average
      // Weight the synsets according to their rank
      // Score = 1/2*first + 1/3*second + 1/4*third ...
      // Sum = 1/1 + 1/2 + 1/3 ...
      double score = 0.0;
      double sum = 0.0;
      for (Map.Entry<Integer, SentiValue> synset : synSetScoreMap.entrySet()) {
        score += synset.getValue().getScore() / (double) synset.getKey();
        sum += 1.0 / (double) synset.getKey();
      }
      score /= sum;

      dictWeighted.put(synTerm, score);
    }
    return dictWeighted;
  }

  public void close() {
    m_wordnet.close();
  }

  public Map<Integer, SentiValue> getSentiValues(String word, char posTag) {
    return m_dict.get(word + "#" + posTag);
  }

  public Map<Integer, SentiValue> getSentiValues(String word, POS posTag) {
    return getSentiValues(word, posTag.getTag());
  }

  public SentiValue getSentiValue(String word, char posTag, int position) {
    HashMap<Integer, SentiValue> values = m_dict.get(word + "#" + posTag);
    if ((values != null) && (values.size() > position)) {
      return values.get(position);
    }
    return null;
  }

  public SentiValue getSentiValue(String word, POS posTag, int position) {
    return getSentiValue(word, posTag.getTag(), position);
  }

  public SentiValue getSentiValue(String word, char posTag) {
    return getSentiValue(word, posTag, 1);
  }

  public SentiValue getSentiValue(String word, POS posTag) {
    return getSentiValue(word, posTag.getTag(), 1);
  }

  public Double getScore(String word, char posTag) {
    SentiValue sentiValue = getSentiValue(word, posTag, 1);
    if (sentiValue != null) {
      return sentiValue.getScore();
    }
    return null;
  }

  public Double getScore(String word, POS posTag) {
    return getScore(word, posTag.getTag());
  }

  public Double getAvgScore(String word, char posTag) {
    return m_dictWeighted.get(word + "#" + posTag);
  }

  public Double getAvgScore(String word, POS posTag) {
    return getAvgScore(word, posTag.getTag());
  }

  public static void printScores(SentiWordNet swn, String word, char posTag) {
    System.out.println(word + "#" + posTag + ": " + swn.getScore(word, posTag));
    System.out.println("avg(" + word + "#" + posTag + ") "
        + swn.getAvgScore(word, posTag));
    Map<Integer, SentiValue> values = swn.getSentiValues(word, posTag);
    if (values != null) {
      for (Map.Entry<Integer, SentiValue> val : values.entrySet()) {
        System.out.println("\t" + val.getKey() + "\t" + val.getValue());
      }
    }
  }

  public static void main(String[] args) {
    SentiWordNet swn = SentiWordNet.getInstance();

    printScores(swn, "good", 'a');
    printScores(swn, "bad", 'a');
    printScores(swn, "blue", 'a');
    printScores(swn, "blue", 'n');

    swn.close();
  }
}
