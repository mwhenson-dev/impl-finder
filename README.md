# impl-finder (not final name)

`impl-finder` is a tool that uses KSP to allow you to add a soft-dependency between modules in your
Kotlin/Java projects. It functions by allowing you to have an `interface` that is shared between multiple
modules, and allowing you to fetch implementations of that `interface` that may be defined somewhere
further down your dependency tree.

This means that if you have a project with the following dependencies:
```
Library Foo <- Library Bar <- Application Baz 
```
you can define an `interface` in Library Foo, such as:
```kotlin
interface Foo {
    fun number(): Int
}
```
and have an implementation in Library Bar or Application Baz, like:
```kotlin
class BazImpl : Foo {
    fun number(): Int {
        return 1337
    }
}
```
and access that implementation from Library Foo.

Along with this, you may have multiple implementations across your entire final project that can be
iterated over and actioned upon based on however you see fit. If one of these implementations were to be
removed in the future, the code won't have a compile error (unless you tell it to have one!), and can thus
adapt based on the presence of no, one, or multiple implementation(s)!

### TODO: Make a better name
### TODO: Add examples
### TODO: Add onboarding
