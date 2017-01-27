package eu.h2020.symbiote.model;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
 
public class UserAccount {

    public UserAccount (String username, String password, String name, String email, int role) {
        
        this.username = username;
        this.password = password;
        this.name = name;
        this.email = email;
        this.role = role;
    }

    public UserAccount () {

    }

    // @Id
    // private String id;

    @Indexed(unique = true)
    @NotNull
    @Size(min=4, max=30)
    private String username;

    @NotNull
    @Size(min=4, max=30)
    private String password;

    // @NotNull
    @Size(min=4, max=30)
    private String name;

    // @NotNull
    @Size(min=4, max=30)
    private String email;

    private String platformId;

    private int role; // 1 = ADMIN, 2 = USER

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public String getName() {
        return this.name;
    }

    public String getEmail() {
        return this.email;
    }

    public String getPlatformId() {
        return this.platformId;
    }

    public int getRole() {
        return this.role;
    }

    // @Override
    // public boolean isAccountNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isAccountNonLocked() {
    //     return true;
    // }

    // @Override
    // public boolean isCredentialsNonExpired() {
    //     return true;
    // }

    // @Override
    // public boolean isEnabled() {
    //     return true;
    // }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPlatformId(String platformId) {
        this.platformId = platformId;
    }

    public void setRole(int role) {
        this.role = role;
    }


}