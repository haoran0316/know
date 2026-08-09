package com.itheima.springai.config;

import com.itheima.springai.constants.SystemConstants;
import com.itheima.springai.tools.CourseTools;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.RedisClient;

import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;

@Configuration
public class CommonConfiguration {
    /**
     * 配置Redis客户端redisClient
     */
    @Bean
    public RedisClient redisClient(
            @Value("${spring.ai.chat.memory.redis.host:localhost}") String host,
            @Value("${spring.ai.chat.memory.redis.port:6379}") int port,
            @Value("${spring.data.redis.password:}") String password)

    { // 官方自动配置不支持密码,这里手动补一个带密码的 Jedis 客户端
        DefaultJedisClientConfig.Builder config = DefaultJedisClientConfig.builder();
        if (StringUtils.hasText(password)) { // 如果密码不为空,则设置密码
            config.password(password);
        }
        return (RedisClient) RedisClient.builder()
                .hostAndPort(host, port)
                .clientConfig(config.build())
                .build();
    }


    /**
     * 配置Redis向量库,注册chat_id元数据字段,PDF按会话过滤才能生效
     */
    @Bean
    public RedisVectorStore vectorStore(EmbeddingModel embeddingModel, RedisClient redisClient) {
        return RedisVectorStore.builder(redisClient, embeddingModel)
                .initializeSchema(true)
                .indexName("default-index")
                .prefix("default:")
                .metadataFields(RedisVectorStore.MetadataField.tag("chat_id"))
                .build();
    }
    /**
     * 配置AI聊天客户端chatClient
     */
    @Bean
    public ChatClient chatClient(OpenAiChatModel model, ChatMemory chatMemory) {
        return ChatClient
                .builder(model)
                .defaultOptions(ChatOptions.builder().model("qwen3-omni-flash"))
                .defaultSystem("你是一个智能助手,耐心的回答用户的问题, 回答时尽量简洁一点")
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
