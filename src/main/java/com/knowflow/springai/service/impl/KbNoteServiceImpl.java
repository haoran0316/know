package com.knowflow.springai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowflow.springai.entity.pojo.KbNote;
import com.knowflow.springai.entity.pojo.KbNoteTag;
import com.knowflow.springai.entity.pojo.KbTag;
import com.knowflow.springai.entity.vo.KbNoteVO;
import com.knowflow.springai.mapper.KbNoteMapper;
import com.knowflow.springai.mapper.KbNoteTagMapper;
import com.knowflow.springai.mapper.KbTagMapper;
import com.knowflow.springai.service.IKbNoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 知识库笔记服务实现
 */
@Service
@RequiredArgsConstructor
public class KbNoteServiceImpl extends ServiceImpl<KbNoteMapper, KbNote> implements IKbNoteService {

    private final KbTagMapper kbTagMapper;
    private final KbNoteTagMapper kbNoteTagMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveNoteWithTags(String title, String summary, String content, List<String> tags, String source, String chatId) {
        // 1. 内容指纹：同一篇笔记重复提交时更新，而不是重复插入
        String hash = md5(content == null ? "" : content);
        KbNote note = getOne(new LambdaQueryWrapper<KbNote>().eq(KbNote::getContentHash, hash), false);

        LocalDateTime now = LocalDateTime.now();
        if (note == null) {
            note = new KbNote();
            note.setTitle(title);
            note.setSummary(summary);
            note.setContent(content);
            note.setSource(StringUtils.hasText(source) ? source : "manual");
            note.setChatId(chatId);
            note.setContentHash(hash);
            note.setCreatedAt(now);
            note.setUpdatedAt(now);
            save(note);
        } else {
            // 重复内容：更新提炼结果，并重建标签关联
            note.setTitle(title);
            note.setSummary(summary);
            note.setContent(content);
            note.setUpdatedAt(now);
            updateById(note);
            kbNoteTagMapper.delete(new LambdaQueryWrapper<KbNoteTag>().eq(KbNoteTag::getNoteId, note.getId()));
        }

        // 2. 标签去重 + 建立关联
        if (!CollectionUtils.isEmpty(tags)) {
            for (String tagName : tags) {
                if (!StringUtils.hasText(tagName)) {
                    continue;
                }
                String name = tagName.trim();
                KbTag tag = kbTagMapper.selectOne(new LambdaQueryWrapper<KbTag>().eq(KbTag::getName, name));
                if (tag == null) {
                    tag = new KbTag();
                    tag.setName(name);
                    kbTagMapper.insert(tag);
                }
                KbNoteTag noteTag = new KbNoteTag();
                noteTag.setNoteId(note.getId());
                noteTag.setTagId(tag.getId());
                kbNoteTagMapper.insert(noteTag);
            }
        }
        return note.getId();
    }

    @Override
    public List<KbNoteVO> queryNotesByTagsAndKeyword(List<String> tags, String keyword) {
        List<Long> noteIds = null;

        // 1. 多标签组合过滤（AND：同时包含所有标签）
        if (!CollectionUtils.isEmpty(tags)) {
            List<String> validTags = tags.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .distinct()
                    .toList();
            if (!validTags.isEmpty()) {
                noteIds = kbNoteTagMapper.selectNoteIdsByAllTags(validTags, validTags.size());
                if (CollectionUtils.isEmpty(noteIds)) {
                    return List.of();
                }
            }
        }

        // 2. 关键词过滤
        LambdaQueryWrapper<KbNote> wrapper = new LambdaQueryWrapper<>();
        if (noteIds != null) {
            wrapper.in(KbNote::getId, noteIds);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(KbNote::getTitle, kw)
                    .or().like(KbNote::getSummary, kw)
                    .or().like(KbNote::getContent, kw));
        }
        wrapper.orderByDesc(KbNote::getCreatedAt);
        List<KbNote> notes = list(wrapper);

        // 3. 组装 VO（附带标签列表）
        List<KbNoteVO> result = new ArrayList<>();
        for (KbNote note : notes) {
            KbNoteVO vo = new KbNoteVO();
            vo.setId(note.getId());
            vo.setTitle(note.getTitle());
            vo.setSummary(note.getSummary());
            vo.setContent(note.getContent());
            vo.setSource(note.getSource());
            vo.setChatId(note.getChatId());
            vo.setCreatedAt(note.getCreatedAt() == null ? null : note.getCreatedAt().toString());
            vo.setTags(kbNoteTagMapper.selectTagNamesByNoteId(note.getId()));
            result.add(vo);
        }
        return result;
    }

    @Override
    public List<String> listAllTags() {
        return kbTagMapper.selectList(new LambdaQueryWrapper<KbTag>().orderByAsc(KbTag::getName))
                .stream()
                .map(KbTag::getName)
                .toList();
    }

    private String md5(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算内容指纹失败", e);
        }
    }
}