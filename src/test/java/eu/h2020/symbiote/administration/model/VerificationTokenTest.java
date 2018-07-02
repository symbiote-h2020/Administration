package eu.h2020.symbiote.administration.model;

import org.junit.Test;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;

public class VerificationTokenTest {

    @Test
    public void constructor1Test() {
        int expirationTimeInHours = 1;
        VerificationToken verificationToken = new VerificationToken("token", new CoreUser(), expirationTimeInHours);
        Date currentDate = new Date();

        assertEquals(TimeUnit.MILLISECONDS.toHours(currentDate.getTime()) + expirationTimeInHours,
                TimeUnit.MILLISECONDS.toHours(verificationToken.getExpirationDate().getTime()));
    }

}