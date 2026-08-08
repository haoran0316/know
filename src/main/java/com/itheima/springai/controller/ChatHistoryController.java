package com.itheima.springai.controller;

import com.itheima.springai.entity.vo.MessageVO;
import com.itheima.springai.repository.IChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/history")
public class ChatHistoryController {
    private final IChatHistoryRepository chatHistoryRepository;
    private final ChatMemory chatMemory;

    /**
     * 获取会话ID列表
     */
    @GetMapping("/{type}")
    public List<String> getChatIds(@PathVariable String type) {
        return chatHistoryRepository.getChatIds(type);
    }

    /**
     * 查询会话历史记录
     */
    @GetMapping("/{type}/{chatId}")
    public List<MessageVO> getChatHistories(@PathVariable String type, @PathVariable String chatId) {
        List<Message> chatHistories = chatMemory.get(chatId);
        if(chatHistories == null){
            return List.of();
        }
        return chatHistories.stream().map(cH -> new MessageVO(cH)).toList();
    }
}
