package fisa.oauth.authorizationserver.client;

import fisa.oauth.authorizationserver.client.dto.ClientRegisterRequest;
import fisa.oauth.authorizationserver.client.dto.ClientRegisterResponse;
import fisa.oauth.authorizationserver.client.ProviderClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class ProviderClientController {

    private final ProviderClientService providerClientService;

    public ProviderClientController(ProviderClientService providerClientService) {
        this.providerClientService = providerClientService;
    }

    @GetMapping("/provider")
    public String mainPage() {
        return "index";
    }

    @GetMapping("/developer/clients/new")
    public String registerPage(Model model) {
        model.addAttribute("request", new ClientRegisterRequest());
        return "client-register";
    }

    @PostMapping("/provider/clients")
    public String register(
            @RequestParam("clientName") String clientName,
            @RequestParam("redirectUris") String redirectUris,
            @RequestParam(value = "scopes", required = false) List<String> scopes,
            Model model
    ) {
        ClientRegisterRequest request = new ClientRegisterRequest();
        request.setClientName(clientName);
        request.setRedirectUris(List.of(redirectUris));
        request.setScopes(scopes != null && !scopes.isEmpty() ? scopes : null);

        ClientRegisterResponse response = providerClientService.register(request);
        model.addAttribute("result", response);

        return "client-register-result";
    }
}