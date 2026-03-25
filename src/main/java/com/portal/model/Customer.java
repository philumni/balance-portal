package com.portal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Customer POJO.
 * Password field stores a BCrypt hash — never plain text.
 * @JsonIgnore ensures it never appears in any JSON response.
 */
public class Customer {

    private final String accountNumber;
    private final String username;
    private final String hashedPassword;
    private final String firstName;
    private final String lastName;
    private final String email;

    public Customer(String accountNumber, String username, String hashedPassword,
                    String firstName, String lastName, String email) {
        this.accountNumber  = accountNumber;
        this.username       = username;
        this.hashedPassword = hashedPassword;
        this.firstName      = firstName;
        this.lastName       = lastName;
        this.email          = email;
    }

    public String getAccountNumber()  { return accountNumber; }
    public String getUsername()       { return username; }

    @JsonIgnore
    public String getHashedPassword() { return hashedPassword; }

    public String getFirstName()      { return firstName; }
    public String getLastName()       { return lastName; }
    public String getEmail()          { return email; }
    public String getFullName()       { return firstName + " " + lastName; }
}
