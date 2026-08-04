package com.itheima.springai.service;

import java.util.List;

public interface ChatHistoryService {
    /**
     * 保存会话记录
     */
    void save(String type, String chatId);

    /**
     * 获取会话ID列表
     */
    List<String> getChatIds(String type);
}
