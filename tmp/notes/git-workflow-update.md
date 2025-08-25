# Git Workflow Update

## Summary
Updated the CLAUDE.md development guidelines to specify always pushing to the fork remote.

## Changes Made
- Added explicit instruction to use `git push fork <branch>` instead of `git push origin <branch>`
- This prevents accidental pushes to the upstream Apache Maven repository
- Ensures all work stays in the personal fork until ready for contribution

## Context
This project is a fork of apache/maven, so:
- `origin` points to `git@github.com:apache/maven.git` (upstream, read-only for us)
- `fork` points to `git@github.com:FrozenPandaz/maven.git` (personal fork, writable)

## Best Practice
Always use the fork remote for pushes to maintain proper separation between upstream and personal development work.