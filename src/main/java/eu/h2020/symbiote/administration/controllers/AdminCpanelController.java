package eu.h2020.symbiote.administration.controllers;

import eu.h2020.symbiote.administration.communication.rabbit.RabbitManager;
import eu.h2020.symbiote.administration.communication.rabbit.exceptions.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.InvitationRequest;
import eu.h2020.symbiote.administration.services.infomodel.InformationModelService;
import eu.h2020.symbiote.administration.services.federation.FederationService;
import eu.h2020.symbiote.core.cci.InformationModelRequest;
import eu.h2020.symbiote.core.cci.InformationModelResponse;
import eu.h2020.symbiote.core.internal.ClearDataRequest;
import eu.h2020.symbiote.core.internal.ClearDataResponse;
import eu.h2020.symbiote.model.mim.InformationModel;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.Principal;
import java.util.List;

/**
 * Spring controller for the User control panel, handles management views and form validation.
 *
 * @author Vasileios Glykantzis (ICOM)
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@Controller
@CrossOrigin
public class AdminCpanelController {
    private static Log log = LogFactory.getLog(AdminCpanelController.class);

    private final RabbitManager rabbitManager;
    private final FederationService federationService;
    private final InformationModelService informationModelService;

    @Autowired
    public AdminCpanelController(RabbitManager rabbitManager,
                                 InformationModelService informationModelService,
                                 FederationService federationService) {

        Assert.notNull(rabbitManager,"RabbitManager can not be null!");
        this.rabbitManager = rabbitManager;

        Assert.notNull(informationModelService,"InformationModelService can not be null!");
        this.informationModelService = informationModelService;

        Assert.notNull(federationService,"FederationService can not be null!");
        this.federationService = federationService;
    }

    /**
     * Gets the default view. If the user is a platform owner, tries to fetch their details.
     * Registry is first polled and, if the platform isn't activated there, AAM is polled for them.
     */
    @GetMapping("/administration/admin/cpanel")
    public String userCPanel(Model model, Principal principal) {

        log.debug("GET request on /administration/admin/cpanel");

        UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) principal;
        CoreUser user = (CoreUser) token.getPrincipal();

        log.debug("User state is: " + ReflectionToStringBuilder.toString(user));

        model.addAttribute("user", user);

        return "index";
    }

    @PostMapping("/administration/admin/cpanel/delete_platform_resources")
    public ResponseEntity<?> deletePlatformResources(@RequestParam String platformId) {

        log.debug("POST request on /administration/admin/cpanel/delete_platform_resources for info model with id = " + platformId);

        // Ask Registry
        try {
            ClearDataRequest request = new ClearDataRequest(null, platformId);

            ClearDataResponse response = rabbitManager.sendClearDataRequest(request);
            if (response != null) {
                if (response.getStatus() != HttpStatus.OK.value()) {
                    return new ResponseEntity<>(response.getMessage(),
                            new HttpHeaders(), HttpStatus.valueOf(response.getStatus()));
                }
            } else {
                log.warn("Registry unreachable!");
                return new ResponseEntity<>("Registry unreachable!",
                        new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } catch (CommunicationException e) {
            String message = "Registry threw communication exception: " + e.getMessage();
            log.warn(message, e);
            return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
    }

    @PostMapping("/administration/admin/cpanel/delete_information_model")
    public ResponseEntity<?> deleteInformationModel(@RequestParam String infoModelIdToDelete) {

        log.debug("POST request on /administration/admin/cpanel/delete_information_model for info model with id = " + infoModelIdToDelete);

        // Get InformationModelList from Registry
        ResponseEntity<?> responseEntity = informationModelService.getInformationModels();
        if (responseEntity.getStatusCode() != HttpStatus.OK)
            return responseEntity;
        else {

            for (InformationModel informationModel : (List<InformationModel>)responseEntity.getBody()) {
                log.debug(informationModel.getId() + " " + informationModel.getOwner());
                if (informationModel.getId().equals(infoModelIdToDelete)) {

                    // Ask Registry
                    try {
                        InformationModelRequest request = new InformationModelRequest();
                        request.setBody(informationModel);

                        InformationModelResponse response = rabbitManager.sendDeleteInfoModelRequest(request);
                        if (response != null) {
                            if (response.getStatus() != HttpStatus.OK.value()) {

                                return new ResponseEntity<>(response.getMessage(),
                                        new HttpHeaders(), HttpStatus.valueOf(response.getStatus()));
                            }
                        } else {
                            log.warn("Registry unreachable!");
                            return new ResponseEntity<>("Registry unreachable!",
                                    new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                        }
                    } catch (CommunicationException e) {
                        String message = "Registry threw communication exception: " + e.getMessage();
                        log.warn(message, e);
                        return new ResponseEntity<>(message, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
                    }

                    return new ResponseEntity<>(new HttpHeaders(), HttpStatus.OK);
                }
            }

            return new ResponseEntity<>("Information Model NOT FOUND",
                    new HttpHeaders(), HttpStatus.BAD_REQUEST);
        }

    }

    @PostMapping("/administration/admin/cpanel/delete_federation")
    public ResponseEntity<?> deleteFederation(@RequestParam String federationIdToDelete, Principal principal) {

        log.debug("POST request on /administration/admin/cpanel/delete_federation for federation with id = " + federationIdToDelete);
        return federationService.deleteFederation(federationIdToDelete, true, principal);
    }

    @PostMapping("/administration/admin/cpanel/leave_federation")
    public ResponseEntity<?> leaveFederation(@RequestParam String federationId, @RequestParam String platformId,
                                              Principal principal) {

        log.debug("POST request on /administration/user/cpanel/leave_federation for federationId = "
                + federationId + " platformId = " + platformId);
        return federationService.leaveFederation(federationId, platformId, principal, true);
    }

    @PostMapping("/administration/admin/cpanel/federation_invite")
    public ResponseEntity<?> inviteToFederation(@Valid @RequestBody InvitationRequest invitationRequest, Principal principal) {

        log.debug("POST request on /administration/user/cpanel/federation_invite :" + invitationRequest);
        return federationService.inviteToFederation(invitationRequest, principal,true);
    }
}