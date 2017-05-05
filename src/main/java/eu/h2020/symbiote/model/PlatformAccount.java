package eu.h2020.symbiote.model;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import eu.h2020.symbiote.model.Platform;
 
public class PlatformAccount {

    public PlatformAccount (String username, String password, String name, String email) {
        
        this.username = username;
        this.password = password;
        this.email = email;
    }

    public PlatformAccount () {

    }

    // @Id
    // private String id;

    @NotNull
    @Size(min=4, max=30)
    private String username;

    @NotNull
    @Size(min=4, max=30)
    private String password;

    // @NotNull
    // @Size(min=4, max=30)
    // private String name;

    // @NotNull
    @Size(min=4, max=30)
    private String email;

    @NotNull
    @Valid
    private Platform platform;



    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getEmail() {
        return this.email;
    }

    public Platform getPlatform(){
        return this.platform;
    }


    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPlatform(Platform platform) {
        this.platform = platform;
    }


}