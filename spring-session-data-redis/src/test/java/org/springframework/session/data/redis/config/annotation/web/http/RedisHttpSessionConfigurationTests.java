/*
 * Copyright 2014-2019 the original author or authors.
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

package org.springframework.session.data.redis.config.annotation.web.http;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.session.FlushMode;
import org.springframework.session.SaveMode;
import org.springframework.session.data.redis.RedisFlushMode;
import org.springframework.session.data.redis.RedisOperationsSessionRepository;
import org.springframework.session.data.redis.config.annotation.SpringSessionRedisConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RedisHttpSessionConfiguration}.
 *
 * @author Eddú Meléndez
 * @author Mark Paluch
 * @author Vedran Pavic
 */
class RedisHttpSessionConfigurationTests {

	private static final String CLEANUP_CRON_EXPRESSION = "0 0 * * * *";

	private AnnotationConfigApplicationContext context;

	@BeforeEach
	void before() {
		this.context = new AnnotationConfigApplicationContext();
	}

	@AfterEach
	void after() {
		if (this.context != null) {
			this.context.close();
		}
	}

	@Test
	void resolveValue() {
		registerAndRefresh(RedisConfig.class, CustomRedisHttpSessionConfiguration.class);
		RedisHttpSessionConfiguration configuration = this.context.getBean(RedisHttpSessionConfiguration.class);
		assertThat(ReflectionTestUtils.getField(configuration, "redisNamespace")).isEqualTo("myRedisNamespace");
	}

	@Test
	void resolveValueByPlaceholder() {
		this.context
				.setEnvironment(new MockEnvironment().withProperty("session.redis.namespace", "customRedisNamespace"));
		registerAndRefresh(RedisConfig.class, PropertySourceConfiguration.class,
				CustomRedisHttpSessionConfiguration2.class);
		RedisHttpSessionConfiguration configuration = this.context.getBean(RedisHttpSessionConfiguration.class);
		assertThat(ReflectionTestUtils.getField(configuration, "redisNamespace")).isEqualTo("customRedisNamespace");
	}

	@Test
	void customFlushImmediately() {
		registerAndRefresh(RedisConfig.class, CustomFlushImmediatelyConfiguration.class);
		RedisOperationsSessionRepository sessionRepository = this.context
				.getBean(RedisOperationsSessionRepository.class);
		assertThat(sessionRepository).isNotNull();
		assertThat(ReflectionTestUtils.getField(sessionRepository, "flushMode")).isEqualTo(FlushMode.IMMEDIATE);
	}

	@Test
	void customFlushImmediatelyLegacy() {
		registerAndRefresh(RedisConfig.class, CustomFlushImmediatelyLegacyConfiguration.class);
		RedisOperationsSessionRepository sessionRepository = this.context
				.getBean(RedisOperationsSessionRepository.class);
		assertThat(sessionRepository).isNotNull();
		assertThat(ReflectionTestUtils.getField(sessionRepository, "flushMode")).isEqualTo(FlushMode.IMMEDIATE);
	}

	@Test
	void setCustomFlushImmediately() {
		registerAndRefresh(RedisConfig.class, CustomFlushImmediatelySetConfiguration.class);
		RedisOperationsSessionRepository sessionRepository = this.context
				.getBean(RedisOperationsSessionRepository.class);
		assertThat(sessionRepository).isNotNull();
		assertThat(ReflectionTestUtils.getField(sessionRepository, "flushMode")).isEqualTo(FlushMode.IMMEDIATE);
	}

	@Test
	void setCustomFlushImmediatelyLegacy() {
		registerAndRefresh(RedisConfig.class, CustomFlushImmediatelySetLegacyConfiguration.class);
		RedisOperationsSessionRepository sessionRepository = this.context
				.getBean(RedisOperationsSessionRepository.class);
		assertThat(sessionRepository).isNotNull();
		assertThat(ReflectionTestUtils.getField(sessionRepository, "flushMode")).isEqualTo(FlushMode.IMMEDIATE);
	}

	@Test
	void customCleanupCronAnnotation() {
		registerAndRefresh(RedisConfig.class, CustomCleanupCronExpressionAnnotationConfiguration.class);

		RedisHttpSessionConfiguration configuration = this.context.getBean(RedisHttpSessionConfiguration.class);
		assertThat(configuration).isNotNull();
		assertThat(ReflectionTestUtils.getField(configuration, "cleanupCron")).isEqualTo(CLEANUP_CRON_EXPRESSION);
	}

