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

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Configuration {

  public static final boolean LOGGING = false;

  public static final String MODEL_PATH = "resources" + File.separator
      + "models";

  public static final String WORD_LIST_PATH = "resources" + File.separator
      + "wordlists";

  public static final String WORD_NET_PATH = "resources" + File.separator
      + "wordnet";

  public static final String DATASET_PATH = "resources" + File.separator
      + "datasets";

  public static final String USER_DIR_PATH = ((System.getProperty("user.dir") != null) ? System
      .getProperty("user.dir") : "");

  public static final String TEMP_DIR_PATH = System
      .getProperty("java.io.tmpdir");

  public static Map<String, Properties> getWordlists() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> wordLists = new HashMap<String, Properties>();

    // Add AFINN word list using Regex = false
    // AFINN word list (minValue -5 and maxValue +5)
    Properties props = new Properties();
    props.put("separator", "\t");
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-5));
    props.put("maxValue", Double.valueOf(5));
    wordLists.put(wordListDir + "AFINN-111.txt", props);

    // Add SentStrength EmoticonLookupTable using Regex = false
    // SentStrength emoticons (minValue -1 and maxValue +1)
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-1));
    props.put("maxValue", Double.valueOf(1));

    wordLists.put(wordListDir
        + "SentStrength_Data_Sept2011_EmoticonLookupTable.txt", props);

    // Add SentStrength EmotionLookupTable using Regex = true
    // SentStrength word list (minValue -5 and maxValue +5)
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsRegex", Boolean.valueOf(true));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-5));
    props.put("maxValue", Double.valueOf(5));
    wordLists.put(wordListDir
        + "SentStrength_Data_Sept2011_EmotionLookupTable.txt", props);

    return wordLists;
  }

  public static Map<String, Properties> getSlangWordlists() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> slangWordLists = new HashMap<String, Properties>();
    Properties props;

    // Add InternetSlang.txt from http://www.internetslang.com/
    props = new Properties();
    props.put("separator", "\t");
    slangWordLists.put(wordListDir + "InternetSlang.txt", props);

    // Add NoSlang.txt from http://www.noslang.com/dictionary/
    props = new Properties();
    props.put("separator", "\t");
    slangWordLists.put(wordListDir + "NoSlang.txt", props);

    // Add GATE SlangLookupTable orth.en
    props = new Properties();
    props.put("separator", ",");
    slangWordLists.put(wordListDir + "GATE_slang.en.csv", props);

    // Add SentStrength SlangLookupTable
    props = new Properties();
    props.put("separator", "\t");
    slangWordLists.put(wordListDir
        + "SentStrength_Data_Sept2011_SlangLookupTable.txt", props);

    return slangWordLists;
  }

  public static Set<String> getNameEntities() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;
    Set<String> nameEntities = new HashSet<String>();

    // Add GATE cities
    nameEntities.add(wordListDir + "GATE_cities.txt");

    // Add GATE corps
    nameEntities.add(wordListDir + "GATE_corps.txt");

    // Add GATE names
    nameEntities.add(wordListDir + "GATE_names.txt");

    return nameEntities;
  }

  public static Set<String> getStopWords() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;

    Set<String> stopWords = new HashSet<String>();
    stopWords.add(wordListDir + "Stopwords.txt");

    return stopWords;
  }

  public static Map<String, Properties> getInterjections() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add GATE interjections including regex patterns
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(wordListDir + "GATE_interjections.regex", props);

    return interjections;
  }

  public static Map<String, Properties> getEmoticons() {
    String wordListDir = USER_DIR_PATH + File.separator + WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add emoticons including regex patterns
    // from http://de.wiktionary.org/wiki/Verzeichnis:International/Smileys
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(wordListDir + "Emoticons.regex", props);

    return interjections;
  }

  public static String getPOSTaggingModel() {
    return USER_DIR_PATH + File.separator + MODEL_PATH + File.separator
        + "gate-EN-twitter-fast.model";
  }

  public static String getWordNetDict() {
    return USER_DIR_PATH + File.separator + WORD_NET_PATH + File.separator
        + "wn3.1.dict.tar.gz";
  }

  public static String getSentiWordNetDict() {
    return USER_DIR_PATH + File.separator + WORD_NET_PATH + File.separator
        + "SentiWordNet_3.0.0_20130122.txt";
  }

  public static String getDataSetPath() {
    return USER_DIR_PATH + File.separator + DATASET_PATH;
  }
}
