package eu.h2020.symbiote.entities;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
 
public class PlatformAccount {
	
    @NotNull
    @Size(min=4, max=30)
    private String username;

    @NotNull
    @Size(min=4, max=30)
    private String password;

    @NotNull
    @Size(min=4, max=30)
    private String name;

    @NotNull
    @Size(min=4, max=30)
    private String location;



    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getName() {
        return this.name;
    }

    public String getLocation() {
        return this.location;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

}