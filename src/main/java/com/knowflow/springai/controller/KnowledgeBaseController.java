package com.knowflow.springai.controller;

import com.knowflow.springai.entity.query.KbNoteRequest;
import com.knowflow.springai.entity.vo.KbNoteVO;
import com.knowflow.springai.entity.vo.Result;
import com.knowflow.springai.repository.IFileRepository;
import com.knowflow.springai.repository.IChatHistoryRepository;
import com.knowflow.springai.service.IKbNoteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 知识库管家接口
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/ai/kb")
public class KnowledgeBaseController {

    private final IKbNoteService kbNoteService;
    private final ChatClient kbChatClient;
    private final ChatClient pdfChatClient;
    private final IFileRepository fileRepository;
    private final IChatHistoryRepository chatHistoryRepository;

    /**
     * 保存笔记（供前端或 AI 工具调用）
     */
    @PostMapping("/note")
    public Result saveNote(@RequestBody KbNoteRequest request) {
        try {
            Long noteId = kbNoteService.saveNoteWithTags(
                    request.getTitle(),
                    request.getSummary(),
                    request.getContent(),
                    request.getTags(),
                    request.getSource(),
                    request.getChatId());
            return Result.ok(noteId);
        } catch (Exception e) {
            log.error("保存知识库笔记失败", e);
            return Result.fail("保存笔记失败：" + e.getMessage());
        }
    }

    /**
     * 按标签/关键词查询笔记
     */
    @GetMapping("/notes")
    public List<KbNoteVO> queryNotes(
            @RequestParam(required = false) List<String> tags,
            @RequestParam(required = false) String keyword) {
        return kbNoteService.queryNotesByTagsAndKeyword(tags, keyword);
    }

    /**
     * 查询所有标签
     */
    @GetMapping("/tags")
    public List<String> listTags() {
        return kbNoteService.listAllTags();
    }

    /**
     * PDF 内容沉淀到知识库：让 RAG 客户端通读该会话文档并提炼，再入库
     */
    @PostMapping("/pdf/{chatId}/digest")
    public Result digestPdf(@PathVariable String chatId) {
        Resource file = fileRepository.getFile(chatId);
        if (!file.exists()) {
            return Result.fail("文件不存在，无法沉淀到知识库");
        }
        try {
            // 1. 让 PDF 问答客户端（RAG）通读该会话文档并提炼
            String summary = pdfChatClient.prompt()
                    .user("请通读当前文档，提炼出核心学习要点，并严格按以下格式输出：\n"
                            + "标题：xxx\n"
                            + "要点：\n"
                            + "- xxx\n"
                            + "- xxx\n"
                            + "标签：标签1,标签2,标签3")
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                    .advisors(a -> a.param(VectorStoreDocumentRetriever.FILTER_EXPRESSION, "chat_id == '" + chatId + "'"))
                    .call()
                    .content();

            // 2. 解析标题与标签，其余内容作为笔记内容保存
            String title = extractField(summary, "标题");
            List<String> tags = parseTags(extractField(summary, "标签"));
            if (!StringUtils.hasText(title)) {
                title = "来自PDF的笔记";
            }
            if (tags.isEmpty()) {
                tags = List.of("PDF");
            }
            Long noteId = kbNoteService.saveNoteWithTags(title, summary, summary, tags, "pdf", chatId);
            return Result.ok(noteId);
        } catch (Exception e) {
            log.error("PDF 沉淀知识库失败 chatId={}", chatId, e);
            return Result.fail("PDF 沉淀失败：" + e.getMessage());
        }
    }

    /** 提取 "键：值" 格式中的值（支持中文冒号/英文冒号，取到行尾） */
    private String extractField(String text, String key) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        Pattern pattern = Pattern.compile(key + "[：:](.*?)(\\n|$)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            return value.isEmpty() ? null : value;
        }
        return null;
    }

    /** 解析标签字符串，支持中文逗号/英文逗号/顿号/空白分隔 */
    private List<String> parseTags(String tagText) {
        if (!StringUtils.hasText(tagText)) {
            return List.of();
        }
        return Arrays.stream(tagText.split("[，,、\\s]+"))
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(10)
                .toList();
    }
    /**
     * 知识库管家对话：发笔记 → AI 自动提炼、打标签、调用工具入库；或提问检索已有笔记
     * 注意：使用非流式 call()，规避 OpenAI 兼容接口「流式 + 工具调用」时 ChunkMerger 合并报错
     */
    @PostMapping(value = "/chat", produces = "text/plain;charset=utf-8")
    public String kbChat(@RequestParam("prompt") String prompt, @RequestParam("chatId") String chatId) {
        // 1. 保存会话ID
        chatHistoryRepository.save("kb", chatId);
        // 2. 请求模型（AI 会自动调用知识库工具完成入库/检索）
        return kbChatClient.prompt()
                .user(prompt)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .content();
    }
}