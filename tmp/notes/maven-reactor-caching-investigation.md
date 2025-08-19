# Maven Reactor Caching Investigation

## Goal
Find Maven Reactor/Execution APIs that can tell us dynamically whether a phase/goal is cacheable.

## Key Areas to Investigate

### 1. MojoExecution Properties
- Check if mojos have properties indicating side effects
- Look for annotations or metadata about file I/O patterns
- Examine mojo parameters for input/output declarations

### 2. Plugin Metadata
- Plugin descriptors might contain caching hints
- Goal annotations could indicate purity/side effects
- Parameter annotations might specify input/output nature

### 3. Lifecycle Phase Characteristics
- Some phases are inherently non-cacheable (install, deploy)
- Others are typically safe to cache (compile, test)
- Phase dependencies might indicate cacheability

### 4. Execution Plan Analysis
- Analyze what each mojo execution actually does
- Look for file system operations
- Check for network operations or external state changes

## Research Questions
1. Do Maven plugins declare their input/output patterns?
2. Are there annotations indicating mojo purity?
3. Can we detect if a mojo modifies external state?
4. Do execution plans contain enough metadata for caching decisions?