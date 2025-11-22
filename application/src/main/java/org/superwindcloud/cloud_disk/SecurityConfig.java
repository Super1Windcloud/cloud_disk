package org.superwindcloud.cloud_disk;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, ObjectProvider<ClientRegistrationRepository> repoProvider) {
    ClientRegistrationRepository repo = repoProvider.getIfAvailable();
    boolean oauthConfigured = repo != null && repo.findByRegistrationId("google") != null;

    if (oauthConfigured) {
      http.authorizeHttpRequests(
              auth ->
                  auth.requestMatchers(
                          "/css/**",
                          "/js/**",
                          "/images/**",
                          "/",
                          "/index",
                          "/s/**",
                          "/api/files/browse",
                          "/error")
                      .permitAll()
                      .anyRequest()
                      .authenticated())
          .exceptionHandling(
              exceptions ->
                  exceptions.authenticationEntryPoint(
                      new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
          .oauth2Login(oauth -> oauth.defaultSuccessUrl("/", true))
          .logout(logout -> logout.logoutUrl("/logout").logoutSuccessUrl("/").permitAll())
          .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**", "/logout"));
    } else {
      // Fallback: allow all if OAuth is not configured, so the app can start without client-id
      http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
          .csrf(AbstractHttpConfigurer::disable);
    }

    return http.build();
  }
}
