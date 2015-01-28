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
package at.illecker.storm.commons;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import libsvm.svm_parameter;
import twitter4j.Status;
import at.illecker.storm.commons.svm.SVM;
import at.illecker.storm.commons.util.io.JsonUtils;

public class Configuration {

  public static final boolean LOGGING = false;

  public static final boolean RUNNING_WITHIN_JAR = Configuration.class
      .getResource("Configuration.class").toString().startsWith("jar:");

  public static final String WORKING_DIR_PATH = (RUNNING_WITHIN_JAR) ? ""
      : System.getProperty("user.dir") + File.separator;

  public static final String TEMP_DIR_PATH = System
      .getProperty("java.io.tmpdir");

  public static final String POS_MODEL_PATH = WORKING_DIR_PATH + "resources"
      + File.separator + "models" + File.separator + "pos";

  public static final String SVM_MODEL_PATH = WORKING_DIR_PATH + "resources"
      + File.separator + "models" + File.separator + "svm";

  public static final String DICTIONARIES_PATH = WORKING_DIR_PATH + "resources"
      + File.separator + "dictionaries";

  public static final String SENTIMENT_DICTIONARIES_PATH = DICTIONARIES_PATH
      + File.separator + "sentiment";

  public static final String SLANG_DICTIONARIES_PATH = DICTIONARIES_PATH
      + File.separator + "slang";

  public static final String WORD_NET_PATH = DICTIONARIES_PATH + File.separator
      + "wordnet";

  public static final String DATASET_PATH = WORKING_DIR_PATH + "resources"
      + File.separator + "datasets";

  public static String getSentiWordNetDict() {
    return SENTIMENT_DICTIONARIES_PATH + File.separator
        + "SentiWordNet_3.0.0_20130122.txt";
  }

