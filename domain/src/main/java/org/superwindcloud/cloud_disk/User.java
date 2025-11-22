package org.superwindcloud.cloud_disk;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(
    name =
        "users") // Changed table name to 'users' to avoid potential conflicts with 'user' keyword
// in some databases
@Data
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String username;
  private String email;
  private String password; // Added password field
  private boolean enabled = true; // Added enabled field with default true
}
