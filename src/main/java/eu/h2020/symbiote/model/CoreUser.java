package eu.h2020.symbiote.model;

import java.util.ArrayList;
import java.util.Collection;
import javax.validation.constraints.Size;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import eu.h2020.symbiote.security.commons.Token;
import eu.h2020.symbiote.security.commons.enums.UserRole;
import org.springframework.security.core.userdetails.User;
import org.apache.commons.lang.builder.ReflectionToStringBuilder;



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

    /* -------- Constants -------- */

    public static final int ERROR = 0;
    public static final int APP = 1;
    public static final int PLATFORM_INACTIVE = 2;
    public static final int PLATFORM_ACTIVE = 3;


    /* -------- Properties -------- */
    
    @NotNull
    @Size(min=4, max=30)
    private String validUsername;

    @NotNull
    @Size(min=4, max=30)
    private String validPassword;

    @NotNull
    @Size(min=4, max=30)
    private UserRole role;

    @NotNull
    private String recoveryMail;

    // @NotNull
    // @Size(min=4, max=30)
    private String federatedId; // Todo: how is this used?

    // Match either a word (letters, digits, "-" and "_") with 30 characters max
    @NotNull
    @Pattern(regexp="(^\\Z|^[\\w-][\\w-][\\w-][\\w-]+\\Z)")
    @Size(min=1, max=30)
    private String platformId;

    @NotNull
    @Size(min=4, max=30)
    private String platformName;

    @NotNull
    // @Size(min=4, max=120)
    private String platformUrl;

    private int state;

    private PlatformDetails platformDetails;
    private Federation federation;

    private Token token;


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
     * @param token                 user token returned by Core AAM
     * @param platformId            contains null or the platform's id, if user is a platform owner
     */
    public CoreUser(String username, String password, UserRole role, boolean enabled,
            boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection authorities,
            Token token, String platformId) {

        super(username, password, enabled, accountNonExpired,
        credentialsNonExpired, accountNonLocked, authorities);

        setRole(role);
        setToken(token);

        if(platformId == null){
            setState(this.APP);
        } else {
            setState(this.PLATFORM_INACTIVE);
            setPlatformId(platformId);
        }
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


    public String getFederatedId() {
        return this.federatedId;
    }
    public void setFederatedId(String federatedId) {
        this.federatedId = federatedId;
    }


    public String getPlatformId() {
        return this.platformId;
    }
    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }


    public String getPlatformName() {
        return this.platformName;
    }
    public void setPlatformName(String platformName) {
        this.platformName = platformName;
    }


    public String getPlatformUrl() {
        return this.platformUrl;
    }
    public void setPlatformUrl(String platformUrl) {
        this.platformUrl = platformUrl;
    }


    public int getState() {
        return this.state;
    }
    public void setState(int state) {
        this.state = state;
    }


    public PlatformDetails getPlatformDetails() {
        return this.platformDetails;
    }
    public void setPlatformDetails(PlatformDetails platformDetails) {
        this.platformDetails = platformDetails;
    }


    public Federation getFederation() {
        return this.federation;
    }
    public void setFederation(Federation federation) {
        this.federation = federation;
    }


    public Token getToken() {
        return this.token;
    }
    public void setToken(Token token) {
        this.token = token;
    }


    /* -------- Helper Methods -------- */

    public void clearPassword() {
        this.validPassword = null;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}