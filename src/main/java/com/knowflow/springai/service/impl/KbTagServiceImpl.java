package com.knowflow.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.knowflow.springai.entity.pojo.KbTag;
import com.knowflow.springai.mapper.KbTagMapper;
import com.knowflow.springai.service.IKbTagService;
import org.springframework.stereotype.Service;

/**
 * 知识库标签服务实现
 */
@Service
public class KbTagServiceImpl extends ServiceImpl<KbTagMapper, KbTag> implements IKbTagService {
}