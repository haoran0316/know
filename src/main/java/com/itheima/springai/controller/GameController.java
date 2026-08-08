package com.itheima.springai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class GameController {
    private final ChatClient gameChatClient;

    @GetMapping(value = "/game", produces = "text/html;charset=utf-8")
    public Flux<String> gameChat(String prompt, String chatId) { // 处理聊天请求
        // 请求模型
        return gameChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 会话ID
                .stream()
                .content();
    }
}
