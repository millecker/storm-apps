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

import libsvm.svm_parameter;
import at.illecker.storm.examples.util.svm.SVM;

public class Configuration {

  public static final boolean LOGGING = false;

  public static final String MODELS_PATH = "resources" + File.separator
      + "models";

  public static final String POS_MODEL_PATH = MODELS_PATH + File.separator
      + "pos";

  public static final String SVM_MODEL_PATH = MODELS_PATH + File.separator
      + "svm";

  public static final String DICTIONARIES_PATH = "resources" + File.separator
      + "dictionaries";

  public static final String SENTIMENT_DICTIONARIES_PATH = DICTIONARIES_PATH
      + File.separator + "sentiment";

  public static final String SLANG_DICTIONARIES_PATH = DICTIONARIES_PATH
      + File.separator + "slang";

  public static final String WORD_NET_PATH = DICTIONARIES_PATH + File.separator
      + "wordnet";

  public static final String DATASET_PATH = "resources" + File.separator
      + "datasets";

  public static final String USER_DIR_PATH = ((System.getProperty("user.dir") != null) ? System
      .getProperty("user.dir") : "");

  public static final String TEMP_DIR_PATH = System
      .getProperty("java.io.tmpdir");

  public static String getSentiWordNetDict() {
    return USER_DIR_PATH + File.separator + SENTIMENT_DICTIONARIES_PATH
        + File.separator + "SentiWordNet_3.0.0_20130122.txt";
  }

  public static Map<String, Properties> getSentimentWordlists() {
    String wordListDir = USER_DIR_PATH + File.separator
        + SENTIMENT_DICTIONARIES_PATH + File.separator;
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
    String wordListDir = USER_DIR_PATH + File.separator
        + SLANG_DICTIONARIES_PATH + File.separator;
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
    String dictDir = USER_DIR_PATH + File.separator + DICTIONARIES_PATH
        + File.separator;
    Set<String> nameEntities = new HashSet<String>();

    // Add GATE cities
    nameEntities.add(dictDir + "GATE_cities.txt");

    // Add GATE corps
    nameEntities.add(dictDir + "GATE_corps.txt");

    // Add GATE names
    nameEntities.add(dictDir + "GATE_names.txt");

    return nameEntities;
  }

  public static Set<String> getFirstNames() {
    String dictDir = USER_DIR_PATH + File.separator + DICTIONARIES_PATH
        + File.separator;
    Set<String> firstNames = new HashSet<String>();

    // Oxford Reference
    // A Dictionary of First Names (2 ed.)
    // http://www.oxfordreference.com/view/10.1093/acref/9780198610601.001.0001/acref-9780198610601?hide=true&pageSize=100&sort=titlesort
    firstNames.add(dictDir + "FirstNames.txt");

    return firstNames;
  }

  public static Set<String> getStopWords() {
    String dictDir = USER_DIR_PATH + File.separator + DICTIONARIES_PATH
        + File.separator;

    Set<String> stopWords = new HashSet<String>();
    stopWords.add(dictDir + "Stopwords.txt");

    return stopWords;
  }

  public static Map<String, Properties> getInterjections() {
    String dictDir = USER_DIR_PATH + File.separator + DICTIONARIES_PATH
        + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add GATE interjections including regex patterns
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(dictDir + "GATE_interjections.regex", props);

    return interjections;
  }

  public static Map<String, Properties> getEmoticons() {
    String dictDir = USER_DIR_PATH + File.separator + DICTIONARIES_PATH
        + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add emoticons including regex patterns
    // from http://de.wiktionary.org/wiki/Verzeichnis:International/Smileys
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(dictDir + "Emoticons.regex", props);

    return interjections;
  }

  public static String getPOSTaggingModel() {
    return USER_DIR_PATH + File.separator + POS_MODEL_PATH + File.separator
        + "gate-EN-twitter.model";
  }

  public static String getPOSTaggingModelFast() {
    return USER_DIR_PATH + File.separator + POS_MODEL_PATH + File.separator
        + "gate-EN-twitter-fast.model";
  }

  public static String getWordNetDict() {
    return USER_DIR_PATH + File.separator + WORD_NET_PATH + File.separator
        + "wn3.1.dict.tar.gz";
  }

  public static String getDataSetPath() {
    return USER_DIR_PATH + File.separator + DATASET_PATH;
  }

  public static Dataset getDataSetSemEval2013Mixed() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;
    // svmParam.C = Math.pow(2, 6);
    // svmParam.gamma = Math.pow(2, -5);

    return new Dataset(Configuration.getDataSetPath() + File.separator
        + "SemEval2013_mixed", "trainingInput.txt", null, "testingInput.txt",
        "\t", 0, 3, 1, new String[] { "negative" }, new String[] { "neutral" },
        new String[] { "positive" }, 0, 1, 2, svmParam);
  }

  public static Dataset getDataSetSemEval2013() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;
    // TODO
    return new Dataset(Configuration.getDataSetPath() + File.separator
        + "SemEval2013", "twitter-train-full-B.tsv", "twitter-dev-gold-B.tsv",
        "twitter-test-gold-B.tsv", "\t", 0, 2, 3, new String[] { "negative",
            "\"negative\"" }, new String[] { "neutral", "objective-OR-neutral",
            "objective" }, new String[] { "positive" }, 0, 1, 2, svmParam);
  }

  public static Dataset getDataSetSemEval2014() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;
    // TODO
    return new Dataset(Configuration.getDataSetPath() + File.separator
        + "SemEval2014", "twitter-train-gold-B-2014.tsv", null,
        "SemEval2014-task9-test-B-input.txt", "\t", 0, 2, 3, new String[] {
            "negative", "\"negative\"" }, new String[] { "neutral",
            "\"neutral\"", "\"objective-OR-neutral\"" }, new String[] {
            "positive", "\"positive\"" }, 0, 1, 2, svmParam);
  }
}
