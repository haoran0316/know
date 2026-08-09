package com.knowflow.springai.tools;

import com.knowflow.springai.entity.vo.KbNoteVO;
import com.knowflow.springai.service.IKbNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库管家工具：注册给 AI 调用，实现笔记自动入库与标签检索
 */
@RequiredArgsConstructor
@Component
public class KnowledgeBaseTools {

    private final IKbNoteService kbNoteService;

    /**
     * 保存笔记：调用前 AI 必须先提炼出标题、要点摘要，并打上 3~5 个标签
     */
    @Tool(description = "把一篇笔记存入知识库。调用前必须先提炼出标题、要点摘要，并打上 3~5 个精准标签")
    public Long saveNote(
            @ToolParam(description = "笔记标题") String title,
            @ToolParam(description = "AI 提炼的要点摘要") String summary,
            @ToolParam(description = "笔记原文内容") String content,
            @ToolParam(description = "标签列表，3~5 个") List<String> tags) {
        return kbNoteService.saveNoteWithTags(title, summary, content, tags, "manual", null);
    }

    /**
     * 查询笔记：按标签组合（AND）+ 关键词检索
     */
    @Tool(description = "按标签/关键词查询知识库中记过的笔记")
    public List<KbNoteVO> queryNotes(
            @ToolParam(required = false, description = "标签列表，多个标签表示要同时包含") List<String> tags,
            @ToolParam(required = false, description = "关键词") String keyword) {
        return kbNoteService.queryNotesByTagsAndKeyword(tags, keyword);
    }

    /**
     * 查询知识库所有标签
     */
    @Tool(description = "查询知识库中所有标签")
    public List<String> listTags() {
        return kbNoteService.listAllTags();
    }
}