package com.songshanhu.blog.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.songshanhu.blog.entity.User;
import com.songshanhu.blog.mapper.UserMapper;
import com.songshanhu.blog.service.IUserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(@Lazy PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User findByUsername(String username) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
    }

    @Override
    public User findByEmail(String email) {
        return getOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
    }

    @Override
    public User findByUsernameOrEmail(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return null;
        }
        User u = findByUsername(identifier);
        if (u != null) {
            return u;
        }
        return findByEmail(identifier);
    }

    @Override
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRole("ROLE_USER");
        save(user);
    }

    @Override
    public void updatePassword(Long userId, String rawPassword) {
        User u = new User();
        u.setId(userId);
        u.setPassword(passwordEncoder.encode(rawPassword));
        updateById(u);
    }
}
