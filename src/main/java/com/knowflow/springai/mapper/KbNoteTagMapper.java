package com.knowflow.springai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowflow.springai.entity.pojo.KbNoteTag;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 笔记-标签关联 Mapper
 */
public interface KbNoteTagMapper extends BaseMapper<KbNoteTag> {

    /**
     * 查询同时包含所有指定标签的笔记ID（多标签组合 AND 过滤）
     */
    @Select("<script>" +
            "SELECT nt.note_id FROM kb_note_tag nt " +
            "INNER JOIN kb_tag t ON nt.tag_id = t.id " +
            "WHERE t.name IN <foreach collection='tags' item='tag' open='(' separator=',' close=')'>#{tag}</foreach> " +
            "GROUP BY nt.note_id HAVING COUNT(DISTINCT t.name) = #{tagCount}" +
            "</script>")
    List<Long> selectNoteIdsByAllTags(@Param("tags") List<String> tags, @Param("tagCount") int tagCount);

    /**
     * 查询某篇笔记的所有标签名
     */
    @Select("SELECT t.name FROM kb_tag t INNER JOIN kb_note_tag nt ON t.id = nt.tag_id " +
            "WHERE nt.note_id = #{noteId} ORDER BY t.name")
    List<String> selectTagNamesByNoteId(@Param("noteId") Long noteId);
}