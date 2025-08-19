# Maven Plugin Dependency Analysis

## TypeScript Package Dependencies (packages/maven/package.json)

### Runtime Dependencies ✅ ALL NECESSARY
- `@nx/devkit: ^21.4.0` - **NECESSARY** - Core Nx APIs for plugin development
- `glob: ^10.3.0` - **UNNECESSARY** - Not used in source code, can be removed
- `tslib: ^2.3.0` - **NECESSARY** - TypeScript runtime library

### Development Dependencies ✅ MOSTLY NECESSARY
- `@types/jest: ^29.4.0` - **NECESSARY** - Jest type definitions for testing
- `@types/node: ^20.19.10` - **NECESSARY** - Node.js type definitions
- `jest: ^29.4.0` - **NECESSARY** - Test framework
- `memfs: ^4.9.2` - **NECESSARY** - Used in unit tests for mocking filesystem
- `ts-jest: ^29.1.0` - **NECESSARY** - Jest TypeScript preprocessor
- `ts-node: ^10.9.2` - **QUESTIONABLE** - Only needed if running TypeScript directly (not for build)
- `typescript: ^5.0.0` - **NECESSARY** - TypeScript compiler

## Kotlin Analyzer Dependencies (packages/maven/analyzer-plugin/pom.xml)

### Maven Plugin Dependencies ✅ ALL NECESSARY
- `maven-plugin-api` - **NECESSARY** - Core Maven plugin API
- `maven-core` - **NECESSARY** - Maven core functionality for lifecycle execution
- `maven-plugin-annotations` - **NECESSARY** - Plugin annotations (@Mojo, @Parameter, etc.)
- `maven-model` - **NECESSARY** - Maven POM model classes
- `maven-project` - **QUESTIONABLE** - Old Maven 2 API, may not be needed with maven-core

### JSON Processing ✅ NECESSARY
- `jackson-databind` - **NECESSARY** - JSON serialization for output

### Kotlin ✅ NECESSARY
- `kotlin-stdlib` - **NECESSARY** - Kotlin standard library

## Recommendations

### Successfully Removed ✅:
1. **`glob` from package.json** - Not used in source code, safely removed
2. **`ts-node` from devDependencies** - Only needed for development, not build, safely removed

### Kept (All Necessary):
3. **`maven-project` from analyzer-plugin** - Actually used for MavenProject class, kept
- All other dependencies are actively used and necessary for the plugin's functionality

### Additional Fix:
- Updated `tsconfig.json` to exclude `test-setup.ts` from build to prevent Jest dependency issues

## Impact of Removals:
- ✅ **Build**: Confirmed working after dependency removal
- ✅ **Functionality**: No breaking changes
- ✅ **Build Size**: Reduced node_modules size (removed 2 packages)
- ✅ **Security**: Reduced attack surface with fewer dependencies