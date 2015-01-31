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
package at.illecker.storm.commons.dict;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.util.io.FileUtils;

public class Interjections {
  private static final Logger LOG = LoggerFactory
      .getLogger(Interjections.class);
  private static final Interjections INSTANCE = new Interjections();

  private Set<String> m_interjections = null;
  private List<Pattern> m_interjectionPatterns = null;

  private Interjections() {
    List<Map> interjectionFiles = Configuration.getInterjections();
    for (Map interjectionEntry : interjectionFiles) {
      String file = (String) interjectionEntry.get("path");
      Boolean containsRegex = (Boolean) interjectionEntry.get("containsRegex");

      Set<String> interjections = FileUtils.readFile(file);

      if (containsRegex) {
        LOG.info("Loaded Interjections including regex patterns from: " + file);
        if (m_interjectionPatterns == null) {
          m_interjectionPatterns = new ArrayList<Pattern>();
        }
        for (String interjection : interjections) {
          m_interjectionPatterns.add(Pattern.compile("^" + interjection + "$"));
        }
      } else {
        LOG.info("Loaded Interjections from: " + file);
        if (m_interjections == null) {
          m_interjections = new HashSet<String>();
        }
        m_interjections.addAll(interjections);
      }
    }
  }

  public static Interjections getInstance() {
    return INSTANCE;
  }

  public boolean isInterjection(String value) {
    value = value.toLowerCase();
    boolean result = false;
    if (m_interjections != null) {
      result = m_interjections.contains(value);
    }

    if ((result == false) && (m_interjectionPatterns != null)) {
      for (Pattern interjection : m_interjectionPatterns) {
        Matcher m = interjection.matcher(value);
        if (m.find()) {
          return true;
        }
      }
    }

    return result;
  }

  public static void main(String[] args) {
    Interjections interjections = Interjections.getInstance();
    // test Interjections
    String[] testInterjections = new String[] { "wow", "wooow", "wooo" };
    for (String s : testInterjections) {
      System.out.println("isInterjection(" + s + "): "
          + interjections.isInterjection(s));
    }
  }
}
