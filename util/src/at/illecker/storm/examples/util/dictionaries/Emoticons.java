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

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
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
  private List<Pattern> m_emoticonPatternsStrict = null;

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
          m_emoticonPatternsStrict = new ArrayList<Pattern>();
        }
        for (String emoticon : emoticons) {
          m_emoticonPatterns.add(Pattern.compile(emoticon));
          m_emoticonPatternsStrict.add(Pattern.compile("^" + emoticon + "$"));
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

  public SimpleEntry<Boolean, String[]> containsEmoticon(String value) {
    value = value.toLowerCase();
    if ((m_emoticons != null) && (m_emoticons.contains(value))) {
      return new SimpleEntry<Boolean, String[]>(true, new String[] { value });
    }

    if (m_emoticonPatterns != null) {
      for (int i = 0; i < m_emoticonPatterns.size(); i++) {
        Pattern emoticon = m_emoticonPatterns.get(i);
        Matcher m = emoticon.matcher(value);
        if (m.find()) {
          Pattern emoticonStrict = m_emoticonPatternsStrict.get(i);
          // check if it's a strict match
          if (emoticonStrict.matcher(value).find()) {
            return new SimpleEntry<Boolean, String[]>(true,
                new String[] { value });
          } else {
            if (m.start() == 0) {
              return new SimpleEntry<Boolean, String[]>(true, new String[] {
                  value.substring(0, m.end()), value.substring(m.end()) });
            } else if (m.end() == value.length()) {
              return new SimpleEntry<Boolean, String[]>(true, new String[] {
                  value.substring(0, m.start()), value.substring(m.start()) });
            }
          }
        }
      }
    }
    return new SimpleEntry<Boolean, String[]>(false, null);
  }

  public static void main(String[] args) {
    Emoticons emoticons = Emoticons.getInstance();
    // test Emoticons
    String[] testEmoticons = new String[] { ":)", ":-)", ":))))", "happy:-)",
        "sad:-(", ";-)yeah", "test" };
    for (String s : testEmoticons) {
      System.out.println("containsEmoticon(" + s + "): "
          + emoticons.containsEmoticon(s).getKey() + " "
          + Arrays.toString(emoticons.containsEmoticon(s).getValue()));
    }
  }
}
