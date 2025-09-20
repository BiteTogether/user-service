package com.bitetogether.user.model;

import com.bitetogether.common.model.BaseEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  Long id;

  @Column(name = "username", nullable = false)
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

  @Column(name = "food_preferences")
  @JdbcTypeCode(SqlTypes.JSON)
  @Builder.Default
  String foodPreferences = "{}";

  @Column(name = "role", nullable = false)
  String role;

  @ManyToMany
  @JoinTable(
      name = "user_friends",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "friend_id"))
  @Builder.Default
  List<User> friendList = new ArrayList<>();
}
