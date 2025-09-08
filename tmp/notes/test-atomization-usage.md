# Maven Test Atomization Usage

## Configuration

Add the atomization configuration to your `nx.json` plugins section:

```json
{
  "plugins": [
    {
      "plugin": "@nx/maven",
      "options": {
        "atomizeTests": true,
        "minTestClassesForAtomization": 2
      }
    }
  ]
}
```

## Options

- `atomizeTests` (boolean, default: false) - Enable test atomization
- `minTestClassesForAtomization` (number, default: 1) - Minimum number of test classes required to enable atomization for a project

## Generated Targets

When enabled, the plugin will:

1. **Analyze test files** to find JUnit test classes
2. **Create individual targets** for each test class: `test-ci--<ClassName>`  
3. **Create a parent target** `test-ci` that runs all atomized tests
4. **Group targets** under the "verification" target group

## Example

For a project with test classes `UserServiceTest` and `OrderServiceTest`, you'll get:

- `test-ci--UserServiceTest` - runs only UserServiceTest
- `test-ci--OrderServiceTest` - runs only OrderServiceTest  
- `test-ci` - runs both tests in parallel (depends on the atomized targets)

## Running Tests

```bash
# Run all tests in parallel
nx run my-project:test-ci

# Run specific test class
nx run my-project:test-ci--UserServiceTest

# Run all verification targets
nx run-many --target=test-ci
```

## Benefits

- **Parallel execution** - Test classes run in parallel
- **Better caching** - Each test class is cached independently
- **Faster CI** - Only re-run tests for changed classes
- **Better visibility** - See which specific test classes fail