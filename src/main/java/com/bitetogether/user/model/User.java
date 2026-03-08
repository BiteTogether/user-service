package com.bitetogether.user.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
@Builder
@Table(name = "users")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(
    callSuper = true,
    exclude = {"friends"})
@ToString(exclude = {"friends"})
public class User extends BaseEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", updatable = false, nullable = false)
  Long id;

  @Column(name = "username", unique = true, nullable = false)
  String username;

  @Column(name = "firebase_uid", unique = true, nullable = false)
  String firebaseUid;

  @Column(name = "fullname")
  String fullName;

  @Column(name = "phone", unique = true, nullable = false)
  String phoneNumber;

  String avatar = null;

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
  @JsonIgnore
  @Builder.Default
  Set<User> friends = new HashSet<>();

  @Column boolean pushNotificationsEnabled = false;

  @Column boolean inAppNotificationsEnabled = false;

  @Column boolean isOnline = false;

  @Column LocalDateTime lastSeen = LocalDateTime.now();
}
