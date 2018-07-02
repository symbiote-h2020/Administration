package eu.h2020.symbiote.administration.exceptions.token;

import java.util.NoSuchElementException;

public class VerificationTokenNotFoundException extends NoSuchElementException {

    public VerificationTokenNotFoundException(String s) {
        super("The token " + s + " was not found. Get a new verification token");
    }
}
