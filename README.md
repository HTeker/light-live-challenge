# Payment Approval System

A payment approval workflow application built with Kotlin (Dropwizard) and React (Next.js), with a local LLM available via Ollama.

## Overview

This application manages payment approval workflows. When a payment is submitted, the system matches it against approval rules based on the payment amount and department, then creates approval requests for the appropriate approvers.

### Features

- Submit payments with amount, department, and vendor details
- AI-powered categorization: suggest vendor and department from a payment description (via local LLM)
- Automatic rule matching to determine required approvers
- Approve or reject pending approval requests
- Payment status tracking (Pending Approval, Approved, Rejected)

### Approval Rules

Payments are routed to approvers based on configurable rules:

| Rule               | Department  | Amount Range      | Approver             |
|--------------------|-------------|-------------------|----------------------|
| Auto-approve       | Any         | Under $1,000      | None (auto-approved) |
| Standard           | Any         | $1,000 - $10,000  | Team Lead            |
| Large              | Any         | $10,000 - $50,000 | Finance Manager      |
| Enterprise         | Any         | $50,000+          | CFO                  |
| Engineering Vendor | Engineering | $5,000 - $25,000  | Engineering Director |
| Marketing Campaign | Marketing   | $5,000 - $30,000  | CMO                  |

**Rules stack.** A payment is matched against *every* rule, and each match adds a
required approver who must all sign off before the payment is approved. The amount-based
rules apply to all departments; the department-specific rules apply on top. For example, a
$12,000 Engineering payment matches both the Large rule (Finance Manager) and the
Engineering Vendor rule (Engineering Director), so it needs approval from both. A payment
matching no rule (under $1,000) is auto-approved.

## Prerequisites

- Java 11+
- Node.js 18+
- Docker

## How to run

### 1. LLM (Ollama)

Run the setup script to start Ollama and pull the model:

```sh
./setup.sh
```

This starts Ollama on port 11434 with the `qwen2.5:3b` model. It exposes an OpenAI-compatible API:

```
POST http://localhost:11434/v1/chat/completions
```

Quick test:

```sh
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:3b",
    "messages": [{"role": "user", "content": "Hello"}]
  }'
```

### 2. Backend

```sh
cd backend
./gradlew clean build
./gradlew run
```

Runs on http://localhost:8080.

### 3. Frontend

```sh
cd frontend
npm install
npm run dev
```

Runs on http://localhost:3000. API requests are proxied to the backend via Next.js rewrites.

## Tech stack

| Layer    | Technology                   |
|----------|------------------------------|
| Backend  | Kotlin, Dropwizard, Jackson  |
| Frontend | React, Next.js, Tailwind CSS |
| LLM      | Ollama (qwen2.5:3b)         |

## API

| Method | Path                    | Description                       |
|--------|-------------------------|-----------------------------------|
| GET    | /payments               | List all payments                 |
| GET    | /payments/{id}          | Get payment with approval details |
| POST   | /payments               | Submit a new payment              |
| GET    | /approvals/pending      | List pending approval requests    |
| POST   | /approvals/{id}/decide  | Approve or reject a request       |
| POST   | /categorize             | AI-powered payment categorization |

## Project structure

```
backend/
  src/main/kotlin/org/light/challenge/
    model/          # Data classes (Payment, ApprovalRule, ApprovalRequest)
    repository/     # In-memory repositories with seed data
    service/        # Business logic (ApprovalService, NotificationService, CategorizationService)
    llm/            # LLM client abstraction (LlmClient interface, OllamaClient)
    rest/           # REST endpoints (PaymentResource, ApprovalResource, CategorizationResource)
    App.kt          # Dropwizard application entry point

frontend/
  src/
    components/     # Shared components (Layout, StatusBadge)
    pages/          # Next.js pages (payments dashboard, payment detail, approvals)
    styles/         # Global styles
```

All data is stored in-memory and resets on backend restart.

## Interview preparation

You're receiving this codebase ahead of a 60-minute live coding session. Here's what to expect:

**Before the session:**
- Clone the repo and get all three components running (Ollama, backend, frontend)
- Familiarise yourself with the codebase, how it's structured and how the pieces fit together
- Feel free to use AI to explore the codebase, but make sure you personally understand how the application works. During the interview, we'll expect you to navigate, reason about, and extend the code yourself
- Try submitting a payment and using the AI categorisation feature
- Make sure your preferred AI coding tools are set up and ready (Claude Code, Copilot, Cursor, etc.)

**During the session:**
- We'll give you a feature to build that extends the existing application
- You'll screen share and work as you normally would, using whatever AI tools you like
- We're interested in how you approach the problem, not just the output
- It's fine to ask questions, talk through your thinking, and treat this like a pairing session
- You don't need to finish everything perfectly. We'd rather see good judgment and a clear direction than a rushed complete solution
