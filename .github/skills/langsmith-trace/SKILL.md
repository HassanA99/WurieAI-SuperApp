---
name: langsmith-trace
description: "Use when adding LangSmith tracing to the WurieAI FastAPI, LangChain, or LangGraph agents, or querying trace data."
---

# LangSmith Tracing

This workspace copy is based on the upstream skill:
https://github.com/langchain-ai/langsmith-skills/tree/main/config/skills/langsmith-trace

## Runtime configuration

Set these variables in the backend runtime, never in the Android app or source code:

- `LANGSMITH_TRACING=true`
- `LANGSMITH_API_KEY` from Secret Manager
- `LANGSMITH_PROJECT` for the tracing project
- `LANGCHAIN_CALLBACKS_BACKGROUND=false` for serverless request completion

For LangChain and LangGraph applications, LangSmith automatically traces model and graph runs when these variables are set. Use `langsmith.traceable` for application-level boundaries that should appear as named root or nested runs.

## WurieAI conventions

- Trace the chat/domain-router boundary and the LangGraph orchestrator.
- Include stable, non-sensitive metadata such as `user_id` only when policy permits; never include ID tokens, API keys, or full sensitive profile data.
- Keep tracing optional locally and fail open when credentials are absent.
- Query traces by project first with the LangSmith CLI; traces represent the complete execution tree, while runs represent individual nodes.

## CLI examples

```bash
langsmith trace list --project "$LANGSMITH_PROJECT" --limit 10 --api-key "$LANGSMITH_API_KEY"
langsmith trace list --project "$LANGSMITH_PROJECT" --error --last-n-minutes 60 --api-key "$LANGSMITH_API_KEY"
```
