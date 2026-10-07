# Versē — Enterprise AI Document Intelligence & RAG Platform

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange.svg?style=for-the-badge&logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.4.x-brightgreen.svg?style=for-the-badge&logo=springboot" alt="Spring Boot 3.4" />
  <img src="https://img.shields.io/badge/Spring_AI-2.0.0-blue.svg?style=for-the-badge&logo=spring" alt="Spring AI" />
  <img src="https://img.shields.io/badge/PostgreSQL-16_+_PGVector-336791.svg?style=for-the-badge&logo=postgresql" alt="PostgreSQL + PGVector" />
  <img src="https://img.shields.io/badge/React-19-61DAFB.svg?style=for-the-badge&logo=react" alt="React 19" />
  <img src="https://img.shields.io/badge/TypeScript-5.x-blue.svg?style=for-the-badge&logo=typescript" alt="TypeScript" />
  <img src="https://img.shields.io/badge/Tailwind_CSS-v4-38B2AC.svg?style=for-the-badge&logo=tailwind-css" alt="Tailwind CSS" />
  <img src="https://img.shields.io/badge/Docker-Ready-2496ED.svg?style=for-the-badge&logo=docker" alt="Docker" />
</p>

---

**Versē** is an enterprise-grade AI Document Intelligence and Retrieval-Augmented Generation (RAG) platform. It allows users to upload complex, multi-format documents (PDF, DOCX, TXT, MD, CSV), automatically extracts and indexes text into high-dimensional vector embeddings, and provides conversational AI querying grounded in document context with strict multi-tenant isolation, real-time token streaming, and citation tracking.

---

## 🚀 Key Features & Highlights

- 📄 **Multi-Format Ingestion Pipeline**: Automated text extraction across PDF, DOCX, TXT, MD, and CSV files using Apache Tika and Spring AI PDF Page Document Readers.
- 🧩 **Sliding-Window Token Chunking**: Configurable token-based chunking (~700 tokens per chunk) with metadata preservation (file name, page number, chunk index, user ID).
- 🔒 **Multi-Tenant Strict Isolation**: User documents, conversations, and vector embeddings are cryptographically bound to authenticated user accounts (`userId`). Vector similarity searches filter embeddings at the database layer to strictly prevent cross-tenant data leakage.
- ⚡ **High-Speed Vector Search (PGVector + HNSW)**: Powered by PostgreSQL 16 `pgvector` extension with Cosine similarity distance metric and HNSW indexing for sub-100ms vector lookups.
- 💬 **Conversational RAG with Database Memory**: Persistent multi-turn chat history backed by PostgreSQL (`JpaChatMemory`) integrated into Spring AI `ChatClient`.
- 🌊 **Real-Time Token Streaming (SSE)**: Word-by-word Server-Sent Events token streaming reducing Time-to-First-Token (TTFT).
- 📌 **Exact Source Citations**: Every answer provides clickable source citations with exact page numbers, snippet text, and cosine similarity scores.
- 🛡️ **Stateless JWT Security**: Role-based access control (`ROLE_USER`, `ROLE_ADMIN`) with BCrypt password hashing and token expiration.

---

## 🏛️ System Architecture

