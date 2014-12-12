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
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.io.FileUtil;

public class NameEntities {

  public static final String NAME_ENTITIES_LIST1 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "GATE_cities.txt";

  public static final String NAME_ENTITIES_LIST2 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "GATE_corps.txt";

  public static final String NAME_ENTITIES_LIST3 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "GATE_names.txt";

  private static final Logger LOG = LoggerFactory.getLogger(NameEntities.class);
  private static final NameEntities instance = new NameEntities();

  // GATE name entities
  private Set<String> m_nameEntities;

  private NameEntities() {
    try {
      LOG.info("Load NameEntities cities from: " + NAME_ENTITIES_LIST1);
      m_nameEntities = FileUtil.readFile(new FileInputStream(
          NAME_ENTITIES_LIST1));

      LOG.info("Load NameEntities corps from: " + NAME_ENTITIES_LIST2);
      m_nameEntities.addAll(FileUtil.readFile(new FileInputStream(
          NAME_ENTITIES_LIST2)));

      LOG.info("Load NameEntities names from: " + NAME_ENTITIES_LIST2);
      m_nameEntities.addAll(FileUtil.readFile(new FileInputStream(
          NAME_ENTITIES_LIST3)));

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static NameEntities getInstance() {
    return instance;
  }

  public boolean isNameEntity(String string) {
    return m_nameEntities.contains(string);
  }

  public static void main(String[] args) {
    NameEntities nameEntities = NameEntities.getInstance();
    System.out.println("NameEntities of 'apple': '"
        + nameEntities.isNameEntity("apple") + "'");
    System.out.println("NameEntities of 'facebook': '"
        + nameEntities.isNameEntity("facebook") + "'");
  }
}
