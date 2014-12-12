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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.io.FileUtil;

public class Interjections {

  public static final String INTERJECTIONS_LIST = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "GATE_interjections.regex";

  private static final Logger LOG = LoggerFactory
      .getLogger(Interjections.class);
  private static final Interjections instance = new Interjections();

  // GATE interjections
  private List<Pattern> m_interjections;

  private Interjections() {
    try {
      LOG.info("Load Interjections from: " + INTERJECTIONS_LIST);
      Set<String> interjections = FileUtil.readFile(new FileInputStream(
          INTERJECTIONS_LIST));

      m_interjections = new ArrayList<Pattern>();
      for (String interjection : interjections) {
        m_interjections.add(Pattern.compile("^" + interjection + "$"));
      }

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static Interjections getInstance() {
    return instance;
  }

  public boolean isInterjection(String string) {
    for (Pattern interjection : m_interjections) {
      Matcher m = interjection.matcher(string);
      if (m.find()) {
        return true;
      }
    }
    return false;
  }

  public static void main(String[] args) {
    Interjections interjections = Interjections.getInstance();
    System.out.println("isInterjection ':))': "
        + interjections.isInterjection(":))"));
    System.out.println("isInterjection 'wooow': "
        + interjections.isInterjection("wooow"));
  }
}
