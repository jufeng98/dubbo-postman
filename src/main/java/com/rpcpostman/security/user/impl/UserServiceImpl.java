/*
 * MIT License
 *
 * Copyright (c) 2019 everythingbest
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rpcpostman.security.user.impl;

import com.rpcpostman.security.entity.RoleType;
import com.rpcpostman.security.entity.User;
import com.rpcpostman.security.user.UserService;
import com.rpcpostman.service.repository.redis.RedisKeys;
import com.rpcpostman.service.repository.redis.RedisRepository;
import com.rpcpostman.util.JSON;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author everythingbest
 * 用户相关操作
 */
@Service
public class UserServiceImpl implements UserService {

    final String defaultAdminCode = "00001";

    @Autowired
    RedisRepository redisRepository;

    @Override
    public List<User> list() {

        List<User> userList = new ArrayList<>();

        List<Object> userStrs = redisRepository.mapGetValues(RedisKeys.USER_KEY);

        for (Object str : userStrs) {

            User user = JSON.parseObject(str.toString(), User.class);

            //只返回有权限的用户
            if (user.getRoles().size() > 0) {

                userList.add(user);
            }
        }

        return userList;
    }

    @Override
    public boolean saveNewUser(User user) {

        String utr = JSON.objectToString(user);

        redisRepository.mapPut(RedisKeys.USER_KEY, user.getUserCode(), utr);

        return true;
    }

    @Override
    public User findOrAdd(String userCode) {

        Object userStr = redisRepository.mapGet(RedisKeys.USER_KEY, userCode);

        if (userStr == null || userStr.toString().isEmpty()) {

            User user = User.of(userCode);

            if (userCode.equals(defaultAdminCode)) {

                user.getRoles().add(RoleType.ADMIN);
            }

            String utr = JSON.objectToString(user);

            redisRepository.mapPut(RedisKeys.USER_KEY, userCode, utr);

            return user;

        } else {

            User user = JSON.parseObject(userStr.toString(), User.class);

            if (userCode.equals(defaultAdminCode)) {

                user.getRoles().add(RoleType.ADMIN);
            }

            return user;
        }
    }

    @Override
    public boolean update(User user) {

        String utr = JSON.objectToString(user);

        redisRepository.mapPut(RedisKeys.USER_KEY, user.getUserCode(), utr);

        return true;
    }

    @Override
    public boolean delete(String userCode) {

        redisRepository.removeMap(RedisKeys.USER_KEY, userCode);

        return true;
    }
}
