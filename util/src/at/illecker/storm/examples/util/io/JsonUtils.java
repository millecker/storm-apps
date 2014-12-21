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
package at.illecker.storm.examples.util.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

public class JsonUtils {
  private static final Logger LOG = LoggerFactory.getLogger(JsonUtils.class);

  public static List<Map<String, Object>> readJsonFile(File jsonFile) {
    LOG.info("Load file " + jsonFile.getAbsolutePath());
    try {
      return readJsonStream(new FileInputStream(jsonFile));
    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage());
    }
    return null;
  }

  public static List<Map<String, Object>> readJsonStream(
      InputStream jsonInputStream) {
    BufferedReader br = null;
    try {
      br = new BufferedReader(new InputStreamReader(jsonInputStream));
      GsonBuilder builder = new GsonBuilder();
      List<Map<String, Object>> elements = (List<Map<String, Object>>) builder
          .create().fromJson(br, Object.class);
      LOG.info("Loaded " + " elements: " + elements.size());
      return elements;
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (IOException ignore) {
        }
      }
    }
  }
}