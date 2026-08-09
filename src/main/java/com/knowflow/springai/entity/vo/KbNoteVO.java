package com.knowflow.springai.entity.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识库笔记返回对象
 */
@Data
public class KbNoteVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private String source;
    private String chatId;
    private List<String> tags;
    /** 创建时间（ISO 字符串） */
    private String createdAt;
}