/*
 * Copyright 2015-2022 the original author or authors.
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
package com.stun4j.stf.core.serializer;

import static com.stun4j.stf.core.utils.Asserts.notNull;
import static com.stun4j.stf.core.utils.Asserts.requireNonNull;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.KotlinDetector;
import org.springframework.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

//import org.springframework.cache.support.NullValue;
//import org.springframework.data.redis.serializer.RedisSerializer;
//import org.springframework.util.Assert;
//import org.springframework.util.StringUtils;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.SerializerFactory;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.stun4j.stf.core.support.NullValue;

/**
 * Generic Jackson 2-based {@link Serializer} that maps {@link Object objects} to JSON using dynamic typing.
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Mao Shuai
 * @author Jay Meng
 *         <p>
 *         From spring-data-redis:2.7.6,changes listed below
 *         <ul>
 *         <li>Disable all Spring dependency,use Asserts#notNull,commons-lang3 instead</li>
 *         <li>Use Serializer instead of RedisSerializer</li>
 *         <li>Change {@code serialVersionUID}</li>
 *         <li>Enhance Serialization/Deserialization Feature</li>
 *         </ul>
 */
public class GenericJackson2JsonSerializer implements Serializer {

  private final ObjectMapper mapper;

  /**
   * Creates {@link GenericJackson2JsonSerializer} and configures {@link ObjectMapper} for default typing.
   */
  public GenericJackson2JsonSerializer() {
    this((String)null);
  }

  /**
   * Creates {@link GenericJackson2JsonSerializer} and configures {@link ObjectMapper} for default typing using the
   * given {@literal name}. In case of an {@literal empty} or {@literal null} String the default
   * {@link JsonTypeInfo.Id#CLASS} will be used.
   * @param classPropertyTypeName Name of the JSON property holding type information. Can be {@literal null}.
   * @see ObjectMapper#activateDefaultTypingAsProperty(PolymorphicTypeValidator, DefaultTyping, String)
   * @see ObjectMapper#activateDefaultTyping(PolymorphicTypeValidator, DefaultTyping, As)
   */
  public GenericJackson2JsonSerializer(String classPropertyTypeName) {
    //@formatter:off
    this(JsonMapper.builder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
        .configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES.mappedFeature(), true)
        .configure(JsonReadFeature.ALLOW_SINGLE_QUOTES.mappedFeature(), true)
        .configure(JsonReadFeature.ALLOW_LEADING_ZEROS_FOR_NUMBERS.mappedFeature(), true)
        .configure(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature(), true)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .propertyNamingStrategy(PropertyNamingStrategies.LOWER_CAMEL_CASE)
        .enable(MapperFeature.USE_STD_BEAN_NAMING)
        .build());
    //@formatter:on

    // simply setting {@code mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)} does not help here since we need
    // the type hint embedded for deserialization using the default typing feature.
    registerNullValueSerializer(mapper, classPropertyTypeName);

    // mj:upgrade from 2.6.4:modify->
    // if (StringUtils.isNotBlank(classPropertyTypeName)) {
    // mapper.activateDefaultTypingAsProperty(mapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL,
    // classPropertyTypeName);
    // } else {
    // mapper.activateDefaultTyping(mapper.getPolymorphicTypeValidator(), DefaultTyping.NON_FINAL, As.PROPERTY);
    // }

    StdTypeResolverBuilder typer = new TypeResolverBuilder(DefaultTyping.EVERYTHING,
        mapper.getPolymorphicTypeValidator());
    typer = typer.init(JsonTypeInfo.Id.CLASS, null);
    typer = typer.inclusion(JsonTypeInfo.As.PROPERTY);

