package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.services.FederationService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;

@Controller
@CrossOrigin
public class ComponentController {

    private static Log log = LogFactory.getLog(ComponentController.class);

    private final FederationService federationService;

    @Autowired
    public ComponentController(FederationService federationService) {
        this.federationService = federationService;
    }

    @PostMapping("/administration/generic/joinedFederations")
    public ResponseEntity<?> joinedFederations(@RequestParam String platformId, @RequestHeader HttpHeaders httpHeaders) {

        log.debug("POST request on /administration/generic/joinedFederations for platformId = "
                + platformId);
        return federationService.joinedFederations(platformId, httpHeaders);
    }
}
