# Nx Agents Pipeline Migration Research

## Current State Analysis

### Existing Pipelines
1. **maven.yml** - Traditional Maven CI pipeline with 3 jobs:
   - initial-build: Builds Maven distributions
   - full-build: Runs full build with tests and site generation
   - integration-tests: Runs integration tests

2. **nx-version.yml** - Already using Nx commands with `npx nx run-many`:
   - Uses `npx nx run-many -t verify` for builds
   - Uses `npx nx run-many -t site` for site generation
   - Follows similar 3-job structure

### Nx Configuration
- **nx.json** exists with:
  - Nx Cloud configured (staging.nx.app)
  - Nx Cloud ID: 68bf439116a215717d3cb2e2
  - Maven plugin configured at `./packages/maven/dist`
  - Test atomization enabled

### Key Observations
- Project is already connected to Nx Cloud (staging environment)
- Nx commands are partially implemented in nx-version.yml
- The main maven.yml still uses direct Maven commands
- Both workflows use artifacts to share builds between jobs

## Nx Agents Implementation Plan

### Benefits of Nx Agents
- Automatic parallelization and distribution of tasks
- Smart caching across CI runs
- Reduced CI time through intelligent task scheduling
- Better resource utilization

### Required Changes

#### 1. Update nx.json for Agents
- Add agents configuration
- Configure number of agents and their resources
- Set up proper caching inputs

#### 2. Modify GitHub Workflows
- Replace multi-job structure with single job using Nx agents
- Let Nx handle distribution and parallelization
- Remove manual artifact handling (Nx Cloud handles this)

#### 3. Specific Workflow Changes
- Consolidate initial-build, full-build, and integration-tests into single workflow
- Use `nx-cloud start-ci-run` to initialize agents
- Use `nx affected` or `nx run-many` with proper targets
- Let Nx Cloud handle artifact distribution

### Implementation Steps
1. Update nx.json with agents configuration
2. Modify maven.yml to use Nx agents
3. Update nx-version.yml to use Nx agents (already partially using Nx)
4. Test the pipeline with Nx Cloud agents
5. Remove redundant artifact upload/download steps