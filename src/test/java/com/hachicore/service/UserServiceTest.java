package com.hachicore.service;

import com.hachicore.user.dao.UserDao;
import com.hachicore.user.domain.Level;
import com.hachicore.user.domain.User;
import com.hachicore.user.service.UserService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailSender;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;

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

    @Autowired
    PlatformTransactionManager transactionManager;

    @Autowired
    MailSender mailSender;

    List<User> users;

    static class TestUserService extends UserService {
        private final String id;

        public TestUserService(String id) {
            this.id = id;
        }

        @Override
        protected void upgradeLevel(User user) throws Exception {
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
                new User("테스트1", "테스트1", "t1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER - 1, 0, "test1@test.com"),
                new User("테스트2", "테스트2", "t2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0, "test2@test.com"),
                new User("테스트3", "테스트2", "t3", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD - 1, "test3@test.com"),
                new User("테스트4", "테스트4", "t4", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD, "test4@test.com"),
                new User("테스트5", "테스트5", "t5", Level.GOLD, 100, Integer.MAX_VALUE, "test5@test.com")
        );
    }

    @Test
    public void bean() {
        assertThat(this.userService, is(notNullValue()));
    }

    @Test
    public void upgradeLevels() throws Exception {
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
    public void upgradeAllOrNothing() throws Exception {
        UserService testUserService = new TestUserService(users.get(3).getId());
        testUserService.setUserDao(this.userDao);
        testUserService.setTransactionManager(transactionManager);
        testUserService.setMailSender(mailSender);

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
