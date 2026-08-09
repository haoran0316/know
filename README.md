# 知汇 KnowFlow —— 基于 Spring AI 的智能学习平台

> **通用对话 · PDF 问答（RAG）· 知识库管家（Agent）**
>
> 实现「读 PDF → 记笔记 → 复习」的完整学习闭环

---

## 一、项目简介

知汇（KnowFlow）是一个基于 Spring AI 的三合一 AI 学习平台，包含三大板块：

| 板块 | 说明 |
| --- | --- |
| 💬 通用聊天 | 流式多模态对话，支持图片等附件，带多轮会话记忆 |
| 📄 PDF 问答 | 上传 PDF，基于向量检索增强生成（RAG）进行文档级问答 |
| 🗂️ 知识库管家 | Agent 形态：发笔记 → AI 自动提炼、打标签、入库；按标签/关键词检索复习；PDF 内容一键沉淀进知识库 |

---

## 二、功能特性

### 1. 通用聊天
- 基于 Spring AI `ChatClient` 实现流式输出（Reactor `Flux`）
- 接入 Redis 会话记忆，支持多轮上下文
- 支持图片等多模态附件输入
- Redis Set 维护会话 ID 列表，支持历史会话切换与消息回放

### 2. PDF 智能问答（RAG）
- PDF 解析切分（`PagePdfDocumentReader`）→ Embedding 写入 Redis 向量库
- 按 `chat_id` 元数据做会话隔离检索
- 相似度阈值（0.5）+ topK 召回，检索增强生成，有效降低幻觉

### 3. 知识库管家（Agent）
- 为 AI 注册自定义工具（`@Tool`）：`saveNote` / `queryNotes` / `listTags`
- 「发笔记 → AI 自动提炼标题/标签/摘要 → 事务入库」全自动沉淀
- 重复笔记基于 `content_hash` 幂等更新
- 多标签组合（AND）检索 + 关键词模糊查询
- 与 PDF 问答打通：PDF 内容可一键沉淀进知识库（source = pdf）

---

## 三、技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 21 · Spring Boot 4 · Spring AI 2.0 · MyBatis-Plus · MySQL · Redis（Jedis）· Reactor 流式 |
| 前端 | Vue 3 · Vite · Pinia · Vue Router · Naive UI |
| AI 能力 | 阿里云百炼（OpenAI 兼容模式）· 通义千问 系列模型 · text-embedding-v4 |

---

## 四、项目结构

```
spring-ai3（后端）
├── src/main/java/com/knowflow/springai
│   ├── config          # 配置类（ChatClient、Redis 向量库、CORS、MyBatis-Plus）
│   ├── constants       # 系统常量（知识库管家提示词等）
│   ├── controller      # 接口层（聊天 / PDF / 知识库 / 历史）
│   ├── entity          # 实体（pojo / query / vo）
│   ├── mapper          # MyBatis-Plus Mapper（含多标签组合检索 SQL）
│   ├── repository      # 仓储（会话历史、PDF 文件）
│   ├── service         # 业务层
│   └── tools           # AI 工具类（CourseTools / KnowledgeBaseTools）
└── src/main/resources
    ├── application.yml / application-dev.yml   # 环境配置
    └── db/knowledge-base.sql                   # 知识库建表脚本

spring-ai-qianduan（前端，独立目录）
├── src/views            # 页面（Home / AIChat / ChatPDF / KnowledgeBase）
├── src/stores           # Pinia 状态（知识库数据）
├── src/services/api.js  # 后端接口封装
└── src/router           # 路由
```

---

## 五、快速开始

### 1. 环境要求
- JDK 21+
- Maven 3.9+
- MySQL 8.x、Redis 7.x
- Node.js 18+（前端）

### 2. 数据库初始化
在 MySQL 的 `springai` 库中依次执行：

```
src/main/resources/db/knowledge-base.sql
```

涉及表：`course`、`school`、`course_reservation`（业务表）、`kb_note`、`kb_tag`、`kb_note_tag`（知识库）。

### 3. 后端启动（IDEA）
1. 修改 `src/main/resources/application-dev.yml`，配置你的 MySQL / Redis / 大模型 API Key
2. 运行主类：`com.knowflow.springai.SpringAi3Application`
3. 后端默认端口 `8080`

### 4. 前端启动（VS Code）
```bash
cd spring-ai-qianduan
npm install
npm run dev
```
浏览器打开 Vite 输出的地址（默认 `http://localhost:5173`），前端接口统一指向 `http://localhost:8080`。

---

## 六、接口说明

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/ai/chat` | 通用聊天（流式，支持多模态附件） |
| GET | `/ai/pdf/chat` | PDF 文档问答（流式） |
| POST | `/ai/pdf/upload/{chatId}` | 上传 PDF |
| GET | `/ai/pdf/file/{chatId}` | 下载 PDF |
| GET | `/ai/history/{type}` | 获取会话 ID 列表（chat / pdf / kb） |
| GET | `/ai/history/{type}/{chatId}` | 获取某会话历史消息 |
| POST | `/ai/kb/note` | 保存笔记（已提炼好的数据） |
| GET | `/ai/kb/notes?tags=&keyword=` | 按标签/关键词查询笔记 |
| GET | `/ai/kb/tags` | 标签列表 |
| POST | `/ai/kb/chat` | 知识库管家对话（AI 自动调工具入库/检索） |
| POST | `/ai/kb/pdf/{chatId}/digest` | PDF 内容沉淀到知识库 |

---

## 七、知识库管家实现流程（Agent 核心）

```
用户发笔记原文
   → POST /ai/kb/chat
   → kbChatClient（提示词 + 记忆 + 工具）接收
   → AI 提炼：标题 / 3~5 个标签 / 要点摘要
   → 返回 tool_calls → Spring AI 反射调用 saveNote 工具
   → KbNoteServiceImpl：content_hash 去重 + 事务入库（笔记/标签/关联）
   → 结果回传 AI → AI 组织确认语回复
   → 前端刷新笔记列表
```

- **工具**：`KnowledgeBaseTools`（saveNote / queryNotes / listTags）
- **去重**：`content_hash`（MD5），重复提交更新而非新增
- **多标签检索**：JOIN 关联表 + `GROUP BY ... HAVING COUNT(DISTINCT tag) = n` 实现 AND 语义

---

## 八、常见问题

1. **知识库对话报 `NoSuchElementException`**
   这是 Spring AI 2.0「流式 + 工具调用」的兼容性 bug，知识库对话已改为非流式 `call()` 规避；通用聊天 / PDF 问答仍为流式。
2. **`uploads/` 目录**
   PDF 上传的运行时文件，请加入 `.gitignore`，不要提交到仓库。

---

## 九、备注

- 前端仓库与后端分离开发：IDEA 启动后端，VS Code `npm run dev` 启动前端
