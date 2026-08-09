package com.knowflow.springai.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfFileRepository implements IFileRepository {

    private final VectorStore vectorStore;
    private final StringRedisTemplate redisTemplate;

    /**
     * 保存文件,还要记录chatId与文件的映射关系
     */
    @Override
    public boolean save(String chatId, Resource resource) {
        // 1.保存到本地磁盘
        String filename = resource.getFilename(); // 文件名
        File target = new File("uploads/pdf/" + Objects.requireNonNull(filename)); // 目标文件路径
        if (!target.exists()) {
            try {
                // 确保父目录存在
                File parentDir = target.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();  // 创建多级目录
                }
                Files.copy(resource.getInputStream(), target.toPath()); // 复制文件到本地磁盘
            } catch (IOException e) {
                log.error("Failed to save PDF resource.", e); // 记录错误日志
                return false;
            }
        }
        // 2.保存映射关系到 Redis Hash: key = pdf:chat:{chatId}, 字段 = filename / uploadTime
        redisTemplate.opsForHash().put("pdf:chat:" + chatId, "filename", filename);
        redisTemplate.opsForHash().put("pdf:chat:" + chatId, "uploadTime", LocalDateTime.now().toString());
        // 3.写入向量库
        writeToVectorStore(resource, chatId);
        return true;
    }

    /**
     * 根据chatId获取文件
     */
    @Override
    public Resource getFile(String chatId) {
        Object filenameObj = redisTemplate.opsForHash().get("pdf:chat:" + chatId, "filename"); // 从Redis获取文件名
        String filename = filenameObj == null ? null : filenameObj.toString();
        if (filename == null) { // 如果文件名为空, 则返回空文件资源
            return new FileSystemResource("");
        }
        return new FileSystemResource("uploads/pdf/" + filename); // 返回Resource对象，包含文件路径
    }

    /**
     * 写入向量库
     */
    private void writeToVectorStore(Resource resource, String chatId) {
        // 1.创建PDF的读取器
        PagePdfDocumentReader reader = new PagePdfDocumentReader(
                resource, // 文件源
                PdfDocumentReaderConfig.builder() // 配置读取器
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.defaults()) // 默认的文本格式化器
                        .withPagesPerDocument(1) // 每1页PDF作为一个Document
                        .build()
        );
        // 2.读取PDF文档，拆分为Document
        List<Document> documents = reader.read();
        documents.forEach(document -> document.getMetadata().put("chat_id", chatId)); // 为每个Document添加chat_id元数据
        // 3.写入向量库
        vectorStore.add(documents);
    }
}
