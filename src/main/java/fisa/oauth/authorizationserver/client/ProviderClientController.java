//package fisa.oauth.authorizationserver.client;
//
//import fisa.oauth.authorizationserver.client.dto.ClientRegisterRequest;
//import fisa.oauth.authorizationserver.client.dto.ClientRegisterResponse;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/provider/clients")
//public class ProviderClientController {
//
//    private final ProviderClientService providerClientService;
//
//    public ProviderClientController(ProviderClientService providerClientService) {
//        this.providerClientService = providerClientService;
//    }
//
//    @PostMapping
//    // @PreAuthorize("hasRole('ADMIN')")
//    public ClientRegisterResponse register(@RequestBody ClientRegisterRequest request) {
//        return providerClientService.register(request);
//    }
//}