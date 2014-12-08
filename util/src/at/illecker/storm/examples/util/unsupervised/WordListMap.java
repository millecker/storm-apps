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
package at.illecker.storm.examples.util.unsupervised;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WordListMap<V> extends TreeMap<String, V> {
  private static final long serialVersionUID = -8666890032266194333L;
  private static final Logger LOG = LoggerFactory.getLogger(WordListMap.class);

  private List<String> m_startStrings = new ArrayList<String>();
  private boolean m_startStringsIsSorted = false;
  private Map<String, V> m_normalizedValues = new HashMap<String, V>();

  @Override
  public V put(String key, V value) {
    if (key.endsWith("*")) {
      String startString = key.substring(0, key.length() - 1);
      // LOG.info("Add startStrings: '" + key + "' startingWith: '" +
      // startString + "'");
      m_startStrings.add(startString);
      m_startStringsIsSorted = false;
    }
    return super.put(key, value);
  }

  public V put(String key, V value, V normalizedValue) {
    m_normalizedValues.put(key, normalizedValue);
    return put(key, value);
  }

  private String searchForMatchingKey(String key) {
    if (!m_startStringsIsSorted) {
      Collections.sort(m_startStrings);
      m_startStringsIsSorted = true;
    }
    // Comparator checks if a key starts with given key string
    Comparator<String> startsWithComparator = new Comparator<String>() {
      public int compare(String currentItem, String key) {
        if (key.startsWith(currentItem)) {
          return 0;
        }
        return currentItem.compareTo(key);
      }
    };

    // binarySearch in sorted list
    int index = Collections.binarySearch(m_startStrings, key,
        startsWithComparator);
    if (index >= 0) {
      return m_startStrings.get(index);
    }
    return null;
  }

  public V matchKey(String key, boolean normalizedValue) {
    V result = super.get(key);
    if (result == null) {
      String matchingKey = searchForMatchingKey(key);
      if (matchingKey != null) {
        // LOG.info("Found match: " + key + "*" + " for " + searchKey);
        if (normalizedValue) {
          result = m_normalizedValues.get(key + "*");
        } else {
          result = super.get(key + "*");
        }
      }
    }
    return result;
  }

  public static WordListMap<Double> loadWordRatings(InputStream is,
      double minValue, double maxValue) {
    WordListMap<Double> wordListMap = new WordListMap<Double>();
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(is));
      String str = "";
      double actualMaxValue = Double.MIN_VALUE;
      double actualMinValue = Double.MAX_VALUE;
      while ((str = reader.readLine()) != null) {
        if (str.trim().length() == 0) {
          continue;
        }
        String[] values = str.split("\t");
        double value = Double.parseDouble(values[1]);
        // Feature scaling
        double normalizedValue = (value - minValue) / (maxValue - minValue);
        // LOG.info("Add Key: '" + values[0] + "' Value: '" + values[1]
        // + "' normalizedValue: '" + normalizedValue + "'");

        // check min and max values
        if (value > actualMaxValue) {
          actualMaxValue = value;
        }
        if (value < actualMinValue) {
          actualMinValue = value;
        }

        wordListMap.put(values[0], value, normalizedValue);
      }
      LOG.info("Loaded " + wordListMap.size() + " items [maxValue: "
          + actualMaxValue + ", minValue:" + actualMinValue + "]");

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

      return wordListMap;

    } catch (IOException e) {
      LOG.error(e.getMessage());
    } finally {
      try {
        if (reader != null) {
          reader.close();
        }
      } catch (IOException e) {
        LOG.error(e.getMessage());
      }
    }
    return null;
  }
}
