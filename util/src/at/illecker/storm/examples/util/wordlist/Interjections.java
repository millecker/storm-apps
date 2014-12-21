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
package at.illecker.storm.examples.util.wordlist;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.FileUtils;

public class Interjections {
  private static final Logger LOG = LoggerFactory
      .getLogger(Interjections.class);
  private static final Interjections instance = new Interjections();

  private Configuration m_conf;
  private Set<String> m_interjections = null;
  private List<Pattern> m_interjectionPatterns = null;

  private Interjections() {
    m_conf = Configuration.getInstance();
    InputStream is = null;
    try {
      Map<String, Properties> interjectionFiles = m_conf.getInterjections();
      for (Map.Entry<String, Properties> interjectionEntry : interjectionFiles
          .entrySet()) {
        String file = interjectionEntry.getKey();
        if (m_conf.isRunningWithinJar()) {
          is = ClassLoader.getSystemResourceAsStream(file);
        } else {
          is = new FileInputStream(file);
        }
        Boolean containsRegex = (Boolean) interjectionEntry.getValue().get(
            "containsRegex");

        Set<String> interjections = FileUtils.readFile(is);

        if (containsRegex) {
          LOG.info("Load Interjections including regex patterns from: " + file);
          if (m_interjectionPatterns == null) {
            m_interjectionPatterns = new ArrayList<Pattern>();
          }
          for (String interjection : interjections) {
            m_interjectionPatterns.add(Pattern
                .compile("^" + interjection + "$"));
          }
        } else {
          LOG.info("Load Interjections from: " + file);
          if (m_interjections == null) {
            m_interjections = new HashSet<String>();
          }
          m_interjections.addAll(interjections);
        }
      }
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }
  }

  public static Interjections getInstance() {
    return instance;
  }

  public boolean isInterjection(String string) {
    boolean result = false;
    if (m_interjections != null) {
      result = m_interjections.contains(string);
    }

    if ((result == false) && (m_interjectionPatterns != null)) {
      for (Pattern interjection : m_interjectionPatterns) {
        Matcher m = interjection.matcher(string);
        if (m.find()) {
          return true;
        }
      }
    }

    return result;
  }

  public static void main(String[] args) {
    Interjections interjections = Interjections.getInstance();
    System.out.println("isInterjection ':))': "
        + interjections.isInterjection(":))"));
    System.out.println("isInterjection 'wooow': "
        + interjections.isInterjection("wooow"));
  }
}
