package eu.h2020.symbiote.administration.services.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ValidationService {
    private static Log log = LogFactory.getLog(ValidationService.class);

    public ResponseEntity<?> getRequestErrors(BindingResult bindingResult) {
        Map<String, Object> responseBody = new HashMap<>();
        String errorPrefix = "error_";
        String mainErrorMessage = "error";

        List<FieldError> errors = bindingResult.getFieldErrors();
        for (FieldError fieldError : errors) {
            log.debug(fieldError.getField());
            String errorField;
            String errorMessage = fieldError.getDefaultMessage();
            String[] parts = fieldError.getField().split("\\[");

            if (parts.length > 1) {

                int errorFieldIndex = Integer.parseInt(parts[1].split("]")[0]);
                log.debug("errorFieldIndex = " + errorFieldIndex);
                errorField = errorPrefix + parts[0] + parts[1].replace(".", "_").split("]")[1];
                ArrayList<String> errorList;

                if (responseBody.get(errorField) == null) {
                    errorList = new ArrayList<>();
                    for (int i = 0; i < errorFieldIndex; i++)
                        errorList.add("");
                    errorList.add(errorMessage);
                } else {
                    errorList = (ArrayList<String>) responseBody.get(errorField);

                    if (errorFieldIndex < errorList.size())
                        errorList.set(errorFieldIndex, errorMessage);
                    else {
                        for (int i = errorList.size(); i < errorFieldIndex; i++)
                            errorList.add("");
                        errorList.add(errorMessage);
                    }
                }

                responseBody.put(errorField, errorList);
                log.debug(responseBody);
            }
            else {
                errorField = errorPrefix + fieldError.getField().replace(".", "_");
                responseBody.put(errorField, errorMessage);
            }
            log.debug(errorField + ": " + errorMessage);

        }

        responseBody.put(mainErrorMessage, "Invalid Arguments");

        return new ResponseEntity<>(responseBody, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }
}
