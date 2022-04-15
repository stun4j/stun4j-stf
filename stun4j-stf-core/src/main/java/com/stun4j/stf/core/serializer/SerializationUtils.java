package com.stun4j.stf.core.serializer;

/**
 * Utility class with various serialization-related methods.
 * @author Costin Leau
 */
public abstract class SerializationUtils {

  static final byte[] EMPTY_ARRAY = new byte[0];

  static boolean isEmpty(byte[] data) {// @Nullable byte[] data
    return (data == null || data.length == 0);
  }
}