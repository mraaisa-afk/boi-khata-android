pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "boi-khata-android"

include(
    ":app",
    ":core:database",
    ":core:domain",
    ":core:cloud",
    ":core:designsystem",
    ":core:common",
    ":feature:home",
    ":feature:sale",
    ":feature:catalog",
    ":feature:khata",
    ":feature:expense",
    ":feature:supplier",
    ":feature:reports",
    ":feature:subscription",
    ":feature:melamode",
    ":feature:support",
    ":shared:receipt",
)