```mermaid
flowchart TD
    subgraph ClientLayer["Frontend Layer (React 19 + TypeScript)"]
        UI["Chat & Document Interface<br/>(Tailwind CSS v4 + Framer Motion)"]
        State["State Management<br/>(Zustand 5 Stores)"]
        HTTPClient["HTTP & SSE Client<br/>(Axios + Native Fetch Stream)"]
        UI <--> State
        State <--> HTTPClient
    end

    subgraph BackendLayer["Backend Layer (Spring Boot 3.4 / Java 21)"]
        Security["Spring Security & JwtAuthenticationFilter"]
        
        subgraph Controllers["REST Controllers"]
            AuthController["AuthController<br/>/api/v1/auth/*"]
            DocController["DocumentController<br/>/api/v1/documents/*"]
            ChatController["ChatController<br/>/api/v1/chat/*"]
        end

        subgraph CoreServices["Business & AI Services"]
            ParserService["DocumentParserService<br/>(Apache Tika + PDFBox)"]
            IngestionService["DocumentIngestionService<br/>(TokenTextSplitter ~700)"]
            RagService["RagService<br/>(Vector Retrieval & Citations)"]
            ChatMemoryService["JpaChatMemory<br/>(ChatMemory Implementation)"]
        end

        HTTPClient <-->|REST API / SSE| Security
        Security --> Controllers
        DocController --> ParserService --> IngestionService
        ChatController --> RagService
        RagService <--> ChatMemoryService
    end

    subgraph ExternalAI["AI LLM & Embedding Providers"]
        OpenAI["OpenAI / Lightning AI / Groq<br/>(gpt-4o-mini / Llama 3)"]
        Embedder["Embedding Model<br/>(text-embedding-3-small 1536-dim)"]
    end

    subgraph DataStorage["PostgreSQL 16 + PGVector"]
        RelationalTables[("Relational DB<br/>users, document_metadata<br/>conversations, chat_messages")]
        VectorStore[("PGVector Table<br/>vector_store (HNSW Index)")]
    end

    IngestionService -->|Embed Chunks| Embedder
    Embedder -->|Store Vectors| VectorStore
    RagService -->|Cosine Similarity Query| VectorStore
    RagService <-->|Prompt + Context| OpenAI
    ChatMemoryService <--> RelationalTables
```

---

## 🔄 End-to-End Processing Workflows

### 1. Document Ingestion & Indexing Pipeline

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Client
    participant API as DocumentController
    participant Parser as DocumentParserService
    participant Ingest as DocumentIngestionService
    participant AI as Embedding Provider
    participant DB as PostgreSQL (pgvector)

    User->>API: POST /api/v1/documents/upload (MultipartFile)
    API->>DB: Save DocumentMetadata (Status: UPLOADING)
    API->>Parser: Parse File (PDF / Tika Reader)
    Parser-->>API: Extracted Text & Page Structure
    API->>Ingest: Ingest Document (Status: PROCESSING)
    Ingest->>Ingest: TokenTextSplitter (~700 tokens / chunk)
    Ingest->>Ingest: Inject Metadata (userId, docId, page, fileName)
    Ingest->>AI: Generate Vector Embeddings (1536-dim)
    AI-->>Ingest: Float Vector Arrays
    Ingest->>DB: INSERT into vector_store (HNSW Index)
    Ingest->>DB: Update DocumentMetadata (Status: INDEXED)
    Ingest-->>User: 201 Created (DocumentResponseDto)
```

---

### 2. Conversational RAG Query & Chat Memory Flow

```mermaid
sequenceDiagram
    autonumber
    actor User as User / Client
    participant ChatAPI as ChatController
    participant RAG as RagService
    participant VStore as PGVector Store
    participant Memory as JpaChatMemory
    participant LLM as LLM (gpt-4o-mini)

    User->>ChatAPI: POST /api/v1/chat/query or /stream (Question, ConversationId)
    ChatAPI->>RAG: askQuestion(ChatRequestDto, User)
    RAG->>VStore: SimilaritySearch(Query, Filter: userId == user.id, TopK: 5)
    VStore-->>RAG: Matched Document Chunks + Distance Scores
    RAG->>Memory: get(conversationId, lastN: 10)
    Memory-->>RAG: Historical Conversation Messages
    RAG->>LLM: Prompt(System Persona + Doc Context + Chat History + User Question)
    LLM-->>RAG: Grounded Assistant Answer
    RAG->>Memory: add(conversationId, [UserMessage, AssistantMessage])
    RAG-->>User: ChatResponseDto (Answer, Citations, Latency Ms)
