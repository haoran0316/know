package com.itheima.springai.service.impl;

import com.itheima.springai.service.ChatHistoryService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
public class ChatHistoryServiceImpl implements ChatHistoryService {
    private final Map<String, List<String>> chatHistory = new HashMap<>();

    /**
     * 保存会话ID
     */
    @Override
    public void save(String type, String chatId) {
        List<String> chatIds = chatHistory.computeIfAbsent(type, k -> new ArrayList<>());
        if(chatIds.contains(chatId)){
            return;
        }
        chatIds.add(chatId);
    }

    /**
     * 获取会话ID列表
     */
    @Override
    public List<String> getChatIds(String type) {
        return chatHistory.getOrDefault(type, List.of());
    }
}
