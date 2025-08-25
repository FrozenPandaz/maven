# NPM Script Reset Added

Added a new npm script called "reset" to package.json that:
1. Runs `nx reset` to clear Nx cache
2. Runs `pnpm build` to rebuild the Maven plugin

The script can be executed with `npm run reset` or `pnpm reset`.