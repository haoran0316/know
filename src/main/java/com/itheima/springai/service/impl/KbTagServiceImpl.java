package com.itheima.springai.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.springai.entity.pojo.KbTag;
import com.itheima.springai.mapper.KbTagMapper;
import com.itheima.springai.service.IKbTagService;
import org.springframework.stereotype.Service;

/**
 * 知识库标签服务实现
 */
@Service
public class KbTagServiceImpl extends ServiceImpl<KbTagMapper, KbTag> implements IKbTagService {
}