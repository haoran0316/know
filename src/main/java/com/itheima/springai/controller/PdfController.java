package com.itheima.springai.controller;

import com.itheima.springai.entity.vo.Result;
import com.itheima.springai.repository.IChatHistoryRepository;
import com.itheima.springai.repository.IFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/pdf")
public class PdfController {

    private final IFileRepository fileRepository;

    private final ChatClient pdfChatClient;

    private final IChatHistoryRepository chatHistoryRepository;
    /**
     * 文件上传
     */
    @RequestMapping("/upload/{chatId}")
    public Result uploadPdf(@PathVariable String chatId, @RequestParam("file") MultipartFile file) {
        try {
            // 1. 校验文件是否为PDF格式
            if (!Objects.equals(file.getContentType(), "application/pdf")) {
                return Result.fail("只能上传PDF文件！");
            }
            // 2.保存文件
            boolean success = fileRepository.save(chatId, file.getResource());
            if(! success) {
                return Result.fail("保存文件失败！");
            }
            return Result.ok();
        } catch (Exception e) {
            log.error("Failed to upload PDF.", e);
            return Result.fail("上传文件失败！");
        }
    }

    /**
     * 文件下载
     */
    @GetMapping("/file/{chatId}")
    public ResponseEntity<Resource> download(@PathVariable("chatId") String chatId) throws IOException {
        // 1.读取文件
        Resource resource = fileRepository.getFile(chatId);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        // 2.文件名编码，在后续返回文件时写入响应头  filename="Spring%E6%95%99%E7%A8%8B.pdf"
        String filename = URLEncoder.encode(Objects.requireNonNull(resource.getFilename()), StandardCharsets.UTF_8);
        // 3.返回文件
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    /**
     * 文件对话
     */
    @GetMapping(value = "/chat", produces = "text/html;charset=UTF-8")
    public Flux<String> PdfChat(@RequestParam String prompt, @RequestParam(required = false, defaultValue = "default") String chatId) {
        // 1. 找到会话文件
        Resource file = fileRepository.getFile(chatId);
        if (!file.exists()) {
            // 如果文件不存在，不回答
            throw new RuntimeException("文件不存在，无法回答问题！");
        }

        // 2. 保存会话id
        chatHistoryRepository.save("pdf", chatId);

        // 3. 请求模型
        return pdfChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "chat_id == '"+ chatId +"'"))
                .stream()
                .content();
    }
}
