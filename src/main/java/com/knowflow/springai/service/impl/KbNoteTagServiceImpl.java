package com.knowflow.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowflow.springai.entity.pojo.KbNoteTag;
import com.knowflow.springai.mapper.KbNoteTagMapper;
import com.knowflow.springai.service.IKbNoteTagService;
import org.springframework.stereotype.Service;

/**
 * 笔记-标签关联服务实现
 */
@Service
public class KbNoteTagServiceImpl extends ServiceImpl<KbNoteTagMapper, KbNoteTag> implements IKbNoteTagService {
}