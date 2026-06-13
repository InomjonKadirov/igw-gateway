package uz.thinkhub.igw.starter.resilience;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(name = "org.apache.hc.client5.http.classic.HttpClient")
@EnableConfigurationProperties(IgwResilienceProperties.class)
public class ResilienceAutoConfiguration {

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public PoolingHttpClientConnectionManager igwConnectionManager(IgwResilienceProperties props) {
        return HttpClientConfig.createConnectionManager(props);
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public HttpClient igwHttpClient(IgwResilienceProperties props,
                                    PoolingHttpClientConnectionManager connectionManager) {
        return HttpClientConfig.createHttpClient(props, connectionManager);
    }
}
