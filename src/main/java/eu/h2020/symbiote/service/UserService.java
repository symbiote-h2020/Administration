package eu.h2020.symbiote.service;

import eu.h2020.symbiote.model.UserAccount;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by  on //.
 */
@Service
public class UserService implements UserDetailsService {

    private MongoTemplate mongoTemplate;

    @Autowired
    public void setMongoTemplate(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        UserAccount user = getUserDetails(username);
        User userDetail = new User(user.getUsername(),user.getPassword(),true,true,true,true,getAuthorities(user.getRole()));

        return userDetail;
    }


    public UserAccount getUserDetails(String username){

        MongoOperations mongoOperation = (MongoOperations)mongoTemplate;
        UserAccount user = mongoOperation.findOne(
            new Query(Criteria.where("username").is(username)),
            UserAccount.class, "users"
        );
        
        return user;
    }

    public List<GrantedAuthority> getAuthorities(Integer role) {

        List<GrantedAuthority> authList = new ArrayList<GrantedAuthority>();
        if (role.intValue() == 1) {

            authList.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        
        } else if (role.intValue() == 2) {
        
            authList.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        return authList;
    }


    public void registerNewUser(UserAccount newUser) {

        MongoOperations mongoOperation = (MongoOperations)mongoTemplate;
        mongoOperation.save( newUser , "users" );

    }

    public void setUserPlatform(String username, String platformId) {

        MongoOperations mongoOperation = (MongoOperations)mongoTemplate;
        mongoOperation.updateFirst(
            new Query(Criteria.where("username").is(username)) ,
            new Update().set("platformId", platformId),
            "users" );

    }

}