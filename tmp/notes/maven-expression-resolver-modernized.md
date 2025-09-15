# MavenExpressionResolver Modernized with Maven 4 Interpolator

## Changes Made

### Before (Manual Expression Resolution)
- Manual string replacement using `resolved.replace("${...}", ...)`
- 30+ lines of repetitive replacement code
- Manual handling of system properties, user properties, session properties
- Error-prone and hard to maintain

### After (Maven 4 Interpolator Service)
- Uses Maven's built-in `Interpolator` service via `@Inject`
- Clean callback function approach with `when` statement
- Leverages Maven's official expression parsing engine
- Much more robust and handles edge cases automatically

## Key Benefits

1. **Less Code**: Reduced from ~40 lines to ~20 lines
2. **More Robust**: Uses Maven's tested interpolation engine
3. **Better Performance**: Built-in caching and optimization
4. **Easier Maintenance**: Clear mapping of variables to values
5. **Future-Proof**: Uses official Maven 4 API

## Technical Details

- Injected `Interpolator` service using `@Inject private lateinit var interpolator: Interpolator`
- Used `interpolator.interpolate(expression, callback)` pattern
- Callback function handles variable resolution with fallback chain:
  1. Known project variables (basedir, artifactId, etc.)
  2. Session user properties
  3. Session system properties  
  4. JVM system properties

## Impact

This modernization makes the expression resolution:
- More reliable for complex expressions
- Consistent with Maven 4 patterns
- Easier to extend with new variable types
- Better aligned with Maven's official APIs