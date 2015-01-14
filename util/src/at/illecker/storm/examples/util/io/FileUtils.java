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
package at.illecker.storm.examples.util.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.dictionaries.WordListMap;
import at.illecker.storm.examples.util.tweet.Tweet;

public class FileUtils {
  private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

  public static List<Tweet> readTweets(String file, Dataset property) {
    return readTweets(IOUtils.getInputStream(file), property);
  }

  public static List<Tweet> readTweets(InputStream is, Dataset dataset) {
    List<Tweet> tweets = new ArrayList<Tweet>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        String[] values = line.split(dataset.getDelimiter());
        long id = Long.parseLong(values[dataset.getIdIndex()]);
        String text = values[dataset.getTextIndex()];
        String label = values[dataset.getLabelIndex()].toLowerCase().trim();
        double score = -1;
        if (label.equals(dataset.getNegativeLabel())) {
          score = dataset.getNegativeValue();
        } else if (label.equals(dataset.getNeutralLabel())) {
          score = dataset.getNeutralValue();
        } else if (label.equals(dataset.getPositiveLabel())) {
          score = dataset.getPositiveValue();
        }
        tweets.add(new Tweet(id, text, score));
      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    LOG.info("Loaded total " + tweets.size() + " tweets");
    return tweets;
  }

  public static Map<String, String> readFile(String file, String splitRegex) {
    return readFile(IOUtils.getInputStream(file), splitRegex, false);
  }

  public static Map<String, String> readFile(InputStream is, String splitRegex,
      boolean logging) {
    Map<String, String> table = new HashMap<String, String>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex, 2);
        table.put(values[0].trim(), values[1].trim());
        if (logging) {
          LOG.info("Add entry key: '" + values[0].trim() + "' value: '"
              + values[1].trim() + "'");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    LOG.info("Loaded total " + table.size() + " entries");
    return table;
  }

  public static Map<String, Double> readFile(String file, String splitRegex,
      boolean featureScaling, double minValue, double maxValue) {
    return readFile(IOUtils.getInputStream(file), splitRegex, featureScaling,
        minValue, maxValue, false);
  }

  public static Map<String, Double> readFile(InputStream is, String splitRegex,
      boolean featureScaling, double minValue, double maxValue, boolean logging) {
    Map<String, Double> map = new HashMap<String, Double>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    double actualMaxValue = Double.MIN_VALUE;
    double actualMinValue = Double.MAX_VALUE;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex, 2);
        String key = values[0].trim();
        double value = Double.parseDouble(values[1].trim());

        if (featureScaling) {
          // Feature scaling
          double normalizedValue = (value - minValue) / (maxValue - minValue);

          if (logging) {
            LOG.info("Add Key: '" + key + "' Value: '" + value
                + "' normalizedValue: '" + normalizedValue + "'");
          }

          // check min and max values
          if (value > actualMaxValue) {
            actualMaxValue = value;
          }
          if (value < actualMinValue) {
            actualMinValue = value;
          }

          value = normalizedValue;
        }

        map.put(key, value);
        if (logging) {
          LOG.info("Add entry key: '" + key + "' value: '" + value + "'");
        }
      }
    } catch (IOException e) {
      LOG.error(e.getMessage());
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    LOG.info("Loaded total " + map.size() + " items [minValue: "
        + actualMinValue + ", maxValue: " + actualMaxValue + "]");

    if (featureScaling) {
      if (minValue != actualMinValue) {
        LOG.error("minValue is incorrect! actual minValue: " + actualMinValue
            + " given minValue: " + minValue
            + " (normalized values might be wrong!)");
      }
      if (maxValue != actualMaxValue) {
        LOG.error("maxValue is incorrect! actual maxValue: " + actualMaxValue
            + " given maxValue: " + maxValue
            + " (normalized values might be wrong!)");
      }
    }
    return map;
  }

  public static WordListMap<Double> readWordListMap(String file,
      String splitRegex, boolean featureScaling, double minValue,
      double maxValue) {
    return readWordListMap(IOUtils.getInputStream(file), splitRegex,
        featureScaling, minValue, maxValue, false);
  }

  public static WordListMap<Double> readWordListMap(InputStream is,
      String splitRegex, boolean featureScaling, double minValue,
      double maxValue, boolean logging) {
    WordListMap<Double> wordListMap = new WordListMap<Double>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    double actualMaxValue = Double.MIN_VALUE;
    double actualMinValue = Double.MAX_VALUE;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        if (line.trim().length() == 0) {
          continue;
        }
        String[] values = line.split(splitRegex);
        String key = values[0].trim();
        double value = Double.parseDouble(values[1].trim());

        if (featureScaling) {
          // Feature scaling
          double normalizedValue = (value - minValue) / (maxValue - minValue);

          if (logging) {
            LOG.info("Add Key: '" + key + "' Value: '" + value
                + "' normalizedValue: '" + normalizedValue + "'");
          }

          // check min and max values
          if (value > actualMaxValue) {
            actualMaxValue = value;
          }
          if (value < actualMinValue) {
            actualMinValue = value;
          }

          value = normalizedValue;
        }
        wordListMap.put(key, value);
      }
      LOG.info("Loaded " + wordListMap.size() + " items [minValue: "
          + actualMinValue + ", maxValue: " + actualMaxValue + "]");

      if (featureScaling) {
        if (minValue != actualMinValue) {
          LOG.error("minValue is incorrect! actual minValue: " + actualMinValue
              + " given minValue: " + minValue
              + " (normalized values might be wrong!)");
        }
        if (maxValue != actualMaxValue) {
          LOG.error("maxValue is incorrect! actual maxValue: " + actualMaxValue
              + " given maxValue: " + maxValue
              + " (normalized values might be wrong!)");
        }
      }

      return wordListMap;

    } catch (IOException e) {
      LOG.error(e.getMessage());
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    return null;
  }

  public static Set<String> readFile(String file) {
    return readFile(IOUtils.getInputStream(file), false, false);
  }

  public static Set<String> readFile(String file, boolean toLowerCase) {
    return readFile(IOUtils.getInputStream(file), toLowerCase, false);
  }

  public static Set<String> readFile(InputStream is, boolean toLowerCase,
      boolean logging) {
    Set<String> set = new HashSet<String>();
    InputStreamReader isr = null;
    BufferedReader br = null;
    try {
      isr = new InputStreamReader(is, "UTF-8");
      br = new BufferedReader(isr);
      String line = "";
      while ((line = br.readLine()) != null) {
        line = line.trim();
        if (line.length() == 0) {
          continue;
        }
        if (toLowerCase) {
          line = line.toLowerCase();
        }
        set.add(line);
        if (logging) {
          LOG.info("Add entry: '" + line + "'");
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
      if (isr != null) {
        try {
          isr.close();
        } catch (IOException ignore) {
        }
      }
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
    LOG.info("Loaded total " + set.size() + " entries");
    return set;
  }
}
