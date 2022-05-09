package com.stun4j.stf.core.support;

import com.google.common.eventbus.EventBus;

public enum StfEventBus {
  INSTANCE;

  private final EventBus bus;

  public static void registerHandler(Object... handlers) {
    for (Object handler : handlers) {
      INSTANCE.bus.register(handler);
    }
  }

  public static void post(Object event) {
    INSTANCE.bus.post(event);
  }

  {
    bus = new EventBus();
  }
}