	@Test
	void customCleanupCronSetter() {
		registerAndRefresh(RedisConfig.class, CustomCleanupCronExpressionSetterConfiguration.class);

		RedisHttpSessionConfiguration configuration = this.context.getBean(RedisHttpSessionConfiguration.class);
		assertThat(configuration).isNotNull();
		assertThat(ReflectionTestUtils.getField(configuration, "cleanupCron")).isEqualTo(CLEANUP_CRON_EXPRESSION);
	}

	@Test
	void customSaveModeAnnotation() {
		registerAndRefresh(RedisConfig.class, CustomSaveModeExpressionAnnotationConfiguration.class);
		assertThat(this.context.getBean(RedisOperationsSessionRepository.class)).hasFieldOrPropertyWithValue("saveMode",
				SaveMode.ALWAYS);
	}

	@Test
	void customSaveModeSetter() {
		registerAndRefresh(RedisConfig.class, CustomSaveModeExpressionSetterConfiguration.class);
		assertThat(this.context.getBean(RedisOperationsSessionRepository.class)).hasFieldOrPropertyWithValue("saveMode",
				SaveMode.ALWAYS);
	}

	@Test
	void qualifiedConnectionFactoryRedisConfig() {
		registerAndRefresh(RedisConfig.class, QualifiedConnectionFactoryRedisConfig.class);

		RedisOperationsSessionRepository repository = this.context.getBean(RedisOperationsSessionRepository.class);
		RedisConnectionFactory redisConnectionFactory = this.context.getBean("qualifiedRedisConnectionFactory",
				RedisConnectionFactory.class);
		assertThat(repository).isNotNull();
		assertThat(redisConnectionFactory).isNotNull();
		RedisOperations redisOperations = (RedisOperations) ReflectionTestUtils.getField(repository,
				"sessionRedisOperations");
		assertThat(redisOperations).isNotNull();
		assertThat(ReflectionTestUtils.getField(redisOperations, "connectionFactory"))
				.isEqualTo(redisConnectionFactory);
	}

	@Test
	void primaryConnectionFactoryRedisConfig() {
		registerAndRefresh(RedisConfig.class, PrimaryConnectionFactoryRedisConfig.class);

		RedisOperationsSessionRepository repository = this.context.getBean(RedisOperationsSessionRepository.class);
		RedisConnectionFactory redisConnectionFactory = this.context.getBean("primaryRedisConnectionFactory",
				RedisConnectionFactory.class);
		assertThat(repository).isNotNull();
		assertThat(redisConnectionFactory).isNotNull();
		RedisOperations redisOperations = (RedisOperations) ReflectionTestUtils.getField(repository,
				"sessionRedisOperations");
		assertThat(redisOperations).isNotNull();
		assertThat(ReflectionTestUtils.getField(redisOperations, "connectionFactory"))
				.isEqualTo(redisConnectionFactory);
	}

	@Test
	void qualifiedAndPrimaryConnectionFactoryRedisConfig() {
		registerAndRefresh(RedisConfig.class, QualifiedAndPrimaryConnectionFactoryRedisConfig.class);

		RedisOperationsSessionRepository repository = this.context.getBean(RedisOperationsSessionRepository.class);
		RedisConnectionFactory redisConnectionFactory = this.context.getBean("qualifiedRedisConnectionFactory",
				RedisConnectionFactory.class);
		assertThat(repository).isNotNull();
		assertThat(redisConnectionFactory).isNotNull();
		RedisOperations redisOperations = (RedisOperations) ReflectionTestUtils.getField(repository,
				"sessionRedisOperations");
		assertThat(redisOperations).isNotNull();
		assertThat(ReflectionTestUtils.getField(redisOperations, "connectionFactory"))
				.isEqualTo(redisConnectionFactory);
	}

	@Test
	void namedConnectionFactoryRedisConfig() {
		registerAndRefresh(RedisConfig.class, NamedConnectionFactoryRedisConfig.class);

		RedisOperationsSessionRepository repository = this.context.getBean(RedisOperationsSessionRepository.class);
		RedisConnectionFactory redisConnectionFactory = this.context.getBean("redisConnectionFactory",
				RedisConnectionFactory.class);
		assertThat(repository).isNotNull();
		assertThat(redisConnectionFactory).isNotNull();
		RedisOperations redisOperations = (RedisOperations) ReflectionTestUtils.getField(repository,
				"sessionRedisOperations");
		assertThat(redisOperations).isNotNull();
		assertThat(ReflectionTestUtils.getField(redisOperations, "connectionFactory"))
				.isEqualTo(redisConnectionFactory);
	}

	@Test
	void multipleConnectionFactoryRedisConfig() {
		assertThatExceptionOfType(BeanCreationException.class)
				.isThrownBy(() -> registerAndRefresh(RedisConfig.class, MultipleConnectionFactoryRedisConfig.class))
				.withMessageContaining("expected single matching bean but found 2");
	}

