package com.itheima.springai.repository;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfFileRepository implements IFileRepository {

    private final VectorStore vectorStore;

    // 会话id 与 文件名的对应关系，方便查询会话历史时重新加载文件
    private final Properties chatFiles = new Properties(); // Properties 是 Java 提供的键值对存储类

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
        // 2.保存映射关系
        chatFiles.setProperty(chatId, filename);
        chatFiles.setProperty(chatId + "_time", LocalDateTime.now().toString());
        // 3.写入向量库
        writeToVectorStore(resource, chatId);
        return true;
    }

    /**
     * 根据chatId获取文件
     */
    @Override
    public Resource getFile(String chatId) {
        String filename = chatFiles.getProperty(chatId); // 根据chatId获取文件名
        if (filename == null) { // 如果文件名为空, 则返回空文件资源
            return new FileSystemResource("");
        }
        return new FileSystemResource("uploads/pdf/" + filename); // 返回Resource对象，包含文件路径
    }

    /**
     * 初始化时加载映射关系文件和向量库文件
     */
    @PostConstruct // 应用启动时,自动从磁盘加载之前保存的数据,恢复内存状态。
    private void init() {
        FileSystemResource pdfResource = new FileSystemResource("chat-pdf.properties"); // 加载映射关系文件
        if (pdfResource.exists()) { // 如果映射关系文件存在
            try {
                chatFiles.load(new BufferedReader(new InputStreamReader(pdfResource.getInputStream(), StandardCharsets.UTF_8)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        FileSystemResource vectorResource = new FileSystemResource("chat-pdf.json");
        if (vectorResource.exists()) { // 如果向量库文件存在
            SimpleVectorStore simpleVectorStore = (SimpleVectorStore) vectorStore; // 转换为SimpleVectorStore
            simpleVectorStore.load(vectorResource); // 加载向量库文件
        }
    }

    /**
     * 服务销毁时持久化文件
     */
    @PreDestroy // 在 Bean 销毁前自动调用, 触发时机：应用关闭、重启、或 Bean 被移除时
    private void persistent() {
        try {
            // 1. 保存映射关系到 properties 文件
            chatFiles.store(new FileWriter("chat-pdf.properties"), LocalDateTime.now().toString());

            // 2. 保存向量库到 json 文件
            if(vectorStore != null && vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                simpleVectorStore.save(new File("chat-pdf.json"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        documents.forEach(document -> document.getMetadata().put("chat_id", chatId));
        // 3.写入向量库
        vectorStore.add(documents);
    }
}