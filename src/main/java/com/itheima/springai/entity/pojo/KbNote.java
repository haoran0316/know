package com.itheima.springai.entity.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库笔记表
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("kb_note")
public class KbNote implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 笔记标题（AI 提炼） */
    private String title;

    /** 笔记原文内容 */
    private String content;

    /** AI 提炼的要点摘要 */
    private String summary;

    /** 来源：manual 手动 / pdf 来自PDF */
    private String source;

    /** 来源会话ID（PDF 沉淀时记录） */
    private String chatId;

    /** 内容指纹（用于去重） */
    private String contentHash;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}