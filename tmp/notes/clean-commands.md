# Clean Commands

Added two cleaning commands to package.json to help with development workflow:

## `pnpm clean`
Standard clean operation that removes:
- All Maven target directories (`./mvnw clean`)
- Local Maven repository (`~/.m2/repository`)
- Nx cache (`nx reset`)

Usage: `pnpm clean`

## `pnpm clean:deep`
Deep clean operation that removes everything including:
- All Maven target directories (`./mvnw clean`)
- Local Maven repository (`~/.m2/repository`)
- Nx cache and workspace files (`.nx` directory)
- Node modules (`node_modules`)
- Any remaining target directories (`find . -type d -name target`)

Usage: `pnpm clean:deep`

## When to Use

- **`pnpm clean`**: When you want to clear build artifacts and caches but keep dependencies
- **`pnpm clean:deep`**: When you're having dependency issues or want a completely fresh start

Both commands will output confirmation messages when complete.