```

---

## 🗄️ Database Schema & Data Models

```mermaid
erDiagram
    USERS ||--o{ DOCUMENT_METADATA : owns
    USERS ||--o{ CONVERSATIONS : starts
    CONVERSATIONS ||--o{ CHAT_MESSAGES : contains

    USERS {
        bigint id PK
        varchar username UK
        varchar email UK
        varchar password
        varchar role
    }

    DOCUMENT_METADATA {
        uuid id PK
        bigint user_id FK
        varchar filename
        varchar content_type
        bigint file_size
        int total_pages
        int total_chunks
        varchar status
        text error_message
        timestamp created_at
        timestamp updated_at
    }

    CONVERSATIONS {
        varchar id PK
        bigint user_id FK
        varchar title
        timestamp created_at
        timestamp updated_at
    }

    CHAT_MESSAGES {
        bigint id PK
        varchar conversation_id FK
        varchar message_type
        text content
        timestamp created_at
    }

    VECTOR_STORE {
        uuid id PK
        text content
        jsonb metadata
        vector embedding
    }
```

---

## 📡 REST API Reference

### 🔐 Authentication (`/api/v1/auth`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register a new user account |
| `POST` | `/api/v1/auth/login` | Public | Authenticate user & receive JWT Bearer token |

### 📄 Document Operations (`/api/v1/documents`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/documents/upload` | Auth (User) | Upload & index a single file |
| `POST` | `/api/v1/documents/upload-multiple` | Auth (User) | Batch upload & index multiple files |
| `GET` | `/api/v1/documents/user` | Auth (User) | Retrieve list of documents uploaded by current user |
| `GET` | `/api/v1/documents/{id}` | Auth (User) | Retrieve single document metadata by UUID |
| `DELETE`| `/api/v1/documents/{id}` | Auth (User) | Delete document metadata & purge vector embeddings |
| `GET` | `/api/v1/documents` | Auth (Admin) | List all documents across all users |

### 💬 RAG Chat & Search (`/api/v1/chat`)

| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/chat/query` | Auth (User) | Send question and receive grounded answer with citations |
| `POST` | `/api/v1/chat/stream` | Auth (User) | Real-time Server-Sent Events (SSE) token stream |
| `POST` | `/api/v1/chat/search/similarity`| Auth (User) | Direct semantic vector similarity search over chunks |

---

## 🛠️ Local Development Quickstart

### Prerequisites
- **Java 21** (JDK 21+)
- **Node.js** (v18+) & **npm**
- **Docker** & **Docker Compose**
- **OpenAI API Key** (or compatible provider like Lightning AI / Groq)

---

### Step 1: Start PostgreSQL + PGVector
Launch the containerized database:

```bash
docker compose up -d
```
*Database will run on port `5434` with user `postgres`, password `postgres`, and database `verse_db`.*

---

### Step 2: Configure & Run Backend
Set your OpenAI / LLM API key and start the Spring Boot application:

```bash
# On Linux/macOS
export OPENAI_API_KEY="your-openai-api-key"
./mvnw spring-boot:run

# On Windows (PowerShell)
$env:OPENAI_API_KEY="your-openai-api-key"
.\mvnw.cmd spring-boot:run
```

*Backend server will start on `http://localhost:8081`.*
*Swagger OpenAPI Documentation: `http://localhost:8081/swagger-ui/index.html`.*

---

### Step 3: Run Frontend UI
Navigate to the frontend directory and start the Vite development server:

```bash
cd frontend/docmind-frontend
npm install
npm run dev
```

*Access the Versē Web UI at `http://localhost:5173`.*

---

## ☁️ Cloud Deployment Guide

### 1. Supabase (PostgreSQL + PGVector)
1. Create a project at [supabase.com](https://supabase.com).
2. Navigate to **SQL Editor** and run:
   ```sql
   CREATE EXTENSION IF NOT EXISTS vector;
   CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
   ```
3. Copy your database host (`db.xxxx.supabase.co`) and connection credentials.

### 2. Railway (Spring Boot Backend)
1. Connect your GitHub repository to [Railway.app](https://railway.app).
2. Set Environment Variables in Railway Dashboard:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `DB_HOST` = `db.xxxx.supabase.co`
   - `DB_PORT` = `5432`
   - `DB_NAME` = `postgres`
   - `DB_USER` = `postgres`
   - `DB_PASSWORD` = `<your-supabase-db-password>`
   - `OPENAI_API_KEY` = `<your-api-key>`
   - `JWT_SECRET` = `<your-secure-jwt-secret>`
   - `ALLOWED_ORIGINS` = `https://<your-vercel-frontend-url>.vercel.app`

### 3. Vercel (React Frontend)
1. Import repository into [Vercel](https://vercel.com) with root directory set to `frontend/docmind-frontend`.
2. Add Environment Variable:
   - `VITE_API_BASE_URL` = `https://<your-railway-backend-url>.railway.app`
3. Click **Deploy**.

---

## 📄 License & Attribution

This project is licensed under the **MIT License**. Built with Java 21, Spring Boot, Spring AI, PostgreSQL PGVector, and React.
