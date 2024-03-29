/*
 * Copyright 2002-2022 the original author or authors.
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
package com.stun4j.stf.core.utils;

import java.io.Closeable;
import java.io.Externalizable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Miscellaneous {@code java.lang.Class} utility methods. Mainly for internal use within the
 * framework.
 * 
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Sam Brannen
 * @author Jay Meng
 *         <p>
 *         From spring-core:5.3.24,changes listed below
 *         <ul>
 *         <li>Disable all Spring dependency, disable @Nullable, use Asserts#notNull instead</li>
 *         <li>Removed '@see' on class-level javadoc</li>
 *         <li>Removed methods not used</li>
 *         </ul>
 * @since 1.1
 */
public abstract class ClassUtils {

  /** Suffix for array class names: {@code "[]"}. */
  public static final String ARRAY_SUFFIX = "[]";

  /** Prefix for internal array class names: {@code "["}. */
  private static final String INTERNAL_ARRAY_PREFIX = "[";

  /** Prefix for internal non-primitive array class names: {@code "[L"}. */
  private static final String NON_PRIMITIVE_ARRAY_PREFIX = "[L";

  /** The package separator character: {@code '.'}. */
  private static final char PACKAGE_SEPARATOR = '.';

  /** The nested class separator character: {@code '$'}. */
  private static final char NESTED_CLASS_SEPARATOR = '$';

  /** The CGLIB class separator: {@code "$$"}. */
  public static final String CGLIB_CLASS_SEPARATOR = "$$";

  /** The ".class" file suffix. */
  public static final String CLASS_FILE_SUFFIX = ".class";

  /**
   * Map with primitive wrapper type as key and corresponding primitive type as value, for example:
   * Integer.class -> int.class.
   */
  private static final Map<Class<?>, Class<?>> primitiveWrapperTypeMap = new IdentityHashMap<>(9);

  /**
   * Map with primitive type name as key and corresponding primitive type as value, for example: "int"
   * -> "int.class".
   */
  private static final Map<String, Class<?>> primitiveTypeNameMap = new HashMap<>(32);

  /**
   * Map with common Java language class name as key and corresponding Class as value. Primarily for
   * efficient deserialization of remote invocations.
   */
  private static final Map<String, Class<?>> commonClassCache = new HashMap<>(64);

  static {
    primitiveWrapperTypeMap.put(Boolean.class, boolean.class);
    primitiveWrapperTypeMap.put(Byte.class, byte.class);
    primitiveWrapperTypeMap.put(Character.class, char.class);
    primitiveWrapperTypeMap.put(Double.class, double.class);
    primitiveWrapperTypeMap.put(Float.class, float.class);
    primitiveWrapperTypeMap.put(Integer.class, int.class);
    primitiveWrapperTypeMap.put(Long.class, long.class);
    primitiveWrapperTypeMap.put(Short.class, short.class);
    primitiveWrapperTypeMap.put(Void.class, void.class);

    Set<Class<?>> primitiveTypes = new HashSet<>(32);
    primitiveTypes.addAll(primitiveWrapperTypeMap.values());
    Collections.addAll(primitiveTypes, boolean[].class, byte[].class, char[].class, double[].class, float[].class,
        int[].class, long[].class, short[].class);
    for (Class<?> primitiveType : primitiveTypes) {
      primitiveTypeNameMap.put(primitiveType.getName(), primitiveType);
    }

    registerCommonClasses(Boolean[].class, Byte[].class, Character[].class, Double[].class, Float[].class,
        Integer[].class, Long[].class, Short[].class);
    registerCommonClasses(Number.class, Number[].class, String.class, String[].class, Class.class, Class[].class,
        Object.class, Object[].class);
    registerCommonClasses(Throwable.class, Exception.class, RuntimeException.class, Error.class,
        StackTraceElement.class, StackTraceElement[].class);
    registerCommonClasses(Enum.class, Iterable.class, Iterator.class, Enumeration.class, Collection.class, List.class,
        Set.class, Map.class, Map.Entry.class, Optional.class);

    Class<?>[] javaLanguageInterfaceArray = {Serializable.class, Externalizable.class, Closeable.class,
        AutoCloseable.class, Cloneable.class, Comparable.class};
    registerCommonClasses(javaLanguageInterfaceArray);
  }

