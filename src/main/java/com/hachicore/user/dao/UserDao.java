package com.hachicore.user.dao;

import com.hachicore.user.domain.User;

import java.util.List;

public interface UserDao {
    void add(User user);

    User get(String id);

    List<User> getAll();

    void deleteAll();

    int getCount();

    void update(User user1);
}
