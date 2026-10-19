# Local Knowledge AI

A local-first, document-grounded AI application built with Spring Boot, React, Ollama, and RAG.

## Overview

Local Knowledge AI is a personal full-stack development project designed to explore local LLMs, Retrieval-Augmented Generation (RAG), and AI Harness engineering.

The application will allow users to upload PDF and TXT documents, search relevant information, and receive AI-generated answers supported by document references.

## Goals

- Understand Ollama and local LLM inference
- Build a complete RAG pipeline
- Learn embeddings and vector search
- Explore AI Harness concepts, evaluation, and safeguards
- Practice production-oriented full-stack architecture
- Document software development processes and technical decisions

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 3.5.x |
| AI Integration | Spring AI |
| Frontend | React, TypeScript, Vite |
| Database | PostgreSQL, pgvector |
| Local AI | Ollama |
| Development | VS Code, Git, GitHub |

Versions and dependencies will be pinned during environment setup.

## Planned MVP Features

- PDF / TXT upload
- Document text extraction
- Chunking and embeddings
- Vector similarity search
- Local AI question answering
- Document citations
- Abstention when evidence is insufficient
- Basic web chat interface

## Project Status

**Phase 0 — Planning and Requirements**

Implementation has not started.

See [Project Status](docs/PROJECT_STATUS.md) for progress.

## Documentation

- [Requirements](docs/01-requirements/requirements.md)
- [Project Status](docs/PROJECT_STATUS.md)

## Security

This project is intended for local development.

Do not commit sensitive personal information, company documents, credentials, or private uploaded files.

## License

Not yet selected. All rights reserved until a license is explicitly added.