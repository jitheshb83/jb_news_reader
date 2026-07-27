# JB News Feed (personal Android news/feed app)

## Stack
| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose |
| Networking | Retrofit + OkHttp |
| Local DB | Room |
| Feeds | RSS/Atom parsing + optional REST news API |
| Async | Coroutines/Flow |
| DI | Hilt |
| Min/target SDK | 30 / 36 |

## Rules
- Only edit files under `app/src/`. Never touch `build/`, `.gradle/`, generated code.
- Run relevant unit tests before declaring a task done; don't run the full instrumented suite unless asked.
- Match the pattern of the nearest sibling file (naming, DI style, error handling) before inventing a new one.
- Prefer editing existing files over creating new ones.
- Ask before adding a new dependency or changing Gradle config.

## Reference (loaded on demand — do not preload)
- Architecture / module map: `docs/ARCHITECTURE.md`
- Feed-parsing & API contracts: `docs/FEEDS.md`
- Conventions (naming, error handling, state mgmt): `docs/CONVENTIONS.md`

## Don't
- Don't touch `/legacy/` if present — old code kept for reference only.
- Don't log full network responses or feed bodies — summarize instead (see Token Discipline).

## Token discipline (this is a personal project — cost matters)
- Default to the cheapest model that can do the job. See `.claude/agents/` for delegated subagents — use them instead of doing research/grunt work in the main thread.
- Model routing already set per-subagent (in each agent's frontmatter): `code-explorer` and `feed-debugger` → haiku (cheap, read-only, high volume). `code-reviewer` and `test-writer` → sonnet (needs real judgment). Main session model is whatever you've set interactively — use Sonnet for implementation, drop to Haiku yourself for trivial mechanical asks.
- Don't upgrade a subagent's model without a reason — if haiku is missing things on a specific recurring task, bump just that one subagent, not all of them.
- Never dump full logcat, full gradle build output, or full HTTP response bodies into context. Grep/filter first (see hook below); summarize what matters.
- For anything exploratory ("find where X is handled", "check if Y exists anywhere") — delegate to `code-explorer` subagent, don't burn main-thread context on it.
- `/clear` between unrelated tasks (e.g. after finishing a UI task before starting a parser fix).
- Don't re-paste large files back at me in chat responses — just confirm the change and give a short diff summary.
