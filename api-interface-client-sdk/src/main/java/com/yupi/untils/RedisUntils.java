package com.yupi.untils;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class RedisUntils {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    public StringRedisTemplate getStringRedisTemplate(){
        return stringRedisTemplate;
    }
}
