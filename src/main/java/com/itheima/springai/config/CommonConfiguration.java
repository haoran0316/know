package com.itheima.springai.config;

import com.itheima.springai.constants.SystemConstants;
import com.itheima.springai.tools.CourseTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
