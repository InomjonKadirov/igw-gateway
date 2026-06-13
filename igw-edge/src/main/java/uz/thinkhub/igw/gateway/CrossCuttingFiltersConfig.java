package uz.thinkhub.igw.gateway;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Registers the cross-cutting servlet filters with explicit ordering so
 * they run <em>after</em> the Spring Security filter chain (which has
 * order {@link Ordered#HIGHEST_PRECEDENCE} + 50 = -50 by default) and in
 * the documented order:
 *
 * <ol>
 *   <li>{@link CorrelationIdFilter} (order 10) — tags MDC.</li>
 *   <li>Spring Security JWT filter chain (auto, order ~-50).</li>
 *   <li>{@link IpCheckFilter} (order 30) — needs authenticated user id.</li>
 *   <li>{@link RateLimitFilter} (order 40) — needs authenticated user id.</li>
 * </ol>
 *
 * <p>The IP and rate-limit filters are read by
 * {@link SecurityContextHolder}, so they must run <em>after</em> Spring
 * Security has authenticated the request.
 */
@Configuration
public class CrossCuttingFiltersConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(
            CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(10);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<IpCheckFilter> ipCheckFilterRegistration(
            IpCheckFilter filter) {
        FilterRegistrationBean<IpCheckFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(30);
        reg.addUrlPatterns("/*");
        return reg;
    }

    @Bean
    public FilterRegistrationBean<RateLimitFilter> rateLimitFilterRegistration(
            RateLimitFilter filter) {
        FilterRegistrationBean<RateLimitFilter> reg = new FilterRegistrationBean<>(filter);
        reg.setOrder(40);
        reg.addUrlPatterns("/*");
        return reg;
    }
}
