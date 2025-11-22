package org.superwindcloud.cloud_disk.controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class UserController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> me(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Map<String, Object> profile = new HashMap<>();

        if (principal instanceof OidcUser oidcUser) {
            profile.put("name", oidcUser.getFullName());
            profile.put("email", oidcUser.getEmail());
            profile.put("picture", oidcUser.getPicture());
            profile.put("issuer", oidcUser.getIssuer());
            return ResponseEntity.ok(profile);
        }

        if (principal instanceof OAuth2User oauth2User) {
            profile.put("name", oauth2User.getAttribute("name"));
            profile.put("email", oauth2User.getAttribute("email"));
            profile.put("picture", oauth2User.getAttribute("picture"));
            profile.put("issuer", oauth2User.getAttribute("iss"));
            return ResponseEntity.ok(profile);
        }

        profile.put("name", principal.getName());
        return ResponseEntity.ok(profile);
    }
}
