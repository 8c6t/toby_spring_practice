package com.hachicore.user.service;

import com.hachicore.user.domain.User;

public interface UserService {

    void add(User user);

    void upgradeLevels();

}
