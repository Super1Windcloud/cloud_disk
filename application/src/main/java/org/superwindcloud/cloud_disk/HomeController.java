package org.superwindcloud.cloud_disk;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private final ObjectProvider<ClientRegistrationRepository> repoProvider;

  public HomeController(ObjectProvider<ClientRegistrationRepository> repoProvider) {
    this.repoProvider = repoProvider;
  }

  @GetMapping({"/", "/index"})
  public String index(Model model) {
    model.addAttribute("googleEnabled", isGoogleConfigured());
    return "index";
  }

  private boolean isGoogleConfigured() {
    ClientRegistrationRepository repo = repoProvider.getIfAvailable();
    if (repo instanceof Iterable<?> iterable) {
      for (Object entry : iterable) {
        if (entry instanceof ClientRegistration registration
            && "google".equals(registration.getRegistrationId())) {
          return true;
        }
      }
    }
    return false;
  }
}
