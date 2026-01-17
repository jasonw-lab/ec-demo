package com.demo.ec.account.application;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.ec.account.domain.Account;
import com.demo.ec.account.domain.User;
import com.demo.ec.account.gateway.AccountMapper;
import com.demo.ec.account.gateway.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserMapper userMapper;
    private final AccountMapper accountMapper;

    public UserServiceImpl(UserMapper userMapper, AccountMapper accountMapper) {
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
    }

    @Override
    @Transactional
    public Long syncUser(String firebaseUid, String email, String name, String providerId) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>()
                .eq(User::getFirebaseUid, firebaseUid);
        User existing = userMapper.selectOne(wrapper);
        if (existing != null) {
            boolean changed = false;
            if (email != null && !email.equals(existing.getEmail())) {
                existing.setEmail(email);
                changed = true;
            }
            if (name != null && !name.isBlank() && !name.equals(existing.getName())) {
                existing.setName(name);
                changed = true;
            }
            if (providerId != null && !providerId.equals(existing.getProviderId())) {
                existing.setProviderId(providerId);
                changed = true;
            }
            if (changed) {
                userMapper.updateById(existing);
            }
            return existing.getId();
        }

        // Try to create new user - handle race condition where another thread might have created it
        try {
            User user = new User();
            user.setFirebaseUid(firebaseUid);
            user.setEmail(email);
            user.setName(name);
            user.setProviderId(providerId);
            userMapper.insert(user);
            Long userId = user.getId();

            if (userId == null) {
                log.error("User insert succeeded but userId is null for firebaseUid={}", firebaseUid);
                throw new RuntimeException("Failed to create user: userId is null after insert");
            }

            // Create initial account row logically linked to the user
            Account account = new Account();
            account.setUserId(userId);
            // simple initial balance: 1000.00
            account.setTotal(new BigDecimal("1000.00"));
            account.setUsed(BigDecimal.ZERO);
            account.setResidue(new BigDecimal("1000.00"));
            accountMapper.insert(account);

            log.info("Created new user and account: firebaseUid={}, userId={}", firebaseUid, userId);
            return userId;
        } catch (DataIntegrityViolationException ex) {
            // Race condition: another thread created the user between our select and insert
            // Re-query to get the existing user
            log.warn("Duplicate key violation during user creation (race condition), re-querying: firebaseUid={}, error={}", 
                    firebaseUid, ex.getMessage());
            User existingAfterRace = userMapper.selectOne(wrapper);
            if (existingAfterRace != null) {
                log.info("Found existing user after race condition: firebaseUid={}, userId={}", 
                        firebaseUid, existingAfterRace.getId());
                return existingAfterRace.getId();
            }
            // If still not found, re-throw the exception
            log.error("Duplicate key exception but user not found on re-query: firebaseUid={}", firebaseUid, ex);
            throw new RuntimeException("Failed to sync user: duplicate key violation and user not found on retry", ex);
        }
    }

    @Override
    @Transactional
    public void updatePersonalInformation(Long userId, String lastName, String firstName,
                                          String lastNameKana, String firstNameKana,
                                          String birthDate, String gender) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found: userId=" + userId);
        }

        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setLastNameKana(lastNameKana);
        user.setFirstNameKana(firstNameKana);
        user.setBirthDate(birthDate);
        user.setGender(gender);

        userMapper.updateById(user);
        log.info("Updated personal information: userId={}", userId);
    }
}


