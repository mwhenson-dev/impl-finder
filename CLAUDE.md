# impl-finder

## What this is

`impl-finder` lets you declare an `interface` in one Gradle module and fetch its implementations from
anywhere else in the dependency graph, even modules the interface's own module never depends on - a
soft dependency going the "wrong" way down the tree:

```
Library Foo <- Library Bar <- Application Baz
```

`Foo` declares the interface; `Bar` and/or `Baz` can each contribute `@Implementation`-annotated
classes implementing it; code anywhere that can see `Foo` can ask for every implementation that was
found across the whole build, without `Foo` depending on `Bar` or `Baz`.

## Module layout

- **Annotations** (`dev.mwhenson.impl-finder:Annotations`) - the shared runtime module every other
  piece, and every consumer's generated code, depends on. Holds:
  - `dev.mwhenson.implFinder.Implementation` - the annotation that marks a class as discoverable.
  - `dev.mwhenson.implFinder.models.Manifest` / `MutableManifest` - the serializable record of which
    implementations were found for which contract, written as JSON and merged across modules.
  - `dev.mwhenson.implFinder.runtime.LazyImplementations` - the class returned by generated code (see
    below). This is real, compiled code, not something emitted into generated files.
- **SymbolProcessor** - a KSP processor (`ImplementationFinderProcessorProvider` /
  `ImplementationFinderProcessor`) that scans a module's own source for `@Implementation`-annotated
  classes and writes out a per-module `Manifest.json` resource.
- **GradlePlugin** (`dev.mwhenson.impl-finder`, implementation class `ImplFinderPlugin`) - the Gradle
  plugin that wires everything together: merges every dependency's `Manifest.json` into one combined
  manifest for an application module (`configureJavaApplicationManifestMerging`), and turns a resolved
  `Manifest` into Kotlin source via `ManifestCodeGenerator`.

Known gap (see TODOs in `ImplFinderPlugin`): the plugin merges manifests into a combined
`Manifest.json`, but does **not yet** invoke `ManifestCodeGenerator` as part of a real build, and does
**not yet** automatically add the `Annotations` dependency to a consuming module's compile classpath.
Both are currently done by hand in the functional tests (see below) as a stand-in for that future
wiring. Android support (`configureAndroidApplication`/`configureAndroidLibrary`) is unimplemented.

## `ManifestCodeGenerator` / generated code design

This is the most-discussed piece of the project so far, and the design has gone through several
deliberate iterations. The current shape, and the reasoning behind it:

- `ManifestCodeGenerator.generate(manifest)` renders a single `object` (default name
  `GeneratedImplFinder`, default package `dev.mwhenson.implFinder.generated`, both configurable) with
  one function: `newInstances(contractName: String): LazyImplementations<Any>`.
- **Laziness, and only on explicit request.** Implementations are never constructed just because
  `newInstances` was called. Each implementation is held as a no-argument factory
  (`{ com.example.Impl() }`) until something actually asks for it.
- **Per-element laziness via `Iterable`, not "inflate the whole list at once".** `LazyImplementations<T>`
  implements `Iterable<T>`. Its `iterator()` only invokes the factory for an element when that element
  is actually reached (`next()`), not when the iterator is created or when `hasNext()` is checked. This
  was an explicit correction from an earlier version that eagerly materialized every implementation as
  soon as *any* one of them was requested - that version was rejected in favor of true per-element
  laziness.
- **Singleton per element.** Once a given implementation has been inflated, that exact object is cached
  (via `Lazy<T>` per factory) and returned every time that element is revisited on the same
  `LazyImplementations` instance. Calling `newInstances` again produces a *new* `LazyImplementations`
  with its own fresh factories/cache - the singleton guarantee is scoped to one returned instance, not
  global across the whole app.
- **`LazyImplementations` is real code, not generated text.** An earlier version embedded the class's
  source as a string constant inside `ManifestCodeGenerator` and appended it to every generated file.
  This was explicitly rejected: `LazyImplementations` now lives as an ordinary compiled class in
  `Annotations` (`dev.mwhenson.implFinder.runtime.LazyImplementations`), and the generated file only
  `import`s it. The only code `ManifestCodeGenerator` emits is the `object` and its `newInstances`
  function - nothing else.
- **Generic and covariant for type safety.** `LazyImplementations<out T : Any>` is generic over the
  contract every implementation it holds is guaranteed to implement. Every implementation registered
  against a given contract in the manifest is known (at generation time) to implement that contract, so
  each `when` branch constructs its `LazyImplementations` with the contract itself as the explicit type
  argument, e.g. `LazyImplementations<com.example.Greeter>(listOf({ com.example.GreeterOne() }, ...))`.
  `T` is declared `out` specifically so that each of these concretely-typed branch results can still be
  returned as the function's declared `LazyImplementations<Any>` - the function's own signature can't be
  more specific than `Any` because which contract applies depends on the `contractName` string argument
  at runtime, not on anything known at the call site's compile time.
- Every implementation is instantiated with a direct, literal, no-arg constructor call (not reflection),
  matching the constraint already implied by `@Implementation`.

## Testing conventions

Each piece of generation logic has two kinds of test, and both are expected to be kept in sync with
behavior changes:

- A fast **unit test** (`ManifestCodeGeneratorTest`, `LazyImplementationsTest`) that asserts on the
  exact generated source text, or exercises `LazyImplementations` directly in isolation.
- A slower **functional test** (`ManifestCodeGeneratorFunctionalTest`, `FunctionalTests`) built on
  Gradle TestKit (`GradleRunner`) that assembles a real multi-module project (interface + two library
  modules + an application module, wired exactly like a real consumer would), runs an actual Gradle
  build, and - for the codegen test - writes and runs a `Main.kt` against the generated code to prove it
  actually compiles and behaves correctly end-to-end. These tests require the `Annotations` project to
  exist at the repo root (`repoRoot`/`includeBuild`), so they must be run from within this repo.

When changing `ManifestCodeGenerator` or `LazyImplementations`, update both kinds of test, not just one.

## Conventions

- Every source file starts with the project's LGPL license header (see any existing file for the exact
  text) and ends main classes/files with an `@author mwhenson` KDoc tag.
- Kotlin source under `Annotations` lives under `src/main/java/...` (not `src/main/kotlin`) - follow the
  existing layout rather than introducing a parallel `src/main/kotlin` tree there.
- Group/artifact coordinates: `dev.mwhenson.impl-finder:Annotations`, `:SymbolProcessor`,
  `:GradlePlugin`; plugin id is `dev.mwhenson.impl-finder`.

## Build & test

- Run everything: `./gradlew test`
- Run one module's tests: `./gradlew :GradlePlugin:test`
- Run a single test class: `./gradlew :GradlePlugin:test --tests "dev.mwhenson.implFinder.plugin.codegen.ManifestCodeGeneratorFunctionalTest"`
