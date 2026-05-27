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
        maven("https://jitpack.io")
    }
}

rootProject.name = "BuyPilot"
include(":app")
include(":core:common")
include(":core:model")
include(":core:network")
include(":core:database")
include(":core:data")
include(":feature:chat")
include(":feature:history")
