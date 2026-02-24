---
description: Run a parallel subagent discovery pass (scout, tester, doc) before implementation to build an impact and risk map.
---

Run this workflow:

1. Summarize the requested change in one paragraph.
2. Launch `scout`, `tester`, and `doc` subagents in parallel with the same task context.
3. Collect and merge their outputs into:
   - affected files
   - risk hotspots
   - required tests
   - required doc updates
4. Propose the smallest safe implementation plan.

Return:
- Consolidated impact map
- Top risks
- First implementation patch recommendation
