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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import twitter4j.Status;
import at.illecker.storm.commons.util.io.IOUtils;
import at.illecker.storm.commons.util.io.JsonUtils;

public class Configuration {
  private static final Logger LOG = LoggerFactory
      .getLogger(Configuration.class);

  public static final boolean RUNNING_WITHIN_JAR = Configuration.class
      .getResource("Configuration.class").toString().startsWith("jar:");

  public static final String WORKING_DIR_PATH = (RUNNING_WITHIN_JAR) ? ""
      : System.getProperty("user.dir") + File.separator;

  public static final String TEMP_DIR_PATH = System
      .getProperty("java.io.tmpdir");

  public static final String GLOBAL_RESOURCES_DATASETS_SEMEVAL = "global.resources.datasets.semeval";
  public static final String GLOBAL_RESOURCES_DATASETS_CRAWLER = "global.resources.datasets.crawler";

  public static final String GLOBAL_RESOURCES_DICT = "global.resources.dict";
  public static final String GLOBAL_RESOURCES_DICT_SENTIMENT = "global.resources.dict.sentiment";
  public static final String GLOBAL_RESOURCES_DICT_SENTIMENT_SENTIWORDNET_PATH = "global.resources.dict.sentiment.sentiwordnet.path";
  public static final String GLOBAL_RESOURCES_DICT_SLANG = "global.resources.dict.slang";
  public static final String GLOBAL_RESOURCES_DICT_WORDNET_PATH = "global.resources.dict.wordnet.path";

  public static final String GLOBAL_RESOURCES_POSTAGGER_GATE_MODEL_PATH = "global.resources.postagger.gate.model.path";
  public static final String GLOBAL_RESOURCES_POSTAGGER_GATE_MODEL_FAST_PATH = "global.resources.postagger.gate.model.fast.path";

  public static final Map CONFIG = readConfig();

  @SuppressWarnings("rawtypes")
  public static Map readConfig() {
    Map conf = readConfigFile(WORKING_DIR_PATH + "conf/defaults.yaml", true);
    // read custom config
    LOG.info("Try to load user-specific config...");
    Map customConfig = readConfigFile(WORKING_DIR_PATH
        + "conf/configuration.yaml", false);
    if (customConfig != null) {
      conf.putAll(customConfig);
    }
    return conf;
  }

  @SuppressWarnings("rawtypes")
  public static Map readConfigFile(String file, boolean mustExist) {
    Yaml yaml = new Yaml(new SafeConstructor());
    Map ret = null;
    InputStream input = IOUtils.getInputStream(file);
    if (input != null) {
      ret = (Map) yaml.load(new InputStreamReader(input));
      LOG.info("Loaded " + file);
      try {
        input.close();
      } catch (IOException e) {
        LOG.error("IOException: " + e.getMessage());
      }
    } else if (mustExist) {
      LOG.error("Config file " + file + " was not found!");
    }
    if (ret == null) {
      if (mustExist) {
        throw new RuntimeException("Config file " + file + " was not found!");
      } else {
        ret = new HashMap();
      }
    }
    return new HashMap(ret);
  }

  public static <K, V> V get(K key) {
    return get(key, null);
  }

  public static <K, V> V get(K key, V defaultValue) {
    return get((Map<K, V>) CONFIG, key, defaultValue);
  }

  public static <K, V> V get(Map<K, V> map, K key, V defaultValue) {
    V value = map.get(key);
    if (value == null) {
      value = defaultValue;
    }
    return value;
  }

  public static Dataset getDataSetSemEval2013() {
    return Dataset.readFromYaml((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_SEMEVAL)).get("2013"));
  }

  public static Dataset getDataSetSemEval2013Mixed() {
    return Dataset.readFromYaml((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_SEMEVAL)).get("2013Mixed"));
  }

  public static Dataset getDataSetSemEval2014() {
    return Dataset.readFromYaml((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_SEMEVAL)).get("2014"));
  }

  public static List<Status> getDataSetCrawler() {
    return getDataSetCrawler((String) ((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_CRAWLER)).get("Crawler"))
        .get("language.filter"));
  }

  public static List<Status> getDataSetCrawler(String filterLanguage) {
    return JsonUtils.readTweetsDirectory((String) ((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_CRAWLER)).get("Crawler")).get("path"),
        filterLanguage);
  }

  public static List<Status> getDataSetCrawlerTest() {
    return getDataSetCrawlerTest((String) ((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_CRAWLER)).get("CrawlerTest"))
        .get("language.filter"));
  }

  public static List<Status> getDataSetCrawlerTest(String filterLanguage) {
    return JsonUtils.readTweetsDirectory((String) ((Map) ((Map) CONFIG
        .get(GLOBAL_RESOURCES_DATASETS_CRAWLER)).get("CrawlerTest"))
        .get("path"), filterLanguage);
  }

  public static List<String> getNameEntities() {
    return (List<String>) ((Map) CONFIG.get(GLOBAL_RESOURCES_DICT))
        .get("NameEntities");
  }

  public static List<String> getFirstNames() {
    return (List<String>) ((Map) CONFIG.get(GLOBAL_RESOURCES_DICT))
        .get("FirstNames");
  }

  public static List<Map> getEmoticons() {
    return (List<Map>) ((Map) CONFIG.get(GLOBAL_RESOURCES_DICT))
        .get("Emoticons");
  }

  public static List<Map> getInterjections() {
    return (List<Map>) ((Map) CONFIG.get(GLOBAL_RESOURCES_DICT))
        .get("Interjections");
  }

  public static List<String> getStopWords() {
    return (List<String>) ((Map) CONFIG.get(GLOBAL_RESOURCES_DICT))
        .get("StopWords");
  }

  public static List<Map> getSentimentWordlists() {
    return (List<Map>) CONFIG.get(GLOBAL_RESOURCES_DICT_SENTIMENT);
  }

  public static String getSentiWordNetDict() {
    return (String) CONFIG
        .get(GLOBAL_RESOURCES_DICT_SENTIMENT_SENTIWORDNET_PATH);
  }

  public static List<Map> getSlangWordlists() {
    return (List<Map>) CONFIG.get(GLOBAL_RESOURCES_DICT_SLANG);
  }

  public static String getWordNetDict() {
    return (String) CONFIG.get(GLOBAL_RESOURCES_DICT_WORDNET_PATH);
  }

  public static String getPOSTaggingModel() {
    return (String) CONFIG.get(GLOBAL_RESOURCES_POSTAGGER_GATE_MODEL_PATH);
  }

  public static String getPOSTaggingModelFast() {
    return (String) CONFIG.get(GLOBAL_RESOURCES_POSTAGGER_GATE_MODEL_FAST_PATH);
  }

}
