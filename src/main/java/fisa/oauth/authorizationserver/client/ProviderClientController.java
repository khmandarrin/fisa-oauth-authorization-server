package fisa.oauth.authorizationserver.client;

import fisa.oauth.authorizationserver.client.dto.ClientRegisterRequest;
import fisa.oauth.authorizationserver.client.dto.ClientRegisterResponse;
import fisa.oauth.authorizationserver.client.ProviderClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class ProviderClientController {

    private final ProviderClientService providerClientService;

    public ProviderClientController(ProviderClientService providerClientService) {
        this.providerClientService = providerClientService;
    }

    @GetMapping("/developer/clients/new")
    public String registerPage(Model model) {
        model.addAttribute("request", new ClientRegisterRequest());
        return "client-register";
    }

    @PostMapping("/provider/clients")
    public String register(
            @ModelAttribute("request") ClientRegisterRequest request,
            Model model
    ) {

        ClientRegisterResponse response = providerClientService.register(request);
        model.addAttribute("result", response);

        return "client-register-result";
    }
}