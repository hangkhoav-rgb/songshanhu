package com.songshanhu.blog.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.songshanhu.blog.entity.User;

public interface IUserService extends IService<User> {
    User findByUsername(String username);
    User findByEmail(String email);
    User findByUsernameOrEmail(String identifier);
    void register(User user);
    void updatePassword(Long userId, String rawPassword);
}
