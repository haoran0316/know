package com.itheima.springai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.springai.entity.pojo.KbNote;
import com.itheima.springai.entity.vo.KbNoteVO;

import java.util.List;

/**
 * 知识库笔记服务
 */
public interface IKbNoteService extends IService<KbNote> {

    /**
     * 保存笔记（含标签）：相同内容自动更新而非重复插入
     */
    Long saveNoteWithTags(String title, String summary, String content, List<String> tags, String source, String chatId);

    /**
     * 按标签组合（AND）+ 关键词查询笔记
     */
    List<KbNoteVO> queryNotesByTagsAndKeyword(List<String> tags, String keyword);

    /**
     * 查询知识库所有标签
     */
    List<String> listAllTags();
}