package com.hachicore.service;

import com.hachicore.user.dao.UserDao;
import com.hachicore.user.domain.Level;
import com.hachicore.user.domain.User;
import com.hachicore.user.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.List;

import static com.hachicore.user.service.UserService.MIN_LOGCOUNT_FOR_SILVER;
import static com.hachicore.user.service.UserService.MIN_RECOMMEND_FOR_GOLD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/test-applicationContext.xml")
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Autowired
    UserDao userDao;

    List<User> users;

    static class TestUserService extends UserService {
        private final String id;

        public TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) {
            if (user.getId().equals(this.id)) {
                super.upgradeLevels();
            }
        }
    }

    static class TestUserServiceException extends RuntimeException {
    }

    @Before
    public void setUp() {
        users = Arrays.asList(
                new User("테스트1", "테스트1", "t1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0),
                new User("테스트2", "테스트2", "t2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0),
                new User("테스트3", "테스트2", "t3", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD - 1),
                new User("테스트4", "테스트4", "t4", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD),
                new User("테스트5", "테스트5", "t5", Level.GOLD, 100, Integer.MAX_VALUE)
        );
    }

    @Test
    public void bean() {
        assertThat(this.userService, is(notNullValue()));
    }

    @Test
    public void upgradeLevels() {
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        userService.upgradeLevels();

        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(0), true);
        checkLevelUpgraded(users.get(0), false);
        checkLevelUpgraded(users.get(0), true);
        checkLevelUpgraded(users.get(0), false);
    }

    @Test
    public void add() {
        userDao.deleteAll();

        User userWithLevel = users.get(4);
        User userWithoutLevel = users.get(0);
        userWithoutLevel.setLevel(null);

        userService.add(userWithLevel);
        userService.add(userWithoutLevel);

        User userWithLevelRead = userDao.get(userWithLevel.getId());
        User userWithoutLevelRead = userDao.get(userWithoutLevel.getId());

        assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel()));
        assertThat(userWithoutLevelRead.getLevel(), is(Level.BASIC));
    }

    @Test
    public void upgradeAllOrNothing() {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        userDao.deleteAll();

        for (User user : users) {
            userDao.add(user);
        }

        try {
            testUserService.upgradeLevels();
            fail("TestUserServiceException expected");
        } catch (TestUserServiceException e) {

        }

        checkLevelUpgraded(users.get(1), false);
    }

    private void checkLevelUpgraded(User user, boolean upgraded) {
        User userUpdate = userDao.get(user.getId());

        if (upgraded) {
            assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
        } else {
            assertThat(userUpdate.getLevel(), is(user.getLevel()));
        }
    }
}
