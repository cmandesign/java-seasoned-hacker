package com.owaspdemo.common.model;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(unique = true, nullable = false)
    private String email;

    private String ssn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private boolean enabled = true;

    public AppUser() {}

    public AppUser(String username, String passwordHash, String email, String ssn, Role role,
                   String firstName, String lastName, String phoneNumber) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.ssn = ssn;
        this.role = role;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phoneNumber = phoneNumber;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSsn() { return ssn; }
    public void setSsn(String ssn) { this.ssn = ssn; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
