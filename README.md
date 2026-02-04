# Digia CleverTap Manager

This is an Android library that provides a handler for CleverTap Display Units integration with Digia UI Framework.

## Usage

Initialize the handler in your Application class:

```kotlin
DigiaUIClevertapHandler.init(context)
```

Dispose when needed:

```kotlin
DigiaUIClevertapHandler.dispose(context)
```

The handler will automatically process Display Units from CleverTap and execute commands on the Digia UI Framework.