  public static Map<String, Properties> getSentimentWordlists() {
    String wordListDir = SENTIMENT_DICTIONARIES_PATH + File.separator;
    Map<String, Properties> wordLists = new HashMap<String, Properties>();
    Properties props = null;

    // Add AFINN word list using Regex=false, containsPOSTags=false
    // AFINN lexicon (minValue -5 and maxValue +5)
    // http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-5));
    props.put("maxValue", Double.valueOf(5));
    wordLists.put(wordListDir + "AFINN-111.txt", props);

    // Add SentiStrength EmotionLookupTable using Regex=true
    // SentiStrength lexicon (minValue -5 and maxValue +5)
    // http://sentistrength.wlv.ac.uk/#Download
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(true));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-5));
    props.put("maxValue", Double.valueOf(5));
    wordLists.put(wordListDir
        + "SentiStrength_Data_Sept2011_EmotionLookupTable.txt", props);

    // Add SentiStrength EmoticonLookupTable using Regex=false
    // SentiStrength emoticons lexicon (minValue -1 and maxValue +1)
    // http://sentistrength.wlv.ac.uk/#Download
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-1));
    props.put("maxValue", Double.valueOf(1));
    wordLists.put(wordListDir
        + "SentiStrength_Data_Sept2011_EmoticonLookupTable.txt", props);

    // Add SentiWords using Regex=false, containsPOSTags=true
    // SentiWords lexicon (minValue -1 and maxValue +1)
    // Real minValue -0.935 and maxValue 0.88257
    // Words might contain an underscore -> blank
    // https://hlt.fbk.eu/technologies/sentiwords
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(true));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-0.935));
    props.put("maxValue", Double.valueOf(0.88257));
    wordLists.put(wordListDir + "SentiWords_1.0.txt", props);

    // Add Sentiment140 Lexicon using Regex=false, containsPOSTags=false
    // Sentiment140 lexicon (minValue -5 and maxValue +5)
    // Real minValue -4.999 and maxValue 5
    // http://www.saifmohammad.com/WebPages/lexicons.html
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-4.999));
    props.put("maxValue", Double.valueOf(5));
    wordLists.put(wordListDir + "Sentiment140_unigrams_pmilexicon.txt", props);

    // Add Bing Liu Lexicon using Regex=false, containsPOSTags=false
    // Bing Liu lexicon (minValue -1 and maxValue 1)
    // http://www.cs.uic.edu/~liub/FBS/sentiment-analysis.html#lexicon
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-1));
    props.put("maxValue", Double.valueOf(1));
    wordLists.put(wordListDir + "Bing_Liu.txt", props);

    // Add MPQA Subjectivity Lexicon using Regex=false, containsPOSTags=false
    // MPQA Subjectivity Lexicon (minValue -1 and maxValue 1)
    // http://mpqa.cs.pitt.edu/lexicons/subj_lexicon/
    props = new Properties();
    props.put("separator", "\t");
    props.put("containsPOSTags", Boolean.valueOf(false));
    props.put("containsRegex", Boolean.valueOf(false));
    props.put("featureScaling", Boolean.valueOf(true));
    props.put("minValue", Double.valueOf(-1));
    props.put("maxValue", Double.valueOf(1));
    wordLists.put(wordListDir + "MPQA_subjclueslen1-HLTEMNLP05.txt", props);

    return wordLists;
  }

  public static Map<String, Properties> getSlangWordlists() {
    String wordListDir = SLANG_DICTIONARIES_PATH + File.separator;
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

    // Add SentiStrength SlangLookupTable
    props = new Properties();
    props.put("separator", "\t");
    slangWordLists.put(wordListDir
        + "SentiStrength_Data_Sept2011_SlangLookupTable.txt", props);

    return slangWordLists;
  }

  public static Set<String> getNameEntities() {
    String dictDir = DICTIONARIES_PATH + File.separator;
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
    String dictDir = DICTIONARIES_PATH + File.separator;
    Set<String> firstNames = new HashSet<String>();

    // Oxford Reference
    // A Dictionary of First Names (2 ed.)
    // http://www.oxfordreference.com/view/10.1093/acref/9780198610601.001.0001/acref-9780198610601?hide=true&pageSize=100&sort=titlesort
    firstNames.add(dictDir + "FirstNames.txt");

    return firstNames;
  }

  public static Set<String> getStopWords() {
    String dictDir = DICTIONARIES_PATH + File.separator;

    Set<String> stopWords = new HashSet<String>();
    stopWords.add(dictDir + "Stopwords.txt");

    return stopWords;
  }

  public static Map<String, Properties> getInterjections() {
    String dictDir = DICTIONARIES_PATH + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add GATE interjections including regex patterns
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(dictDir + "GATE_interjections.regex", props);

    return interjections;
  }

  public static Map<String, Properties> getEmoticons() {
    String dictDir = DICTIONARIES_PATH + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add emoticons including regex patterns
    // from http://de.wiktionary.org/wiki/Verzeichnis:International/Smileys
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(dictDir + "Emoticons.regex", props);

    return interjections;
  }

  public static String getPOSTaggingModel() {
    return POS_MODEL_PATH + File.separator + "gate-EN-twitter.model";
  }

  public static String getPOSTaggingModelFast() {
    return POS_MODEL_PATH + File.separator + "gate-EN-twitter-fast.model";
  }

  public static String getWordNetDict() {
    return WORD_NET_PATH + File.separator + "wn3.1.dict.tar.gz";
  }

  public static List<Status> getDataSetUibkCrawler(String filterLanguage) {
    String tweetsDir = DATASET_PATH + File.separator + "uibk_crawler";
    return JsonUtils.readTweetsDirectory(tweetsDir, filterLanguage);
  }

  public static List<Status> getDataSetUibkCrawlerTest(String filterLanguage) {
    String tweetsDir = DATASET_PATH + File.separator + "uibk_crawler"
        + File.separator + "test";
    return JsonUtils.readTweetsDirectory(tweetsDir, filterLanguage);
  }

  public static Dataset getDataSetSemEval2013Mixed() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;
    // svmParam.C = Math.pow(2, 6);
    // svmParam.gamma = Math.pow(2, -5);

    return new Dataset(DATASET_PATH + File.separator + "SemEval2013_mixed",
        "trainingInput.txt", null, "testingInput.txt", "\t", 0, 3, 1,
        new String[] { "positive" }, new String[] { "negative" },
        new String[] { "neutral" }, 0, 1, 2, svmParam);
  }

  public static Dataset getDataSetSemEval2013() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;

    svmParam.nr_weight = 3;
    svmParam.weight_label = new int[svmParam.nr_weight];
    svmParam.weight_label[0] = 0;
    svmParam.weight_label[1] = 1;
    svmParam.weight_label[2] = 2;
    svmParam.weight = new double[svmParam.nr_weight];
    // class weights for train + dev
    svmParam.weight[0] = 1.26;
    svmParam.weight[1] = 2.95;
    svmParam.weight[2] = 1;

    return new Dataset(DATASET_PATH + File.separator + "SemEval2013",
        "twitter-train-full-B.tsv", "twitter-dev-gold-B.tsv",
        "twitter-test-gold-B.tsv", "\t", 0, 2, 3, new String[] { "positive" },
        new String[] { "negative", "\"negative\"" }, new String[] { "neutral",
            "objective-OR-neutral", "objective" }, 0, 1, 2, svmParam);
  }

  public static Dataset getDataSetSemEval2014() {
    svm_parameter svmParam = SVM.getDefaultParameter();
    // After Grid search use best C and gamma values
    svmParam.kernel_type = svm_parameter.LINEAR;
    svmParam.C = 0.5;
    // TODO
    return new Dataset(DATASET_PATH + File.separator + "SemEval2014",
        "twitter-train-gold-B-2014.tsv", null,
        "SemEval2014-task9-test-B-input.txt", "\t", 0, 2, 3, new String[] {
            "positive", "\"positive\"" }, new String[] { "negative",
            "\"negative\"" }, new String[] { "neutral", "\"neutral\"",
            "\"objective-OR-neutral\"" }, 0, 1, 2, svmParam);
  }

}