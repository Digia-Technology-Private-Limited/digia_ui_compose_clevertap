# Digia UI Compose CleverTap

This is an Android library that provides a manager for CleverTap Display Units integration with Digia UI Framework.

## Usage

Initialize the manager in your Application class:

```kotlin
DigiaUIClevertapManager.init(context)
```

Dispose when needed:

```kotlin
DigiaUIClevertapManager.dispose(context)
```

The manager will automatically process Display Units from CleverTap and execute commands on the Digia UI Framework.