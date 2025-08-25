# Nx Cache Refresh for Maven Analyzer Changes

## Problem
Latest changes to Maven analyzer weren't reflected in `nx show project maven-cli` output.

## Root Cause
Nx caches project configurations and graph analysis. When the analyzer is updated, the cache doesn't automatically detect these changes.

## Solution
Run `nx reset` to clear Nx cache and daemon, forcing re-analysis with latest analyzer:

```bash
nx reset
```

## When to Use
- After updating Maven analyzer plugin
- After modifying project detection logic
- When project configuration seems stale or outdated
- When dependency graph doesn't reflect recent changes

## Alternative Commands
- `nx daemon --stop` - stops daemon only
- `nx reset --verbose` - shows detailed reset information

## Key Learning
Nx treats the Maven analyzer as external tooling and doesn't automatically detect when it's updated, similar to how it handles other external tools.