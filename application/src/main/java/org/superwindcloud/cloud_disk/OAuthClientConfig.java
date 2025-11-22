package org.superwindcloud.cloud_disk;

import java.util.Arrays;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

@Configuration
public class OAuthClientConfig {

    @Bean
    @ConditionalOnMissingBean(ClientRegistrationRepository.class)
    ClientRegistrationRepository clientRegistrationRepository(Environment env) {
        ClientRegistration google = buildGoogleRegistration(env);
        return google != null
                ? new InMemoryClientRegistrationRepository(google)
                : new InMemoryClientRegistrationRepository();
    }

    private ClientRegistration buildGoogleRegistration(Environment env) {
        String clientId =
                firstNonNull(
                        env.getProperty("spring.security.oauth2.client.registration.google.client-id"),
                        env.getProperty(
                                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID"));
        String clientSecret =
                firstNonNull(
                        env.getProperty(
                                "spring.security.oauth2.client.registration.google.client-secret"),
                        env.getProperty(
                                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET"));

        if (clientId == null || clientSecret == null) {
            return null;
        }

        String redirectUri =
                firstNonNull(
                        env.getProperty(
                                "spring.security.oauth2.client.registration.google.redirect-uri"),
                        env.getProperty(
                                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_REDIRECT_URI"),
                        "{baseUrl}/login/oauth2/code/google");
        String issuer =
                firstNonNull(
                        env.getProperty("spring.security.oauth2.client.provider.google.issuer-uri"),
                        env.getProperty(
                                "SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_GOOGLE_ISSUER_URI"),
                        "https://accounts.google.com");
        String scopeRaw =
                firstNonNull(
                        env.getProperty("spring.security.oauth2.client.registration.google.scope"),
                        env.getProperty(
                                "SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_SCOPE"),
                        "openid,profile,email");
        List<String> scopes =
                Arrays.stream(scopeRaw.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();

        return ClientRegistration.withRegistrationId("google")
                .clientId(clientId)
                .clientSecret(clientSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(redirectUri)
                .scope(scopes)
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .issuerUri(issuer)
                .userInfoUri("https://www.googleapis.com/oauth2/v3/userinfo")
                .userNameAttributeName("sub")
                .clientName("Google")
                .build();
    }

    @SafeVarargs
    private <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