    if (StringUtils.isNotBlank(classPropertyTypeName)) {
      typer = typer.typeProperty(classPropertyTypeName);
    }
    mapper.setDefaultTyping(typer);
    // <-
  }

  /**
   * Setting a custom-configured {@link ObjectMapper} is one way to take further control of the JSON serialization
   * process. For example, an extended {@link SerializerFactory} can be configured that provides custom serializers for
   * specific types.
   * @param mapper can't be {@literal null}.
   */
  public GenericJackson2JsonSerializer(ObjectMapper mapper) {
    this.mapper = requireNonNull(mapper, "ObjectMapper can't be null!");
  }

  /**
   * Register {@link NullValueSerializer} in the given {@link ObjectMapper} with an optional
   * {@code classPropertyTypeName}. This method should be called by code that customizes
   * {@link GenericJackson2JsonSerializer} by providing an external {@link ObjectMapper}.
   * @param objectMapper the object mapper to customize.
   * @param classPropertyTypeName name of the type property. Defaults to {@code @class} if {@literal null}/empty.
   * @since 2.2
   */
  public static void registerNullValueSerializer(ObjectMapper objectMapper, String classPropertyTypeName) {

    // simply setting {@code mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)} does not help here since we need
    // the type hint embedded for deserialization using the default typing feature.
    objectMapper.registerModule(new SimpleModule().addSerializer(new NullValueSerializer(classPropertyTypeName)));
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.redis.serializer.RedisSerializer#serialize(java.lang.Object)
   */
  @Override
  public byte[] serialize(Object source) throws SerializationException {

    if (source == null) {
      return SerializationUtils.EMPTY_ARRAY;
    }

    try {
      return mapper.writeValueAsBytes(source);
    } catch (JsonProcessingException e) {
      throw new SerializationException("Could not write JSON: " + e.getMessage(), e);
    }
  }

  /*
   * (non-Javadoc)
   * @see org.springframework.data.redis.serializer.RedisSerializer#deserialize(byte[])
   */
  @Override
  public Object deserialize(byte[] source) throws SerializationException {
    return deserialize(source, Object.class);
  }

  /**
   * @param source can be {@literal null}.
   * @param type can't be {@literal null}.
   * @return {@literal null} for empty source.
   * @throws SerializationException
   */
  public <T> T deserialize(byte[] source, Class<T> type) throws SerializationException {
    notNull(type,
        "Deserialization type can't be null! Please provide Object.class to make use of Jackson2 default typing.");

    if (SerializationUtils.isEmpty(source)) {
      return null;
    }

    try {
      return mapper.readValue(source, type);
    } catch (Exception ex) {
      throw new SerializationException("Could not read JSON: " + ex.getMessage(), ex);
    }
  }

  /**
   * {@link StdSerializer} adding class information required by default typing. This allows de-/serialization of
   * {@link NullValue}.
   * @author Christoph Strobl
   * @since 1.8
   */
  private static class NullValueSerializer extends StdSerializer<NullValue> {
    private static final long serialVersionUID = 1L;
    private final String classIdentifier;

    /**
     * @param classIdentifier can be {@literal null} and will be defaulted to {@code @class}.
     */
    NullValueSerializer(String classIdentifier) {// @Nullable String classIdentifier

      super(NullValue.class);
      this.classIdentifier = StringUtils.isNotBlank(classIdentifier) ? classIdentifier : "@class";
    }

    /*
     * (non-Javadoc)
     * @see com.fasterxml.jackson.databind.ser.std.StdSerializer#serialize(java.lang.Object,
     * com.fasterxml.jackson.core.JsonGenerator, com.fasterxml.jackson.databind.SerializerProvider)
     */
    @Override
    public void serialize(NullValue value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
      jgen.writeStartObject();
      jgen.writeStringField(classIdentifier, NullValue.class.getName());
      jgen.writeEndObject();
    }

    // mj:upgrade from 2.6.4:add->
    @Override
    public void serializeWithType(NullValue value, JsonGenerator gen, SerializerProvider serializers,
        TypeSerializer typeSer) throws IOException {
      serialize(value, gen, serializers);
    }
    // <-
  }

  // mj:upgrade from 2.6.4:add->
  /**
   * Custom {@link StdTypeResolverBuilder} that considers typing for non-primitive types. Primitives, their wrappers and
   * primitive arrays do not require type hints. The default {@code DefaultTyping#EVERYTHING} typing does not satisfy
   * those requirements.
   * @author Mark Paluch
   * @since 2.7.2
   */
  private static class TypeResolverBuilder extends ObjectMapper.DefaultTypeResolverBuilder {
    private static final long serialVersionUID = 1L;

    public TypeResolverBuilder(DefaultTyping t, PolymorphicTypeValidator ptv) {
      super(t, ptv);
    }

    @Override
    public ObjectMapper.DefaultTypeResolverBuilder withDefaultImpl(Class<?> defaultImpl) {
      return this;
    }

    /**
     * Method called to check if the default type handler should be used for given type. Note: "natural types" (String,
     * Boolean, Integer, Double) will never use typing; that is both due to them being concrete and final, and since
     * actual serializers and deserializers will also ignore any attempts to enforce typing.
     */
    public boolean useForType(JavaType t) {

      if (t.isJavaLangObject()) {
        return true;
      }

      t = resolveArrayOrWrapper(t);

      if (t.isEnumType() || ClassUtils.isPrimitiveOrWrapper(t.getRawClass())) {
        return false;
      }

      if (t.isFinal() && !KotlinDetector.isKotlinType(t.getRawClass())
          && t.getRawClass().getPackage().getName().startsWith("java")) {
        return false;
      }

      // [databind#88] Should not apply to JSON tree models:
      return !TreeNode.class.isAssignableFrom(t.getRawClass());
    }

    private JavaType resolveArrayOrWrapper(JavaType type) {

      while (type.isArrayType()) {
        type = type.getContentType();
        if (type.isReferenceType()) {
          type = resolveArrayOrWrapper(type);
        }
      }

      while (type.isReferenceType()) {
        type = type.getReferencedType();
        if (type.isArrayType()) {
          type = resolveArrayOrWrapper(type);
        }
      }

      return type;
    }
  }
  // <-
}