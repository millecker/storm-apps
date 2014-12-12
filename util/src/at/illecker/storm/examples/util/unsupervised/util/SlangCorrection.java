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
package at.illecker.storm.examples.util.unsupervised.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.io.FileUtil;

public class SlangCorrection {

  public static final String SLANG_WORD_LIST1 = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "wordlists"
      + File.separator + "SentStrength_Data_Sept2011_SlangLookupTable.txt";

  public static final String SLANG_WORD_LIST2 = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "wordlists"
      + File.separator + "GATE_slang.en.csv";

  private static final Logger LOG = LoggerFactory
      .getLogger(SlangCorrection.class);
  private static final SlangCorrection instance = new SlangCorrection();

  // SentStrength SlangLookupTable
  private Map<String, String> m_slangWordList1;
  // GATE SlangLookupTable orth.en
  private Map<String, String> m_slangWordList2;

  private SlangCorrection() {
    try {
      LOG.info("Load SentStrength SlangLookupTable from: " + SLANG_WORD_LIST1);
      m_slangWordList1 = FileUtil.readFile(
          new FileInputStream(SLANG_WORD_LIST1), "\t");

      LOG.info("Load GATE SlangLookupTable from: " + SLANG_WORD_LIST2);
      m_slangWordList2 = FileUtil.readFile(
          new FileInputStream(SLANG_WORD_LIST2), ",");

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static SlangCorrection getInstance() {
    return instance;
  }

  public String getCorrection(String slangString) {
    // check SentStrength SlangLookupTable
    String correctionStr = null;
    if (m_slangWordList1 != null) {
      correctionStr = m_slangWordList1.get(slangString);
    }
    // check GATE SlangLookupTable
    if (correctionStr == null) {
      if (m_slangWordList2 != null) {
        correctionStr = m_slangWordList2.get(slangString);
      }
    }
    LOG.info("getCorrection('" + slangString + "'): " + correctionStr);
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
