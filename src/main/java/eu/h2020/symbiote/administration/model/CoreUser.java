package eu.h2020.symbiote.administration.model;

import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.security.core.userdetails.User;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.util.ArrayList;
import java.util.Collection;



/**
 * Class for a symbIoTe user entity
 *
 * The same class is used for form validation and as a UserDetails object passed in the session.
 * That's why two constructors are needed, an empty validation one and a super-constructor one.
 * During login, CustomAuthenticationProvider is used
 *
 * @author Tilemachos Pechlivanoglou (ICOM)
 */
public class CoreUser extends User {

    /* -------- Properties -------- */
    
    @NotNull
    @Pattern(regexp="^[\\w-]{4,}$")
    @Size(max=30)
    private String validUsername;

    @NotNull
    @Size(min=4, max=30)
    private String validPassword;

    @NotNull
    @Pattern(regexp="^(([^<>()\\[\\]\\\\.,;:\\s@\"]+(\\.[^<>()\\[\\]\\\\.,;:\\s@\"]+)*)" +
            "|(\".+\"))@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}])" +
            "|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$")
    private String recoveryMail;

    @NotNull
    private UserRole role;


    /* -------- Constructors -------- */

    /**
     * Empty constructor for form validation
     */
    public CoreUser () {
        super("placeholder", "placeholder", false, false, false, false, new ArrayList<>());
    }

    /**
     * Constructor for use in UserDetails (Principal) object
     *
     * @param username              username
     * @param password              password
     * @param role                  user role
     * @param enabled               user is enabled
     * @param accountNonExpired     account isn't expired
     * @param credentialsNonExpired credentials aren't expired
     * @param accountNonLocked      account isn't locked
     * @param authorities           authorities to be granted to the user (mostly USER_ROLE)
     */
    public CoreUser(String username, String password, UserRole role, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities) {

        super(username, password, enabled, accountNonExpired,
        credentialsNonExpired, accountNonLocked, authorities);

        setRole(role);
    }
    

    /* -------- Getters & Setters -------- */

    public String getValidUsername() {
        return this.validUsername;
    }
    public void setValidUsername(String validUsername) {
        this.validUsername = validUsername;
    }

    public String getValidPassword() {
        return this.validPassword;
    }
    public void setValidPassword(String validPassword) {
        this.validPassword = validPassword;
    }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public String getRecoveryMail() {
        return this.recoveryMail;
    }
    public void setRecoveryMail(String recoveryMail) {
        this.recoveryMail = recoveryMail;
    }



    /* -------- Helper Methods -------- */

    public void clearPassword() { this.validPassword = null; }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}