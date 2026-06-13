package uz.thinkhub.igw.starter.tokencache;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

class TokenCacheAutoConfigurationTest {

    @Test
    void registersCacheBeanWhenStringRedisTemplateIsPresent() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TokenCacheAutoConfiguration.class,
                        org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class))
                .withUserConfiguration(TestRedisConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenCache.class);
                    assertThat(context.getBean(TokenCache.class)).isInstanceOf(RedisTokenCache.class);
                });
    }

    @Test
    void registersCachingProviderWhenTokenRefresherIsPresent() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TokenCacheAutoConfiguration.class,
                        org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class))
                .withUserConfiguration(TestRedisConfig.class, TestRefresherConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(CachingTokenProvider.class);
                });
    }

    @Test
    void doesNotRegisterCachingProviderWhenTokenRefresherMissing() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TokenCacheAutoConfiguration.class,
                        org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration.class))
                .withUserConfiguration(TestRedisConfig.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(TokenCache.class);
                    assertThat(context).doesNotHaveBean(CachingTokenProvider.class);
                });
    }

    @Test
    void doesNotRegisterCacheWhenStringRedisTemplateMissing() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TokenCacheAutoConfiguration.class))
                .run(context -> {
                    assertThat(context).doesNotHaveBean(TokenCache.class);
                });
    }

    @Configuration
    static class TestRedisConfig {
        @Bean
        public RedisConnectionFactory redisConnectionFactory() {
            // Use a mock that doesn't actually connect; we only test bean wiring.
            return new LettuceConnectionFactory(new org.springframework.data.redis.connection.RedisStandaloneConfiguration("localhost", 6379));
        }

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
            return new StringRedisTemplate(factory);
        }
    }

    @Configuration
    static class TestRefresherConfig {
        @Bean
        public TokenRefresher tokenRefresher() {
            return key -> new Token("test", java.time.Instant.now().plus(java.time.Duration.ofMinutes(30)));
        }
    }
}
