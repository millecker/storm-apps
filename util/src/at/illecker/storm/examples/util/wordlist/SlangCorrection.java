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
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.FileUtil;

public class SlangCorrection {
  private static final Logger LOG = LoggerFactory
      .getLogger(SlangCorrection.class);
  private static final SlangCorrection instance = new SlangCorrection();

  private Configuration m_conf;
  private Map<String, String> m_slangWordList = null;

  private SlangCorrection() {
    m_conf = Configuration.getInstance();
    try {
      Map<String, Properties> slangWordLists = m_conf.getSlangWordlists();
      for (Map.Entry<String, Properties> slangWordListEntry : slangWordLists
          .entrySet()) {
        String file = slangWordListEntry.getKey();
        String separator = slangWordListEntry.getValue().getProperty(
            "separator");
        LOG.info("Load SlangLookupTable from: " + file);
        if (m_slangWordList == null) {
          m_slangWordList = FileUtil.readFile(new FileInputStream(file),
              separator);
        } else {
          Map<String, String> slangWordList = FileUtil.readFile(
              new FileInputStream(file), separator);
          for (Map.Entry<String, String> entry : slangWordList.entrySet()) {
            if (!m_slangWordList.containsKey(entry.getKey())) {
              m_slangWordList.put(entry.getKey(), entry.getValue());
            }
          }
        }
      }
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage());
    }
  }

  public static SlangCorrection getInstance() {
    return instance;
  }

  public String getCorrection(String slangString) {
    String correctionStr = null;
    if (m_slangWordList != null) {
      correctionStr = m_slangWordList.get(slangString);
    }
    // LOG.info("getCorrection('" + slangString + "'): " + correctionStr);
    return correctionStr;
  }

  public static void main(String[] args) {
    SlangCorrection slangCorrection = SlangCorrection.getInstance();
    System.out.println("SlangCorrection of 'afaik': '"
        + slangCorrection.getCorrection("afaik") + "'");
    System.out.println("SlangCorrection of 'cum': '"
        + slangCorrection.getCorrection("cum") + "'");
  }
}
