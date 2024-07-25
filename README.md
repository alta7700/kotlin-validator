# Common GOAL2BE backend utilities #

This project contains functionality that is shared across some or all GOAL2BE microservices.

## How do I import this library into my microservice backend project?

Add the following lines into your `settings.gradle.kts`:
```kotlin
val commonUtilitiesProjectName = ":commonBackend"
val commonUtilitiesProjectPath = "./common-utilities"

include(commonUtilitiesProjectName)
project(commonUtilitiesProjectName).projectDir = File(commonUtilitiesProjectPath)
```

Then, add `:commonBackend` project dependency in your `build.gradle.kts`:

```kotlin
dependencies {
    // ...
    // common backend utilities project
    implementation(project(":commonBackend"))
}
```

Lastly, reload all gradle projects in IntellijIDEA, and you should be good to go.
