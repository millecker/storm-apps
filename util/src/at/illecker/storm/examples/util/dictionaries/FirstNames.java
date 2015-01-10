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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.FileUtils;

public class FirstNames {
  private static final Logger LOG = LoggerFactory.getLogger(FirstNames.class);
  private static final FirstNames instance = new FirstNames();

  private Set<String> m_firstNames = null;

  private FirstNames() {
    for (String file : Configuration.getFirstNames()) {
      LOG.info("Load FirstNames from: " + file);
      if (m_firstNames == null) {
        m_firstNames = FileUtils.readFile(file, true);
      } else {
        m_firstNames.addAll(FileUtils.readFile(file, true));
      }
    }
  }

  public static FirstNames getInstance() {
    return instance;
  }

  public boolean isFirstName(String value) {
    return m_firstNames.contains(value.toLowerCase());
  }

  public static void main(String[] args) {
    FirstNames firstNames = FirstNames.getInstance();
    // test FirstNames
    String[] testFirstNames = new String[] { "Kevin", "Martin", "martin",
        "justin" };
    for (String s : testFirstNames) {
      System.out
          .println("isFirstName(" + s + "): " + firstNames.isFirstName(s));
    }
  }
}
