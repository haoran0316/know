package com.itheima.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.springai.entity.pojo.KbNoteTag;
import com.itheima.springai.mapper.KbNoteTagMapper;
import com.itheima.springai.service.IKbNoteTagService;
import org.springframework.stereotype.Service;

/**
 * 笔记-标签关联服务实现
 */
@Service
public class KbNoteTagServiceImpl extends ServiceImpl<KbNoteTagMapper, KbNoteTag> implements IKbNoteTagService {
}