package com;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String mail;
    private String username;
    private String password;

    public User() {}

    public User(String username, String password, String mail) {
        this.username = username;
        this.password = password;
        this.mail = mail;
    }

    public Long getId() { return id; }
    @JsonProperty("username")
    public String getUsername() { return username; }
    @JsonProperty("password")
    public String getPassword() { return password; }
    @JsonProperty("mail")
    public String getMail() { return mail; }

    public void setId(Long id) { this.id = id; }
    @JsonProperty("username")
    public void setUsername(String username) { this.username = username; }
    @JsonProperty("password")
    public void setPassword(String password) { this.password = password; }
    @JsonProperty("mail")
    public void setMail(String mail) { this.mail = mail; }

}
