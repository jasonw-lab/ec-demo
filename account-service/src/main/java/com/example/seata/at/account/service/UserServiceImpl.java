package com.example.seata.at.account.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.seata.at.account.domain.entity.Account;
import com.example.seata.at.account.domain.entity.User;
import com.example.seata.at.account.domain.mapper.AccountMapper;
import com.example.seata.at.account.domain.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

        User user = new User();
        user.setFirebaseUid(firebaseUid);
        user.setEmail(email);
        user.setName(name);
        user.setProviderId(providerId);
        userMapper.insert(user);
        Long userId = user.getId();

        // Create initial account row logically linked to the user
        Account account = new Account();
        account.setUserId(userId);
        // simple initial balance: 1000.00
        account.setTotal(new BigDecimal("1000.00"));
        account.setUsed(BigDecimal.ZERO);
        account.setResidue(new BigDecimal("1000.00"));
        account.setFrozen(BigDecimal.ZERO);
        accountMapper.insert(account);

        log.info("Created new user and account: firebaseUid={}, userId={}", firebaseUid, userId);
        return userId;
    }
}


