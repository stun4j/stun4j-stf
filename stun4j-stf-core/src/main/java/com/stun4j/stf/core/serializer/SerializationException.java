package com.stun4j.stf.core.serializer;
public class SerializationException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public SerializationException(String message, Throwable cause) {
    super(message, cause);
  }

  public SerializationException(String message) {
    super(message);
  }

}