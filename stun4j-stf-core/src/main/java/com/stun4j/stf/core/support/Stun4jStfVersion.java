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
package com.stun4j.stf.core.support;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.CodeSource;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;

/**
 * Class that exposes the Stun4j Stf version. Fetches the
 * {@link Name#IMPLEMENTATION_VERSION Implementation-Version} manifest attribute from the
 * jar file via {@link Package#getImplementationVersion()}, falling back to locating the
 * jar file that contains this class and reading the {@code Implementation-Version}
 * attribute from its manifest.
 * <p>
 * This class might not be able to determine the Stun4j Stf version in all environments.
 * Consider using a reflection-based check instead: For example, checking for the presence
 * of a specific Stun4j Stf method that you intend to call.
 * @author Drummond Dawson
 * @author Hendrig Sellik
 * @author Andy Wilkinson
 * @author Jay Meng
 *         <p>
 *         From spring-boot:2.6.5,with name changed
 * @since 1.3.0
 */
public final class Stun4jStfVersion {

  private Stun4jStfVersion() {
  }

  /**
   * Return the full version string of the present Stun4j Stf codebase, or {@code null}
   * if it cannot be determined.
   * @return the version of Stun4j Stf or {@code null}
   * @see Package#getImplementationVersion()
   */
  public static String getVersion() {
    return determineStun4jStfVersion();
  }

  private static String determineStun4jStfVersion() {
    String implementationVersion = Stun4jStfVersion.class.getPackage().getImplementationVersion();
    if (implementationVersion != null) {
      return implementationVersion;
    }
    CodeSource codeSource = Stun4jStfVersion.class.getProtectionDomain().getCodeSource();
    if (codeSource == null) {
      return null;
    }
    URL codeSourceLocation = codeSource.getLocation();
    try {
      URLConnection connection = codeSourceLocation.openConnection();
      if (connection instanceof JarURLConnection) {
        return getImplementationVersion(((JarURLConnection)connection).getJarFile());
      }
      try (JarFile jarFile = new JarFile(new File(codeSourceLocation.toURI()))) {
        return getImplementationVersion(jarFile);
      }
    } catch (Exception ex) {
      return null;
    }
  }

  private static String getImplementationVersion(JarFile jarFile) throws IOException {
    return jarFile.getManifest().getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
  }

}
