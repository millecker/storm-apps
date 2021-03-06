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

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.util.io.FileUtils;

public class NameEntities {
  private static final Logger LOG = LoggerFactory.getLogger(NameEntities.class);
  private static final NameEntities INSTANCE = new NameEntities();

  private Set<String> m_nameEntities = null;

  private NameEntities() {
    for (String file : Configuration.getNameEntities()) {
      LOG.info("Load NameEntities from: " + file);
      if (m_nameEntities == null) {
        m_nameEntities = FileUtils.readFile(file);
      } else {
        m_nameEntities.addAll(FileUtils.readFile(file));
      }
    }
  }

  public static NameEntities getInstance() {
    return INSTANCE;
  }

  public boolean isNameEntity(String value) {
    return m_nameEntities.contains(value.toLowerCase());
  }

  public static void main(String[] args) {
    NameEntities nameEntities = NameEntities.getInstance();
    System.out.println("NameEntities of 'apple': '"
        + nameEntities.isNameEntity("apple") + "'");
    System.out.println("NameEntities of 'facebook': '"
        + nameEntities.isNameEntity("facebook") + "'");
  }
}
