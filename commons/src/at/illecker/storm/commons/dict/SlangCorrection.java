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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.util.io.FileUtils;

public class SlangCorrection {
  private static final Logger LOG = LoggerFactory
      .getLogger(SlangCorrection.class);
  private static final SlangCorrection instance = new SlangCorrection();

  private Map<String, String[]> m_slangWordList = new HashMap<String, String[]>();

  private SlangCorrection() {
    List<Map> slangWordLists = Configuration.getSlangWordlists();
    for (Map slangWordListEntry : slangWordLists) {
      String file = (String) slangWordListEntry.get("path");
      String separator = (String) slangWordListEntry.get("delimiter");
      boolean isEnabled = (Boolean) slangWordListEntry.get("enabled");
      if (isEnabled) {
        LOG.info("Load SlangLookupTable from: " + file);
        Map<String, String> slangWordList = FileUtils.readFile(file, separator);
        for (Map.Entry<String, String> entry : slangWordList.entrySet()) {
          if (!m_slangWordList.containsKey(entry.getKey())) {
            m_slangWordList.put(entry.getKey(), entry.getValue().split(" "));
          }
        }
      }
    }
  }

  public static SlangCorrection getInstance() {
    return instance;
  }

  public String[] getCorrection(String token) {
    // LOG.info("getCorrection('" + token + "'): "
    // + Arrays.toString(m_slangWordList.get(token)));
    return m_slangWordList.get(token);
  }

  public static void main(String[] args) {
    SlangCorrection slangCorrection = SlangCorrection.getInstance();

    // test testSlangCorrection
    String[] testSlangCorrection = new String[] { "afaik", "cum", "w/", "Fri",
        "fri", "Sat", "sat", "Sun", "sun", "U.S.", "U.K." };
    for (String s : testSlangCorrection) {
      System.out.println("slang correction of '" + s + "': '"
          + Arrays.toString(slangCorrection.getCorrection(s)) + "'");
    }
  }

}
