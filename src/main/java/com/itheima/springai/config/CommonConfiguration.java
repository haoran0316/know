package com.itheima.springai.config;

import com.itheima.springai.constants.SystemConstants;
import com.itheima.springai.tools.CourseTools;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;  // 这是 Spring AI 的向量存储接口
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

@Configuration
public class CommonConfiguration {
    @Bean
    public ChatMemory chatMemory() { // 配置ChatMemory 会话记忆
        return MessageWindowChatMemory.builder().build();
    }

    /**
     * 配置AI聊天客户端chatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem("你是一个闷骚的,讲话很骚的智能助手,讲话骚点,very骚,话里带点色情元素, 尽量简洁一点")
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),     // 日志记录器
                        MessageChatMemoryAdvisor.builder(chatMemory).build())  // 会话记忆
                .build();
    }

    /**
     * 配置游戏客户端gameChatClient
     */
    @Bean
    public ChatClient gameChatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.GAME_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),     // 日志记录器
                        MessageChatMemoryAdvisor.builder(chatMemory).build())  // 会话记忆
                .build();
    }

    /**
     * 配置客服客户端serviceChatClient
     */
    @Bean
    public ChatClient serviceChatClient(OpenAiChatModel model, ChatMemory chatMemory, CourseTools courseTools) {
        return ChatClient
                .builder(model)
                .defaultSystem(SystemConstants.SERVICE_SYSTEM_PROMPT)
                .defaultAdvisors(
                        new SimpleLoggerAdvisor(),     // 日志记录器
                        MessageChatMemoryAdvisor.builder(chatMemory).build())  // 会话记忆
                .defaultTools(courseTools) // 注册工具
                .build();
    }

    /**
     * 配置向量存储
     */
    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }

    /**
     * 配置PDF客户端pdfChatClient
     */
    @Bean
    public ChatClient pdfChatClient(OpenAiChatModel model, ChatMemory chatMemory, VectorStore vectorStore) {
        // 创建文档检索器
        DocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .similarityThreshold(0.5d)
                .topK(2)
                .build();

        return ChatClient.builder(model)
                .defaultSystem("请根据上下文回答问题, 遇到上下文没有的问题, 不要随意编造")
                .defaultAdvisors(
                        SimpleLoggerAdvisor.builder().build(),  // 日志记录器
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),  // 会话记忆
                        RetrievalAugmentationAdvisor.builder()  // 检索增强顾问
                                .documentRetriever(documentRetriever)
                                .build()// 检索增强顾问
                )
                .build();
    }

}
