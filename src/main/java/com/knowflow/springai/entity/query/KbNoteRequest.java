package com.knowflow.springai.entity.query;

import lombok.Data;

import java.util.List;

/**
 * 保存笔记请求体
 */
@Data
public class KbNoteRequest {
    /** 笔记标题（AI 提炼） */
    private String title;
    /** AI 提炼的要点摘要 */
    private String summary;
    /** 笔记原文内容 */
    private String content;
    /** 标签列表 */
    private List<String> tags;
    /** 来源：manual / pdf */
    private String source;
    /** 来源会话ID */
    private String chatId;
}