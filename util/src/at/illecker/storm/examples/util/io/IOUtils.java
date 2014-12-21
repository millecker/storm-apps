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

import java.io.File;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IOUtils {
  private static final Logger LOG = LoggerFactory.getLogger(IOUtils.class);

  // TODO
  // https://github.com/stanfordnlp/CoreNLP/blob/master/src/edu/stanford/nlp/io/IOUtils.java
  // getInputStreamFromURLOrClasspathOrFileSystem

  public static void delete(File file) throws IOException {
    IOUtils.delete(file, false);
  }

  public static void delete(File file, boolean logging) throws IOException {
    if (logging) {
      LOG.info("Delete: " + file.getAbsolutePath());
    }
    if (file.isDirectory()) {
      for (File c : file.listFiles())
        delete(c, logging);
    }
    if (!file.delete()) {
      LOG.error("Failed to delete file: " + file);
    }
  }

}
