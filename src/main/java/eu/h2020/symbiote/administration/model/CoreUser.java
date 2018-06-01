package eu.h2020.symbiote.administration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.security.core.GrantedAuthority;
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
    @Id
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

    @NotNull
    private boolean termsAccepted;

    @NotNull
    private boolean conditionsAccepted;

    @NotNull
    private boolean usernamePermission;

    @NotNull
    private boolean emailPermission;

    @NotNull
    private boolean publicKeysPermission;

    @NotNull
    private boolean jwtPermission;


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
     * @param recoveryMail          use email
     * @param role                  user role
     * @param enabled               user is enabled
     * @param accountNonExpired     account isn't expired
     * @param credentialsNonExpired credentials aren't expired
     * @param accountNonLocked      account isn't locked
     * @param authorities           authorities to be granted to the user (mostly USER_ROLE)
     * @param termsAccepted         shows if user accepts the terms
     * @param conditionsAccepted    shows if user accepts the conditions
     * @param usernamePermission    shows if user gives permission for using their username for analytical and marketing purposes
     * @param emailPermission       shows if user gives permission for using their email for analytical and marketing purposes
     * @param publicKeysPermission  shows if user gives permission for using their public key for analytical and marketing purposes
     * @param jwtPermission         shows if user gives permission for using their jwt for analytical and marketing purposes
     */
    @PersistenceConstructor
    @JsonCreator
    public CoreUser(@JsonProperty("username") String username,
                    @JsonProperty("password") String password,
                    @JsonProperty("enabled") boolean enabled,
                    @JsonProperty("accountNonExpired") boolean accountNonExpired,
                    @JsonProperty("credentialsNonExpired") boolean credentialsNonExpired,
                    @JsonProperty("accountNonLocked") boolean accountNonLocked,
                    @JsonProperty("authorities") Collection<? extends GrantedAuthority> authorities,
                    @JsonProperty("recoveryMail") String recoveryMail,
                    @JsonProperty("role") UserRole role,
                    @JsonProperty("termsAccepted") boolean termsAccepted,
                    @JsonProperty("conditionsAccepted") boolean conditionsAccepted,
                    @JsonProperty("usernamePermission") boolean usernamePermission,
                    @JsonProperty("emailPermission") boolean emailPermission,
                    @JsonProperty("publicKeysPermission") boolean publicKeysPermission,
                    @JsonProperty("jwtPermission") boolean jwtPermission) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.validUsername = username;
        this.recoveryMail = recoveryMail;
        this.role = role;
        this.termsAccepted = termsAccepted;
        this.conditionsAccepted = conditionsAccepted;
        this.usernamePermission = usernamePermission;
        this.emailPermission = emailPermission;
        this.publicKeysPermission = publicKeysPermission;
        this.jwtPermission = jwtPermission;
    }

//    public CoreUser(String username, String password, UserRole role, boolean enabled,
//            boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities) {
//
//        super(username, password, enabled, accountNonExpired,
//        credentialsNonExpired, accountNonLocked, authorities);
//
//        setRole(role);
//    }
    

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

    public boolean isTermsAccepted() { return termsAccepted; }
    public void setTermsAccepted(boolean termsAccepted) { this.termsAccepted = termsAccepted; }

    public boolean isConditionsAccepted() { return conditionsAccepted; }
    public void setConditionsAccepted(boolean conditionsAccepted) { this.conditionsAccepted = conditionsAccepted; }

    public boolean isUsernamePermission() { return usernamePermission; }
    public void setUsernamePermission(boolean usernamePermission) { this.usernamePermission = usernamePermission; }

    public boolean isEmailPermission() { return emailPermission; }
    public void setEmailPermission(boolean emailPermission) { this.emailPermission = emailPermission; }

    public boolean isPublicKeysPermission() { return publicKeysPermission; }
    public void setPublicKeysPermission(boolean publicKeysPermission) { this.publicKeysPermission = publicKeysPermission; }

    public boolean isJwtPermission() { return jwtPermission; }
    public void setJwtPermission(boolean jwtPermission) { this.jwtPermission = jwtPermission; }

    /* -------- Helper Methods -------- */

    public void clearSensitiveData() {
        this.validPassword = "";
        this.recoveryMail = "";
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}