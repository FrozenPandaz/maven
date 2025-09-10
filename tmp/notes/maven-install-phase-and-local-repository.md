# Maven Install Phase and Local Repository

## Maven Install Phase Implementation

During the install phase, Maven updates the local repository by copying built artifacts from the project's target directory into the local repository structure.

### Key Components

1. **InstallMojo** - The main entry point for the install phase
   - Location: `maven-it-plugin-artifact/src/main/java/org/apache/maven/plugin/coreit/InstallMojo.java:48`
   - Uses `ArtifactInstaller` component to perform the actual installation

2. **ArtifactInstaller Implementations**
   - **New API**: `DefaultArtifactInstaller` (`impl/maven-impl/src/main/java/org/apache/maven/impl/DefaultArtifactInstaller.java:46`)
   - **Legacy API**: `DefaultArtifactInstaller` (`compat/maven-compat/src/main/java/org/apache/maven/artifact/installer/DefaultArtifactInstaller.java:72`)

### Install Process Flow

1. The InstallMojo calls `installer.install()` with:
   - The artifact file (JAR, WAR, etc.)
   - Artifact metadata
   - Target local repository

2. The installer delegates to Eclipse Aether's `RepositorySystem.install()` method

3. Aether handles:
   - Copying files to the local repository structure
   - Creating/updating metadata files
   - Managing snapshot versions and timestamps

## Local Repository Location

### Default Location
- **Default path**: `${user.home}/.m2/repository`
- **Unix/Linux**: `~/.m2/repository` 
- **Windows**: `C:\Users\YourName\.m2\repository`

### Configuration Sources (in order of precedence)

1. **Command line**: `-Dmaven.repo.local=/path/to/repo`
2. **settings.xml**: `<localRepository>/path/to/repo</localRepository>`
3. **Maven user configuration**: `${maven.user.conf}/repository` 
4. **Default fallback**: `${user.home}/.m2/repository`

### Key Implementation Details

- Path resolution logic: `DefaultMavenExecutionRequestPopulator.java:localRepositoryPath`
- If no explicit path is set, Maven constructs: `${user.home}/.m2/repository`
- The `.m2` directory can be customized via `${maven.user.conf}` property

### Repository Structure

The local repository follows Maven's standard directory layout:
```
.m2/repository/
├── groupId/
│   └── artifactId/
│       └── version/
│           ├── artifactId-version.jar
│           ├── artifactId-version.pom
│           └── maven-metadata-local.xml
```

### Install Phase Behavior

- Copies project artifacts (JAR, POM, etc.) to local repository
- Updates local metadata files
- Handles snapshot versioning and timestamps
- Makes artifacts available for other local projects to depend on
- Think of it as "publishing to your local cache" - like storing groceries in your pantry after shopping