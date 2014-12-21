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
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.FileUtils;

public class NameEntities {
  private static final Logger LOG = LoggerFactory.getLogger(NameEntities.class);
  private static final NameEntities instance = new NameEntities();

  private Configuration m_conf;
  private Set<String> m_nameEntities = null;

  private NameEntities() {
    m_conf = Configuration.getInstance();
    InputStream is = null;
    try {
      for (String nameEntityFile : m_conf.getNameEntities()) {
        LOG.info("Load NameEntities from: " + nameEntityFile);
        if (m_conf.isRunningWithinJar()) {
          is = ClassLoader.getSystemResourceAsStream(nameEntityFile);
        } else {
          is = new FileInputStream(nameEntityFile);
        }
        if (m_nameEntities == null) {
          m_nameEntities = FileUtils.readFile(is);
        } else {
          m_nameEntities.addAll(FileUtils.readFile(is));
        }
      }
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
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
