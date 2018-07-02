package eu.h2020.symbiote.administration.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

public class VerificationToken {

    @Id
    private String id;

    private String token;

    private CoreUser user;

    private Date expirationDate;

    @PersistenceConstructor
    public VerificationToken(String id, String token, CoreUser user, Date expirationDate) {
        this.id = id;
        this.token = token;
        this.user = user;
        this.expirationDate = expirationDate;
    }

    public VerificationToken(String token, CoreUser user, int expirationTimeInHours) {
        this.token = token;
        this.user = user;
        this.expirationDate = calculateExpiryDate(expirationTimeInHours);
    }

    public String getId() { return id; }

    public String getToken() { return token; }

    public CoreUser getUser() { return user; }

    public Date getExpirationDate() { return expirationDate; }

    private Date calculateExpiryDate(int expirationTimeInHours) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Timestamp(cal.getTime().getTime()));
        cal.add(Calendar.HOUR, expirationTimeInHours);
        return new Date(cal.getTime().getTime());
    }

    @Override
    public String toString() {
        return "VerificationToken{" +
                "id=" + id +
                ", token='" + token + '\'' +
                ", user=" + user +
                ", expirationDate=" + expirationDate +
                '}';
    }
}
