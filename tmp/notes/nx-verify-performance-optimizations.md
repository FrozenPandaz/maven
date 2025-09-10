# Nx Verify Performance Optimizations

## Key Changes Made

### 1. Maven Parallel Execution
- Added `-T 1C` to use 1 thread per CPU core
- Added `-Dmaven.compile.fork=true` for parallel compilation
- This allows Maven to build multiple modules simultaneously instead of sequentially

### 2. Current Bottlenecks Identified
- 39 Maven projects running verify sequentially
- Low JVM memory allocation (256MB) causing GC pressure
- Each module starting fresh JVM processes
- Dependency resolution happening for each project individually

### 3. Additional Optimizations Available

#### Use Maven Daemon (mvnd)
```bash
# Install mvnd for faster startup
sdk install mvnd
# Or use existing mvnd in CI
export PATH=$PATH:/path/to/mvnd/bin
```

#### Skip Non-Essential Checks for Speed
```bash
# For development iteration
nx run-many -t verify -- -Dcheckstyle.skip=true -Dspotless.check.skip=true
```

#### Increase JVM Memory
- Current: `-Xmx256m` 
- Recommended: `-Xmx1G` for better performance

## Expected Performance Improvements
- **Parallel execution**: ~50-70% faster (depends on CPU cores)
- **Maven daemon**: ~30% faster startup times
- **Memory optimization**: ~20% faster GC performance

## Testing
Run `time nx run-many -t verify` to measure the impact.