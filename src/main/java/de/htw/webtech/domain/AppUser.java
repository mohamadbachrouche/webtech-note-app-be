package de.htw.webtech.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_user",
       uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class AppUser implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Email
    @NotBlank
    @Column(nullable = false, unique = true)
    private String email;

    @NotBlank
    @JsonIgnore // never serialise the password hash out of the API
    @Column(nullable = false)
    private String password; // BCrypt hash — never plain text

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public void setPassword(String password) { this.password = password; }

    // --- UserDetails contract ---

    @JsonIgnore
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @JsonIgnore
    @Override public String getPassword()  { return password; }
    @Override public String getUsername()  { return email; } // email is the login identifier

    @JsonIgnore @Override public boolean isAccountNonExpired()    { return true; }
    @JsonIgnore @Override public boolean isAccountNonLocked()     { return true; }
    @JsonIgnore @Override public boolean isCredentialsNonExpired(){ return true; }
    @JsonIgnore @Override public boolean isEnabled()              { return true; }
}
