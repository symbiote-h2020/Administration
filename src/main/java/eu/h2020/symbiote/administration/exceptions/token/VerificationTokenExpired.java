package eu.h2020.symbiote.administration.exceptions.token;

public class VerificationTokenExpired extends RuntimeException {

    public VerificationTokenExpired(String s) {
        super("The token " + s + " has been expired");
    }
}
