package com.knowflow.springai.controller;

import com.knowflow.springai.repository.IChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@RestController
@RequestMapping("/ai")
public class ChatController {
    private final ChatClient chatClient;
    private final IChatHistoryRepository chatHistoryRepository;

    @PostMapping(value = "/chat", produces = "text/html;charset=utf-8")
    public Flux<String> AiChat(@RequestParam("prompt") String prompt,     // 处理聊天请求
                               @RequestParam("chatId") String chatId,
                               @RequestParam(value = "files", required = false) List<MultipartFile> files) {
        // 1. 保存会话id
        chatHistoryRepository.save("chat", chatId);

        // 2. 请求模型
        if (files == null || files.isEmpty()) {
            // 没有附件, 纯文本聊天
            return textChat(prompt, chatId);
        } else {
            // 有附件, 多模态聊天
            return mutiModalChat(prompt, chatId, files);
        }
    }

    /**
     * 纯文本聊天
     */
    private Flux<String> textChat(String prompt, String chatId) {
        return chatClient.prompt().
                user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 会话ID
                .stream()
                .content();
    }

    /**
     * 多模态聊天
     */
    private Flux<String> mutiModalChat(String prompt, String chatId, List<MultipartFile> files) {
        // 1. 解析多媒体
        List<Media> medias = files.stream()
                .map(file -> new Media(MimeType.valueOf(Objects.requireNonNull(file.getContentType())), file.getResource()))
                .toList();
        // 2. 请求模型
        return chatClient.prompt().
                user(p -> p.text(prompt).media(medias.toArray(m -> new Media[m])))
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId)) // 会话ID
                .stream()
                .content();
    }
}
