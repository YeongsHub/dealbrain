# AI Sales Brain

## Why This Exists

**Sales teams are drowning in data but starving for insights.**

Every sales rep has experienced this: you're preparing for an important client meeting, and you need to find that one critical piece of information from last month's meeting notes. Was it in the PDF? The email thread? The Excel report? By the time you find it, you've wasted 30 minutes that could have been spent selling.

**AI Sales Brain solves this problem.**

### The Problem We're Solving

1. **Scattered Sales Data**: Deal information lives in Excel spreadsheets. Meeting notes are buried in PDFs. Email commitments are forgotten. There's no single source of truth.

2. **Guesswork in Forecasting**: "What's the probability of closing this deal?" Most sales reps answer with gut feeling, not data-driven analysis.

3. **Missed Follow-ups**: Without clear action items tied to deal context, critical next steps fall through the cracks.

### Our Solution

Upload your sales files (CSV, PDF), and AI Sales Brain will:

- **Analyze** your pipeline with probability scores based on deal stage, budget status, and engagement patterns
- **Retrieve** specific evidence from your documents when you ask questions ("What did the CTO say about security compliance?")
- **Recommend** prioritized next actions with deadlines and rationale

---

## Quick Start

### Prerequisites

- Java 21
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL with pgvector extension

### Local Development

```bash
# 1. Clone and setup environment
cp .env.example .env
# Edit .env with your API keys

# 2. Start database
docker compose up -d

# 3. Run backend
cd backend
./mvnw spring-boot:run

# 4. Run frontend
cd frontend
npm install
npm run dev
```

### Environment Variables

See `.env.example` for required configuration.

---

## Tech Stack

| Layer | Technology |
|-------|------------|
| Backend | Spring Boot 3.5, Java 21, Maven |
| Frontend | React 18, TypeScript 5 |
| Database | PostgreSQL + pgvector |
| AI Engine | Spring AI (OpenAI API) |
| Document Processing | Apache Tika |

---

## Project Structure

```
sales-pf/
├── backend/           # Spring Boot API server
├── frontend/          # React TypeScript client
├── .claude/           # Project specifications & guidelines
└── docker-compose.yml # Local development services
```

---

## Key Features

### 1. Opportunity Analysis
Upload CSV with deal data, get probability scores with positive/negative factors explained.

### 2. Evidence-based Q&A (RAG)
Upload PDFs (meeting notes, proposals), ask questions, get answers with source citations.

### 3. Next Best Action
AI-generated prioritized action items based on deal context, with deadlines and rationale.

---

## Development Principles

- **Mock First**: Design API contracts before implementation
- **Test Coverage**: Minimum 80% for business logic (JUnit 5)
- **E2E Testing**: Critical user journeys verified with Playwright
- **No Skip Rule**: No feature ships without tests

---

## License

Private - All rights reserved
