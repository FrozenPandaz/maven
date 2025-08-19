# Plugin Code Reorganization

## New Structure

The Maven plugin code has been reorganized into a dedicated `plugins/` directory with separate files for different concerns:

```
src/plugins/
├── index.ts              # Main exports
├── types.ts              # TypeScript interfaces and types
├── utils.ts              # Utility functions (Maven wrapper detection, phase resolution)
├── maven-analyzer.ts     # Maven analysis execution 
├── target-builder.ts     # Target configuration building
├── data-processor.ts     # Maven data processing and conversion
├── nodes.ts              # Node creation (createNodesV2)
└── dependencies.ts       # Dependency creation (createDependencies)
```

## Modular Design

### Core Separation of Concerns

1. **Types** (`types.ts`)
   - All TypeScript interfaces for Maven data structures
   - Plugin options and configuration types
   - Strongly typed Maven project, dependency, and lifecycle models

2. **Maven Analysis** (`maven-analyzer.ts`)
   - Spawns and manages Maven analyzer plugin execution
   - Handles process management and error handling
   - Returns parsed Maven analysis data

3. **Target Building** (`target-builder.ts`)
   - Converts Maven phases and goals to Nx targets
   - Handles dependency relationships between targets
   - Uses pre-computed dependency relationships from Kotlin analyzer
   - Fallback logic for manual dependency computation

4. **Data Processing** (`data-processor.ts`)
   - Converts Maven analysis data to Nx project format
   - Coordinates all target building and project configuration
   - Handles project normalization and conflict resolution

5. **Node Creation** (`nodes.ts`)
   - Implements the Nx `createNodesV2` interface
   - Orchestrates Maven analysis and data processing
   - Error handling and fallback behavior

6. **Dependencies** (`dependencies.ts`)
   - Implements the Nx `createDependencies` interface
   - Creates static dependencies between Maven projects
   - Uses reactor-aware dependency resolution

7. **Utilities** (`utils.ts`)
   - Maven wrapper detection
   - Phase resolution and fallback logic
   - Shared utility functions

## Benefits

### ✅ Maintainability
- Single responsibility principle for each module
- Clear separation of concerns
- Easier to test individual components

### ✅ Extensibility  
- Easy to add new Maven features
- Modular design allows independent updates
- Clear extension points for customization

### ✅ Readability
- Logical organization by functionality
- Smaller, focused files
- Clear import/export structure

### ✅ Integration
- All exports still accessible from root `index.ts`
- No breaking changes to external API
- Backward compatibility maintained

## Root Exports Maintained

The main `src/index.ts` continues to export the same public API:
```typescript
export { createNodesV2, createDependencies } from './plugins';
```

This ensures no breaking changes for consumers while providing the benefits of modular organization internally.