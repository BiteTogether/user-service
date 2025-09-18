package com.bitetogether.user.model;

import com.bitetogether.common.enums.UserRole;
import com.bitetogether.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
  @Id
  @GeneratedValue(generator = "uuid2")
  @Column(name = "id", updatable = false, nullable = false)
  Long id;

  @Column(name = "username", unique = true, nullable = false)
  String username;

  @Column(name = "email", unique = true, nullable = false)
  String email;

  @Column(name = "password", nullable = false)
  String password;

  @Column(name = "fullname")
  String fullName;

  @Column(name = "phone", unique = true, nullable = false)
  String phoneNumber;

  @Column(name = "avatar")
  String avatar;

  @Column(name = "food_preferences", columnDefinition = "jsonb")
  String foodPreferences;

  @Column(name = "role", nullable = false)
  UserRole role;
}
