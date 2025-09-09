package com.bitetogether.user.model;

import com.bitetogether.common.model.BaseEntity;
import jakarta.persistence.*;

@Entity
public class User extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "fullname")
    private String fullname;

    @Column(name = "avatar")
    private String avatar;

    @Column(name = "food_preferences", columnDefinition = "jsonb")
    private String foodPreferences;
}
