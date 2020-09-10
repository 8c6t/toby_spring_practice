package com.hachicore.user.dao;

public class UserDaoTest {

    public static void main(String[] args) {
        ConnectionMaker connectionMaker = new DConnectionMaker();
        UserDao dao = new UserDao(connectionMaker);
    }
}
