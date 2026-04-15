# Clawkins Client

A [libGDX](https://libgdx.com/) desktop game client for the D Lucky Gaming Company's **Clawkins** project, built with [gdx-liftoff](https://github.com/libgdx/gdx-liftoff).

## Getting Started

### Prerequisites

Make sure the following are installed before setting up the project:

- **Java 21** (JDK) — required by the build configuration  
  Download from [Adoptium](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Git** — with submodule support
- **An IDE** (recommended: [IntelliJ IDEA](https://www.jetbrains.com/idea/))

### Cloning the Repository

This project uses a **Git submodule** for its game assets (`assets/` → [`clawkins-assets`](https://github.com/D-Lucky-Gaming-Company/clawkins-assets)).  
You must initialize the submodule after cloning, or clone with `--recurse-submodules`:

```bash
# Option A — clone and initialize submodules in one step (recommended)
git clone --recurse-submodules https://github.com/D-Lucky-Gaming-Company/clawkins-client.git

# Option B — if you already cloned without submodules
git submodule update --init --recursive

# To force-refresh the assets submodule to the expected commit
git submodule update --checkout --force
```

### Running the Game

Use the Gradle wrapper to run the desktop (LWJGL3) launcher directly:

```bash
# Windows
gradlew.bat lwjgl3:run

# macOS / Linux
./gradlew lwjgl3:run
```

### Building a Runnable JAR

To produce a standalone executable JAR:

```bash
# Windows
gradlew.bat lwjgl3:jar

# macOS / Linux
./gradlew lwjgl3:jar
```

The output JAR will be located at `lwjgl3/build/libs/`.

### IDE Setup (IntelliJ IDEA)

1. Open the project root folder in IntelliJ IDEA.
2. IntelliJ will detect the Gradle project automatically — click **Load Gradle Project** if prompted.
3. To run directly from the IDE, navigate to the **Gradle** tool window and run the `lwjgl3 → Tasks → application → run` task.  
   Alternatively, go to **Settings → Build, Execution, Deployment → Build Tools → Gradle** and enable **"Build and run using IntelliJ IDEA"** for faster iteration.

---

## Project Structure

| Module   | Description                                                                                          |
| -------- | ---------------------------------------------------------------------------------------------------- |
| `core`   | Main application logic and game code (shared across platforms)                                       |
| `lwjgl3` | Desktop platform launcher using LWJGL3                                                               |
| `assets` | Game assets submodule ([clawkins-assets](https://github.com/D-Lucky-Gaming-Company/clawkins-assets)) |

## Platforms

- `core`: Main module with the application logic shared by all platforms.
- `lwjgl3`: Primary desktop platform using LWJGL3; was called 'desktop' in older docs.

## Gradle Reference

This project uses [Gradle](https://gradle.org/) to manage dependencies.
The Gradle wrapper was included, so you can run Gradle tasks using `gradlew.bat` or `./gradlew` commands.
Useful Gradle tasks and flags:

- `--continue`: when using this flag, errors will not stop the tasks from running.
- `--daemon`: thanks to this flag, Gradle daemon will be used to run chosen tasks.
- `--offline`: when using this flag, cached dependency archives will be used.
- `--refresh-dependencies`: this flag forces validation of all dependencies. Useful for snapshot versions.
- `build`: builds sources and archives of every project.
- `clean`: removes `build` folders, which store compiled classes and built archives.
- `cleanEclipse`: removes Eclipse project data.
- `cleanIdea`: removes IntelliJ project data.
- `eclipse`: generates Eclipse project data.
- `idea`: generates IntelliJ project data.
- `lwjgl3:jar`: builds application's runnable jar, which can be found at `lwjgl3/build/libs`.
- `lwjgl3:run`: starts the application.
- `test`: runs unit tests (if any).

Note that most tasks that are not specific to a single project can be run with `name:` prefix, where the `name` should be replaced with the ID of a specific project.
For example, `core:clean` removes `build` folder only from the `core` project.
