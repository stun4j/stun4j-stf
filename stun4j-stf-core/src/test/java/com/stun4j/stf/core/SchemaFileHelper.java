/*
 * Copyright 2022-? the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stun4j.stf.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.lang3.tuple.Triple;
import org.springframework.core.io.ClassPathResource;

import com.google.common.base.Strings;
import com.stun4j.guid.core.LocalGuid;

public class SchemaFileHelper {
  public static String classpath(String dbVendor, long roundId) {
    String schemaFileParentPath = parentPath(dbVendor);
    return Strings.lenientFormat("%s%s%s%s", schemaFileParentPath, File.separator, roundId,
        TestConsts.GENERATED_SCHEMA_FILE_SUFFIX);
  }

  public static Triple<String, File, Long> extracted(String dbVendor) {
    String schemaFileParentPath = parentPath(dbVendor);
    String dftDdlTplPath = Strings.lenientFormat("%s%sddl.sql", schemaFileParentPath, File.separator);
    long roundId = -1;
    try {
      roundId = LocalGuid.instance().next();
      File fileTpl = new ClassPathResource(dftDdlTplPath).getFile();
      File SCHEMA_FILE_WITH_TBL_NAME_CHANGED = new File(
          fileTpl.getParent() + File.separator + roundId + TestConsts.GENERATED_SCHEMA_FILE_SUFFIX);

      String TBL_NAME = TestConsts.GENERATED_TBL_PREFIX + roundId;
      try (BufferedReader br = new BufferedReader(new FileReader(fileTpl));
          RandomAccessFile fileTo = new RandomAccessFile(SCHEMA_FILE_WITH_TBL_NAME_CHANGED, "rw");
          FileChannel fchTo = fileTo.getChannel()) {
        String line = br.readLine();
        do {
          if (line.startsWith("create table stn_stf (")) {
            line = line.replaceFirst("stn_stf", TBL_NAME);
          }
          if (line.startsWith("create index") && line.indexOf("on stn_stf ") != -1) {
            line = line.replaceAll("stn_stf", TBL_NAME);
          }
          fchTo.write(ByteBuffer.wrap(line.getBytes()));
        } while ((line = br.readLine()) != null);
        return Triple.of(TBL_NAME, SCHEMA_FILE_WITH_TBL_NAME_CHANGED, roundId);
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  public static void cleanup(File schemaFileWithTblNameChanged) {
    if (schemaFileWithTblNameChanged == null) return;
    try {
      schemaFileWithTblNameChanged.delete();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  static String parentPath(String dbVendor) {
    String schemaFileParentPath = Strings.lenientFormat("schema%s%s", File.separator, dbVendor.toLowerCase());
    return schemaFileParentPath;
  }
}