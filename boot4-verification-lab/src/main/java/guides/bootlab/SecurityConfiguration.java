package guides.bootlab;

import jakarta.servlet.DispatcherType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
class SecurityConfiguration {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Value("${lab.allow-error-dispatch:true}") boolean allowErrorDispatch) throws Exception {
        return http.authorizeHttpRequests(auth -> {
                    // Teaching switch: false reproduces an error dispatch masking a CSRF 403 as 401.
                    if (allowErrorDispatch) auth.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll();
                    auth.anyRequest().authenticated();
                })
                .httpBasic(withDefaults()).build();
        // CSRF uses the framework default; it is not disabled for this browser-oriented fixture.
    }
}