  /**
   * Register the given common classes with the ClassUtils cache.
   */
  private static void registerCommonClasses(Class<?>... commonClasses) {
    for (Class<?> clazz : commonClasses) {
      commonClassCache.put(clazz.getName(), clazz);
    }
  }

  /**
   * Replacement for {@code Class.forName()} that also returns Class instances for primitives (e.g.
   * "int") and array class names (e.g. "String[]"). Furthermore, it is also capable of resolving
   * nested class names in Java source style (e.g. "java.lang.Thread.State" instead of
   * "java.lang.Thread$State").
   * 
   * @param name        the name of the Class
   * @param classLoader the class loader to use (may be {@code null}, which indicates the default
   *                    class loader)
   * @return a class instance for the supplied name
   * @throws ClassNotFoundException if the class was not found
   * @throws LinkageError           if the class file could not be loaded
   * @see Class#forName(String, boolean, ClassLoader)
   */
  public static Class<?> forName(String name, ClassLoader classLoader)// @Nullable ClassLoader
      throws ClassNotFoundException, LinkageError {

    Asserts.notNull(name, "Name must not be null");

    Class<?> clazz = resolvePrimitiveClassName(name);
    if (clazz == null) {
      clazz = commonClassCache.get(name);
    }
    if (clazz != null) {
      return clazz;
    }

    // "java.lang.String[]" style arrays
    if (name.endsWith(ARRAY_SUFFIX)) {
      String elementClassName = name.substring(0, name.length() - ARRAY_SUFFIX.length());
      Class<?> elementClass = forName(elementClassName, classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[Ljava.lang.String;" style arrays
    if (name.startsWith(NON_PRIMITIVE_ARRAY_PREFIX) && name.endsWith(";")) {
      String elementName = name.substring(NON_PRIMITIVE_ARRAY_PREFIX.length(), name.length() - 1);
      Class<?> elementClass = forName(elementName, classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    // "[[I" or "[[Ljava.lang.String;" style arrays
    if (name.startsWith(INTERNAL_ARRAY_PREFIX)) {
      String elementName = name.substring(INTERNAL_ARRAY_PREFIX.length());
      Class<?> elementClass = forName(elementName, classLoader);
      return Array.newInstance(elementClass, 0).getClass();
    }

    ClassLoader clToUse = classLoader;
    if (clToUse == null) {
      clToUse = getDefaultClassLoader();
    }
    try {
      return Class.forName(name, false, clToUse);
    } catch (ClassNotFoundException ex) {
      int lastDotIndex = name.lastIndexOf(PACKAGE_SEPARATOR);
      if (lastDotIndex != -1) {
        String nestedClassName = name.substring(0, lastDotIndex) + NESTED_CLASS_SEPARATOR
            + name.substring(lastDotIndex + 1);
        try {
          return Class.forName(nestedClassName, false, clToUse);
        } catch (ClassNotFoundException ex2) {
          // Swallow - let original exception get through
        }
      }
      throw ex;
    }
  }

  /**
   * Resolve the given class name into a Class instance. Supports primitives (like "int") and array
   * class names (like "String[]").
   * <p>
   * This is effectively equivalent to the {@code forName} method with the same arguments, with the
   * only difference being the exceptions thrown in case of class loading failure.
   * 
   * @param className   the name of the Class
   * @param classLoader the class loader to use (may be {@code null}, which indicates the default
   *                    class loader)
   * @return a class instance for the supplied name
   * @throws IllegalArgumentException if the class name was not resolvable (that is, the class could
   *                                  not be found or the class file could not be loaded)
   * @throws IllegalStateException    if the corresponding class is resolvable but there was a
   *                                  readability mismatch in the inheritance hierarchy of the class
   *                                  (typically a missing dependency declaration in a Jigsaw module
   *                                  definition for a superclass or interface implemented by the
   *                                  class to be loaded here)
   * @see #forName(String, ClassLoader)
   */
  public static Class<?> resolveClassName(String className, ClassLoader classLoader)// @Nullable
      throws IllegalArgumentException {

    try {
      return forName(className, classLoader);
    } catch (IllegalAccessError err) {
      throw new IllegalStateException(
          "Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
    } catch (LinkageError err) {
      throw new IllegalArgumentException("Unresolvable class definition for class [" + className + "]", err);
    } catch (ClassNotFoundException ex) {
      throw new IllegalArgumentException("Could not find class [" + className + "]", ex);
    }
  }

  /**
   * Check if the given class represents a primitive (i.e. boolean, byte, char, short, int, long,
   * float, or double), {@code void}, or a wrapper for those types (i.e. Boolean, Byte, Character,
   * Short, Integer, Long, Float, Double, or Void).
   * 
   * @param clazz the class to check
   * @return {@code true} if the given class represents a primitive, void, or a wrapper class
   */
  public static boolean isPrimitiveOrWrapper(Class<?> clazz) {
    Asserts.notNull(clazz, "Class must not be null");
    return (clazz.isPrimitive() || isPrimitiveWrapper(clazz));
  }

  /**
   * Check if the given class represents a primitive wrapper, i.e. Boolean, Byte, Character, Short,
   * Integer, Long, Float, Double, or Void.
   * 
   * @param clazz the class to check
   * @return whether the given class is a primitive wrapper class
   */
  public static boolean isPrimitiveWrapper(Class<?> clazz) {
    Asserts.notNull(clazz, "Class must not be null");
    return primitiveWrapperTypeMap.containsKey(clazz);
  }

  /**
   * Resolve the given class name as primitive class, if appropriate, according to the JVM's naming
   * rules for primitive classes.
   * <p>
   * Also supports the JVM's internal class names for primitive arrays. Does <i>not</i> support the
   * "[]" suffix notation for primitive arrays; this is only supported by
   * {@link #forName(String, ClassLoader)}.
   * 
   * @param name the name of the potentially primitive class
   * @return the primitive class, or {@code null} if the name does not denote a primitive class or
   *         primitive array class
   */
  // @Nullable
  public static Class<?> resolvePrimitiveClassName(String name) {// @Nullable
    Class<?> result = null;
    // Most class names will be quite long, considering that they
    // SHOULD sit in a package, so a length check is worthwhile.
    if (name != null && name.length() <= 7) {
      // Could be a primitive - likely.
      result = primitiveTypeNameMap.get(name);
    }
    return result;
  }

  /**
   * Determine whether the {@link Class} identified by the supplied name is present and can be loaded.
   * Will return {@code false} if either the class or one of its dependencies is not present or cannot
   * be loaded.
   * 
   * @param className   the name of the class to check
   * @param classLoader the class loader to use (may be {@code null} which indicates the default class
   *                    loader)
   * @return whether the specified class is present (including all of its superclasses and interfaces)
   * @throws IllegalStateException if the corresponding class is resolvable but there was a
   *                               readability mismatch in the inheritance hierarchy of the class
   *                               (typically a missing dependency declaration in a Jigsaw module
   *                               definition for a superclass or interface implemented by the class
   *                               to be checked here)
   */
  public static boolean isPresent(String className, ClassLoader classLoader) {// @Nullable ClassLoader
    try {
      forName(className, classLoader);
      return true;
    } catch (IllegalAccessError err) {
      throw new IllegalStateException(
          "Readability mismatch in inheritance hierarchy of class [" + className + "]: " + err.getMessage(), err);
    } catch (Throwable ex) {
      // Typically ClassNotFoundException or NoClassDefFoundError...
      return false;
    }
  }

  /**
   * Return the default ClassLoader to use: typically the thread context ClassLoader, if available;
   * the ClassLoader that loaded the ClassUtils class will be used as fallback.
   * <p>
   * Call this method if you intend to use the thread context ClassLoader in a scenario where you
   * clearly prefer a non-null ClassLoader reference: for example, for class path resource loading
   * (but not necessarily for {@code Class.forName}, which accepts a {@code null} ClassLoader
   * reference as well).
   * 
   * @return the default ClassLoader (only {@code null} if even the system ClassLoader isn't
   *         accessible)
   * @see Thread#getContextClassLoader()
   * @see ClassLoader#getSystemClassLoader()
   */
  // @Nullable
  public static ClassLoader getDefaultClassLoader() {
    ClassLoader cl = null;
    try {
      cl = Thread.currentThread().getContextClassLoader();
    } catch (Throwable ex) {
      // Cannot access thread context ClassLoader - falling back...
    }
    if (cl == null) {
      // No thread context class loader -> use class loader of this class.
      cl = ClassUtils.class.getClassLoader();
      if (cl == null) {
        // getClassLoader() returning null indicates the bootstrap ClassLoader
        try {
          cl = ClassLoader.getSystemClassLoader();
        } catch (Throwable ex) {
          // Cannot access system ClassLoader - oh well, maybe the caller can live with null...
        }
      }
    }
    return cl;
  }
}