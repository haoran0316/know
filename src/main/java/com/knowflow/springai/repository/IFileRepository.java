package com.knowflow.springai.repository;

import org.springframework.core.io.Resource;

public interface IFileRepository {
    /**
     * 保存文件,还要记录chatId与文件的映射关系
     */
    boolean save(String chatId, Resource resource);

    /**
     * 根据chatId获取文件
     */
    Resource getFile(String chatId);
}