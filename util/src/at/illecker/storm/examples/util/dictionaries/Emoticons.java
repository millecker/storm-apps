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
package at.illecker.storm.examples.util.dictionaries;

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

public class Emoticons {
  private static final Logger LOG = LoggerFactory.getLogger(Emoticons.class);
  private static final Emoticons instance = new Emoticons();

  private Set<String> m_emoticons = null;
  private List<Pattern> m_emoticonPatterns = null;

  private Emoticons() {
    Map<String, Properties> emoticonFiles = Configuration.getEmoticons();
    for (Map.Entry<String, Properties> emoticonEntry : emoticonFiles.entrySet()) {
      String file = emoticonEntry.getKey();
      Boolean containsRegex = (Boolean) emoticonEntry.getValue().get(
          "containsRegex");

      Set<String> emoticons = FileUtils.readFile(file);

      if (containsRegex) {
        LOG.info("Loaded Emoticons including regex patterns from: " + file);
        if (m_emoticonPatterns == null) {
          m_emoticonPatterns = new ArrayList<Pattern>();
        }
        for (String emoticon : emoticons) {
          m_emoticonPatterns.add(Pattern.compile("^" + emoticon + "$"));
        }
      } else {
        LOG.info("Loaded Emoticons from: " + file);
        if (m_emoticons == null) {
          m_emoticons = new HashSet<String>();
        }
        m_emoticons.addAll(emoticons);
      }
    }
  }

  public static Emoticons getInstance() {
    return instance;
  }

  public boolean isEmoticon(String value) {
    value = value.toLowerCase();
    boolean result = false;
    if (m_emoticons != null) {
      result = m_emoticons.contains(value);
    }

    if ((result == false) && (m_emoticonPatterns != null)) {
      for (Pattern emoticon : m_emoticonPatterns) {
        Matcher m = emoticon.matcher(value);
        if (m.find()) {
          return true;
        }
      }
    }

    return result;
  }

  public static void main(String[] args) {
    Emoticons emoticons = Emoticons.getInstance();
    // test Emoticons
    String[] testEmoticons = new String[] { ":)", ":-)", ":))))" };
    for (String s : testEmoticons) {
      System.out.println("isEmoticon(" + s + "): " + emoticons.isEmoticon(s));
    }
  }
}
