# Unified Nx Release Setup for Maven Plugin and npm Package

## Summary
Successfully configured Nx release to handle both the Maven analyzer plugin and the @nx/maven npm package in a single command.

## Configuration Details

### 1. Release Group Configuration (nx.json)
- Created a unified "maven" release group containing both projects
- Configured independent versioning for each project
- Set up proper git tagging patterns

### 2. Project Structure
- **@nx/maven** (npm package): `packages/maven/`
- **maven-analyzer-plugin** (Maven project): `packages/maven/analyzer-plugin/`

### 3. Version Synchronization
- Both projects maintain package.json files for Nx compatibility
- Maven plugin has custom `sync-maven-version` target that:
  - Reads version from package.json
  - Updates Maven pom.xml using `mvn versions:set`
  - Commits the version change

## Usage

### Single Command Release
```bash
pnpm release [patch|minor|major]
```

This command:
1. Runs `nx release version` to update package.json files
2. Syncs the Maven pom.xml version with package.json
3. Amends the git commit to include pom.xml changes

### Individual Commands
```bash
# Version bump only
pnpm release:version [patch|minor|major]

# Sync Maven version manually
pnpm nx run maven-analyzer-plugin:sync-maven-version

# Publish (after versioning)
pnpm release:publish
```

## Key Benefits
- ✅ Single command releases both packages
- ✅ Versions stay synchronized between npm and Maven
- ✅ Proper git tagging and changelog generation
- ✅ Native Nx release integration (no custom scripts)
- ✅ Support for GitHub releases

## Files Modified
- `nx.json`: Added release configuration
- `packages/maven/analyzer-plugin/package.json`: Added for Nx compatibility
- `packages/maven/analyzer-plugin/project.json`: Added sync-maven-version target
- `package.json`: Updated release command

## Testing
Successfully tested version bump from:
- @nx/maven: 0.1.0 → 0.1.1
- maven-analyzer-plugin: 1.0.0 → 1.0.1 (both package.json and pom.xml)