package com.itheima.springai.controller;

import com.itheima.springai.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ServiceController {
    private final ChatClient serviceChatClient;
    private final ChatHistoryRepository chatHistoryRepository;

    @GetMapping(value = "/service", produces = "text/html;charset=utf-8")
    public String chat(String prompt, String chatId) { // 处理聊天请求
        // 1. 保存会话id
        chatHistoryRepository.save("service", chatId);

        // 2. 请求模型
        return serviceChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 会话ID
                .call()
                .content();
    }
}
