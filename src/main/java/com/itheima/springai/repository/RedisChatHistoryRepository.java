package com.itheima.springai.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的会话 ID 列表仓库,使用 Set 结构:
 * key 为 ai:history:{type},每个会话 ID 作为 Set 中的一个成员。
 */
@Repository
@RequiredArgsConstructor
public class RedisChatHistoryRepository implements IChatHistoryRepository {

    private static final String KEY_PREFIX = "ai:history:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 保存会话 ID(Set 天然去重,重复保存不会产生重复数据)
     */
    @Override
    public void save(String type, String chatId) {
        redisTemplate.opsForSet().add(KEY_PREFIX + type, chatId);
    }

    /**
     * 获取会话 ID 列表
     */
    @Override
    public List<String> getChatIds(String type) {
        Set<String> chatIds = redisTemplate.opsForSet().members(KEY_PREFIX + type);
        if (chatIds == null) {
            return List.of();
        }
        return chatIds.stream().sorted().toList();
    }
}
