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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtil {
  public static final int BUFFER_SIZE = 2048;
  private static final Logger LOG = LoggerFactory.getLogger(FileUtil.class);

  public static void extractTarGz(String inputTarGz, String outDir)
      throws IOException {

    FileInputStream fin = new FileInputStream(inputTarGz);
    BufferedInputStream in = new BufferedInputStream(fin);
    GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in);
    TarArchiveInputStream tarIn = new TarArchiveInputStream(gzIn);

    TarArchiveEntry entry = null;
    // read Tar entries
    while ((entry = (TarArchiveEntry) tarIn.getNextEntry()) != null) {
      LOG.info("Extracting: " + entry.getName());

      if (entry.isDirectory()) { // create directory
        File f = new File(outDir + entry.getName());
        f.mkdirs();
      } else { // decompress file
        int count;
        byte data[] = new byte[BUFFER_SIZE];

        FileOutputStream fos = new FileOutputStream(outDir + entry.getName());
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER_SIZE);
        while ((count = tarIn.read(data, 0, BUFFER_SIZE)) != -1) {
          dest.write(data, 0, count);
        }
        dest.close();
      }
    }
    // close input stream
    tarIn.close();
  }

  public static void delete(File f) throws IOException {
    LOG.info("Delete: " + f.getAbsolutePath());
    if (f.isDirectory()) {
      for (File c : f.listFiles())
        delete(c);
    }
    if (!f.delete()) {
      throw new FileNotFoundException("Failed to delete file: " + f);
    }
  }
}
