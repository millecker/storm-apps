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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

  private static final Logger LOG = LoggerFactory
      .getLogger(Configuration.class);
  private static final Configuration instance = new Configuration();

  private static final String CONF_MODEL_PATH = "resources" + File.separator
      + "models";
  private static final String CONF_WORD_LIST_PATH = "resources"
      + File.separator + "wordlists";
  private static final String CONF_WORD_NET_PATH = "resources" + File.separator
      + "wordnet";

  public static boolean CONF_LOGGING = false;

  private String m_userDir;
  private String m_workingDir;
  private String m_classPath;
  private boolean m_runningFromJar;

  private Configuration() {
    m_userDir = System.getProperty("user.dir");

    m_classPath = Configuration.class.getResource("Configuration.class")
        .toString();
    if (m_classPath.startsWith("jar:")) {
      LOG.info("Running within a jar file...");
      m_runningFromJar = true;
    } else {
      LOG.info("Running outside of a jar file...");
      m_runningFromJar = false;
      m_workingDir = m_userDir;
    }
  }

  public static Configuration getInstance() {
    return instance;
  }

  public Map<String, Properties> getWordlists() {
    String wordListDir = m_workingDir + File.separator + CONF_WORD_LIST_PATH
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

  public Map<String, Properties> getSlangWordlists() {
    String wordListDir = m_workingDir + File.separator + CONF_WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> slangWordLists = new HashMap<String, Properties>();

    // Add SentStrength SlangLookupTable
    Properties props = new Properties();
    props.put("separator", "\t");
    slangWordLists.put(wordListDir
        + "SentStrength_Data_Sept2011_SlangLookupTable.txt", props);

    // Add GATE SlangLookupTable orth.en
    props = new Properties();
    props.put("separator", ",");
    slangWordLists.put(wordListDir + "GATE_slang.en.csv", props);

    return slangWordLists;
  }

  public Set<String> getNameEntities() {
    String wordListDir = m_workingDir + File.separator + CONF_WORD_LIST_PATH
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

  public Map<String, Properties> getInterjections() {
    String wordListDir = m_workingDir + File.separator + CONF_WORD_LIST_PATH
        + File.separator;
    Map<String, Properties> interjections = new HashMap<String, Properties>();

    // Add GATE interjections including regex patterns
    Properties props = new Properties();
    props.put("containsRegex", Boolean.valueOf(true));
    interjections.put(wordListDir + "GATE_interjections.regex", props);

    return interjections;
  }

  public String getPOSTaggingModel() {
    String modelDir = m_workingDir + File.separator + CONF_MODEL_PATH
        + File.separator;
    return modelDir + "gate-EN-twitter-fast.model";
  }

  public String getWordNetDict() {
    String wordNetDir = m_workingDir + File.separator + CONF_WORD_NET_PATH
        + File.separator;
    return wordNetDir + "wn3.1.dict.tar.gz";
  }

  @Override
  public String toString() {
    return "Configuration [classPath=" + m_classPath + ", runningFromJar="
        + m_runningFromJar + ", workingDir=" + m_workingDir + "]";
  }

  public static void main(String[] args) {
    System.out.println(Configuration.getInstance());
  }
}
