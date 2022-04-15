/*
 * Copyright 2012-2019 the original author or authors.
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
package com.stun4j.stf.core.support.banner;

import java.io.PrintStream;

import com.stun4j.stf.core.support.Stun4jStfVersion;
import com.stun4j.stf.core.support.ansi.AnsiColor;
import com.stun4j.stf.core.support.ansi.AnsiOutput;
import com.stun4j.stf.core.support.ansi.AnsiStyle;

/**
 * Default Banner implementation which writes the 'Stf' banner.
 * @author Phillip Webb
 * @author Jay Meng
 *         <p>
 *         From spring-boot:2.6.5,with banner changed
 */
public class Stun4jStfBanner implements Banner {
  private static final String STUN4J_STF = ": Stun4J Stf :  (/";

  private static final int STRAP_LINE_SIZE = 38;
  String[] BANNERS = {"  __,               ___   __,     _____", " (    _/_      /  /( /   (    _/_(  /  ",
      "  `.  /  , , _'--/  /     `.  /   -/-- ", "(___)(__(_/_/ /_/ _/_   (___)(__ _/    ",
      "                 //                    "};

  @Override
  public void printBanner(PrintStream out) {
    for (String line : BANNERS) {
      out.println(line);
    }
    String version = Stun4jStfVersion.getVersion();
    version = (version != null) ? " (v" + version + ")" : "(Dev Mode)";
    StringBuilder padding = new StringBuilder();
    while (padding.length() < STRAP_LINE_SIZE - (version.length() + STUN4J_STF.length())) {
      padding.append(" ");
    }

    out.println(AnsiOutput.toString(AnsiColor.BRIGHT_CYAN, STUN4J_STF, AnsiColor.DEFAULT, padding.toString(),
        AnsiStyle.FAINT, version));
    out.println();
  }

}