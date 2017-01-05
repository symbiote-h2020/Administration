package eu.h2020.symbiote.entities;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
 
public class AdminAccount {
	
    @NotNull
    @Size(min=4, max=30)
    private String username;

    @NotNull
    @Size(min=4, max=30)
    private String password;

    @NotNull
    @Size(min=4, max=30)
    private String email;


    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getEmail() {
        return this.email;
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

}