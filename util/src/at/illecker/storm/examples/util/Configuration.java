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
package at.illecker.storm.examples.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Configuration {

  private static final Logger LOG = LoggerFactory
      .getLogger(Configuration.class);
  private static final Configuration instance = new Configuration();

  private static final String CONF_WORD_LIST_PATH = "resources"
      + File.separator + "wordlists";

  private String m_userDir;
  private String m_workingDir;
  private String m_classPath;
  private boolean m_runningFromJar;

  private Configuration() {
    m_userDir = System.getProperty("user.dir");

    m_classPath = Configuration.class.getResource("Configuration.class")
        .toString();
    if (m_classPath.startsWith("jar:")) {
      LOG.info("Running within a jar file...");
      m_runningFromJar = true;
    } else {
      LOG.info("Running outside of a jar file...");
      m_runningFromJar = false;
      m_workingDir = m_userDir;
    }
  }

  public static Configuration getInstance() {
    return instance;
  }

  public Map<String, String> getSlangWordlists() {
    String wordListDir = m_workingDir + File.separator + CONF_WORD_LIST_PATH
        + File.separator;
    Map<String, String> slangWordLists = new HashMap<String, String>();
    // Add SentStrength SlangLookupTable
    slangWordLists.put(wordListDir
        + "SentStrength_Data_Sept2011_SlangLookupTable.txt", "\t");
    // Add GATE SlangLookupTable orth.en
    slangWordLists.put(wordListDir + "GATE_slang.en.csv", ",");
    return slangWordLists;
  }

  @Override
  public String toString() {
    return "Configuration [classPath=" + m_classPath + ", runningFromJar="
        + m_runningFromJar + ", workingDir=" + m_workingDir + "]";
  }

  public static void main(String[] args) {
    System.out.println(Configuration.getInstance());
  }
}
