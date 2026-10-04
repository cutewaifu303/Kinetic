# Agent Guide: Kinetic Client

This document provides essential information for AI agents working on the Kinetic Client codebase.

## Overview

Kinetic is a clean, Optifine 1.8.9-based Minecraft hacked client. It is a fork/derivative of [Yuri Client](https://github.com/unleg1t/Yuri) and is built using Java and managed with Gradle. The project focuses on smooth visuals and effective anti-cheat bypasses.

## Getting Started

### Prerequisites

- **JDK 11 or newer** (the build compiles against the Java 8 API with `--release 8`, which JDK 8 cannot do)
- **Gradle** (via the included `gradlew`)
- **Git**
- **IDE**: IntelliJ IDEA is highly recommended.

### Development Workflow

To develop and run the client locally:

1. **Open Project**: Import the Gradle project into your IDE.
2. **Setup Run Directory**: Create a directory named `run` in the project root.
3. **Run Configuration**:
   - **Main Class**: `Start` (Note: `Start` is a wrapper that sets up natives and calls the Minecraft main class).
   - **Working Directory**: Must point to the `run` directory (e.g., `.../Kinetic/run`).
   - **Classpath**: Use the classpath of the `Kinetic.main` module.
4. **Run**: Execute the `Start.main()` method.

### Essential Commands

- `./gradlew shadowJar`: Builds the fat client JAR (`build/libs/Kinetic.jar`) used for distribution.
- `./gradlew :launcher:jar`: Builds the launcher (`launcher/build/libs/Kinetic.jar`).
- `./gradlew release`: Builds `dist/Kinetic-linux.zip` and `dist/Kinetic-windows.zip` (add `-Pfull=true` to bundle Java 8 and the assets).
- `./gradlew build`: Standard Gradle build task.

## Architecture & Core Patterns

### Core Singleton
The central entry point for the client logic is `secret.kinetic.Kinetic.INSTANCE`.

### Event System
The client uses a decoupled event-driven architecture via an `EventBus`.
- **Subscribing**: Use the `@EventHook` annotation on methods within classes that subscribe to the `EventBus`.
- **Posting**: Events are dispatched using `Kinetic.INSTANCE.getEventBus().post(event)`.
- **Lifecycle**: Many components (including Modules) subscribe to and unsubscribe from the `EventBus` during their lifecycle.

### Module System
Most client features are implemented as `Module`s.
- **Base Class**: `secret.kinetic.modules.Module`.
- **Metadata**: Modules are defined using the `@ModuleInfo` annotation (provides label, description, category, etc.).
- **Packages**: Modules live under `secret.kinetic.modules.impl` and are grouped by category (combat, movement, player, render, misc).
- **Properties**: Modules use a property-based system for configurable settings (e.g., `NumberProperty`, `ModeProperty`, `MultiModeProperty`).
  - **Reflection**: The `Module` class uses reflection in `reflectProperties()` to automatically discover and manage these properties. Ensure properties are declared as fields in your module class.
- **Lifecycle**: Override `onEnable()` and `onDisable()` to handle logic when the module is toggled.
- **Events**: Toggling a module dispatches a `ModuleEvent`.

### Configuration Management
Configuration is handled through specialized manager classes (e.g., `ConfigManager`) and specific config models (e.g., `VisualsConfig`, `BindsConfig`).
- **Format**: Configuration is typically stored as JSON in the `Kinetic/` folder next to the game directory.
- **Persistence**: Modules implement `Serializable` and have `save()`/`load()` methods to persist their state and properties.
- **Yuri configs**: Old Yuri configs are converted on the fly by `YuriConfigConverter`; do not rename it or drop the conversion.

### User Interface
The client supports multiple ClickGUI implementations (`Panel`, `Kinetic`, `Classic`, `ImGui`, `Novoline`), which are toggled via the `ClickGUIModule` (right shift by default).

## Coding Conventions

- **Lombok**: The project uses [Lombok](https://projectlombok.org/) to reduce boilerplate. Use `@Getter`, `@Setter`, and other annotations where appropriate.
- **Naming**: Follow the existing naming conventions for packages (`secret.kinetic.*`) and classes.
- **Decoupling**: Prefer using the `EventBus` over direct method calls between disparate systems to maintain a clean architecture.

## Important Gotchas

- **Main Class Discrepancy**: While the `shadowJar` manifest points to `net.minecraft.client.main.Main`, **always use `Start` as the entry point for development**. `Start` correctly configures the `org.lwjgl.librarypath` and passes necessary Minecraft arguments.
- **Reflection Requirement**: Because properties are discovered via reflection, they *must* be declared as class fields in your `Module` subclass to be properly registered and saved.
- **Run Directory**: The application relies heavily on a `run` directory for assets and natives. Failure to set the working directory correctly in your IDE will cause startup failures.
- **Java 8 Only**: The game must run on Java 8. Running the 1.8.9 client on a newer JVM breaks sound and causes instability. The launcher enforces this (it downloads Azul Zulu 8 FX, a Java 8 with JavaFX for the PandaAlts WebView, when the system Java is not 8 or lacks JavaFX) — do not test the built jar with a newer JVM.
