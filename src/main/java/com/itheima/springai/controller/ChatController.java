package com.itheima.springai.controller;

import com.itheima.springai.repository.IChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;
    private final IChatHistoryRepository chatHistoryRepository;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> AiChat(String prompt, String chatId) { // 处理聊天请求
        // 1. 保存会话id
        chatHistoryRepository.save("chat", chatId);

        // 2. 请求模型
        return chatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 会话ID
                .stream()
                .content();
    }
}
