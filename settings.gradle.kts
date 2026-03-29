pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://jitpack.io")
    }
}

rootProject.name = "DaisyConfig"

includeBuild("../DaisySeries") {
    dependencySubstitution {
        substitute(module("com.github.DaisyCatTs:DaisySeries")).using(project(":series-all"))
    }
}

includeBuild("../DaisyCore") {
    dependencySubstitution {
        substitute(module("com.github.DaisyCatTs:DaisyCore")).using(project(":platform-all"))
    }
}

include(
    "config-base",
    "config-yaml",
    "config-daisycore",
    "config-all",
    "example-plugin",
)
