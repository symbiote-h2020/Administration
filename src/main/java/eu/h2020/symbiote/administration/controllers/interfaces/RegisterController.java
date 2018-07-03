package eu.h2020.symbiote.administration.controllers.interfaces;


import eu.h2020.symbiote.administration.exceptions.ServiceValidationException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericBadRequestException;
import eu.h2020.symbiote.administration.exceptions.generic.GenericInternalServerErrorException;
import eu.h2020.symbiote.administration.exceptions.rabbit.CommunicationException;
import eu.h2020.symbiote.administration.model.CoreUser;
import eu.h2020.symbiote.administration.model.mappers.UserRoleValueTextMapping;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;


/**
 * Spring controller, handles user registration views and form validation.
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
@RequestMapping("/administration")
public interface RegisterController {

	@GetMapping("/register")
	String coreUserRegisterForm(Model model);

    @GetMapping("/register/roles")
    ResponseEntity<List<UserRoleValueTextMapping>> getUserRoles();

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    Map<String, Object> coreUserRegister(@Valid CoreUser coreUser,
                                         BindingResult bindingResult,
                                         WebRequest webRequest)
            throws CommunicationException, GenericBadRequestException, GenericInternalServerErrorException, ServiceValidationException;

    @GetMapping("/registrationConfirm")
    @ResponseStatus(HttpStatus.OK)
    String confirmRegistration(@RequestParam("token") String token) throws CommunicationException, GenericBadRequestException;
}