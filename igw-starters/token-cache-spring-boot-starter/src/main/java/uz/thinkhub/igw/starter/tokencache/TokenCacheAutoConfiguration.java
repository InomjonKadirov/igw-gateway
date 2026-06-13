package uz.thinkhub.igw.starter.tokencache;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnBean(StringRedisTemplate.class)
@EnableConfigurationProperties(IgwTokenCacheProperties.class)
public class TokenCacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public TokenCache redisTokenCache(StringRedisTemplate redis,
                                     IgwTokenCacheProperties properties) {
        return new RedisTokenCache(redis, properties);
    }

    /**
     * Registered only when the consuming service provides a {@link TokenRefresher}
     * bean (i.e. a service that knows how to fetch a fresh token from its
     * upstream provider).
     */
    @Bean
    @ConditionalOnBean(TokenRefresher.class)
    @ConditionalOnMissingBean
    public CachingTokenProvider cachingTokenProvider(TokenCache cache,
                                                   TokenRefresher refresher,
                                                   IgwTokenCacheProperties properties) {
        return new CachingTokenProvider(cache, refresher, properties);
    }
}
