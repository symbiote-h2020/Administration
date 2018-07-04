package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.services.federation.FederationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/administration/generic")
@CrossOrigin
public class GenericController {

    private static Log log = LogFactory.getLog(GenericController.class);

    private final FederationService federationService;

    @Autowired
    public GenericController(FederationService federationService) {
        this.federationService = federationService;
    }

    @PostMapping("/joinedFederations")
    public ResponseEntity<?> joinedFederations(@RequestParam String platformId, @RequestHeader HttpHeaders httpHeaders) {

        log.debug("POST request on /administration/generic/joinedFederations for platformId = "
                + platformId);
        return federationService.joinedFederations(platformId, httpHeaders);
    }
}