	@Test // gh-1252
	void customRedisMessageListenerContainerConfig() {
		registerAndRefresh(RedisConfig.class, CustomRedisMessageListenerContainerConfig.class);
		Map<String, RedisMessageListenerContainer> beans = this.context
				.getBeansOfType(RedisMessageListenerContainer.class);
		assertThat(beans).hasSize(2);
		assertThat(beans).containsKeys("springSessionRedisMessageListenerContainer", "redisMessageListenerContainer");
	}

	private void registerAndRefresh(Class<?>... annotatedClasses) {
		this.context.register(annotatedClasses);
		this.context.refresh();
	}

	private static RedisConnectionFactory mockRedisConnectionFactory() {
		RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
		RedisConnection connection = mock(RedisConnection.class);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.getConfig(anyString())).willReturn(new Properties());
		return connectionFactory;
	}

	@Configuration
	static class PropertySourceConfiguration {

		@Bean
		public PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
			return new PropertySourcesPlaceholderConfigurer();
		}

	}

	@Configuration
	static class RedisConfig {

		@Bean
		public RedisConnectionFactory defaultRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	static class CustomFlushImmediatelySetConfiguration extends RedisHttpSessionConfiguration {

		CustomFlushImmediatelySetConfiguration() {
			setFlushMode(FlushMode.IMMEDIATE);
		}

	}

	@Configuration
	static class CustomFlushImmediatelySetLegacyConfiguration extends RedisHttpSessionConfiguration {

		CustomFlushImmediatelySetLegacyConfiguration() {
			setRedisFlushMode(RedisFlushMode.IMMEDIATE);
		}

	}

	@Configuration
	@EnableRedisHttpSession(flushMode = FlushMode.IMMEDIATE)
	static class CustomFlushImmediatelyConfiguration {

	}

	@Configuration
	@EnableRedisHttpSession(redisFlushMode = RedisFlushMode.IMMEDIATE)
	static class CustomFlushImmediatelyLegacyConfiguration {

	}

	@EnableRedisHttpSession(cleanupCron = CLEANUP_CRON_EXPRESSION)
	static class CustomCleanupCronExpressionAnnotationConfiguration {

	}

	@Configuration
	static class CustomCleanupCronExpressionSetterConfiguration extends RedisHttpSessionConfiguration {

		CustomCleanupCronExpressionSetterConfiguration() {
			setCleanupCron(CLEANUP_CRON_EXPRESSION);
		}

	}

	@EnableRedisHttpSession(saveMode = SaveMode.ALWAYS)
	static class CustomSaveModeExpressionAnnotationConfiguration {

	}

	@Configuration
	static class CustomSaveModeExpressionSetterConfiguration extends RedisHttpSessionConfiguration {

		CustomSaveModeExpressionSetterConfiguration() {
			setSaveMode(SaveMode.ALWAYS);
		}

	}

	@Configuration
	@EnableRedisHttpSession
	static class QualifiedConnectionFactoryRedisConfig {

		@Bean
		@SpringSessionRedisConnectionFactory
		public RedisConnectionFactory qualifiedRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	@EnableRedisHttpSession
	static class PrimaryConnectionFactoryRedisConfig {

		@Bean
		@Primary
		public RedisConnectionFactory primaryRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	@EnableRedisHttpSession
	static class QualifiedAndPrimaryConnectionFactoryRedisConfig {

		@Bean
		@SpringSessionRedisConnectionFactory
		public RedisConnectionFactory qualifiedRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

		@Bean
		@Primary
		public RedisConnectionFactory primaryRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	@EnableRedisHttpSession
	static class NamedConnectionFactoryRedisConfig {

		@Bean
		public RedisConnectionFactory redisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	@EnableRedisHttpSession
	static class MultipleConnectionFactoryRedisConfig {

		@Bean
		public RedisConnectionFactory secondaryRedisConnectionFactory() {
			return mockRedisConnectionFactory();
		}

	}

	@Configuration
	@EnableRedisHttpSession(redisNamespace = "myRedisNamespace")
	static class CustomRedisHttpSessionConfiguration {

	}

	@Configuration
	@EnableRedisHttpSession(redisNamespace = "${session.redis.namespace}")
	static class CustomRedisHttpSessionConfiguration2 {

	}

	@Configuration
	@EnableRedisHttpSession
	static class CustomRedisMessageListenerContainerConfig {

		@Bean
		public RedisMessageListenerContainer redisMessageListenerContainer() {
			return new RedisMessageListenerContainer();
		}

	}

}
