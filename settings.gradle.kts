enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "simplefreeze"

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.papermc.io/repository/maven-snapshots/") {
            name = "papermc-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.codemc.io/repository/maven-releases/") {
            name = "codemc-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.codemc.io/repository/maven-snapshots/") {
            name = "codemc-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = ".gradle/loom-cache/minecraftMaven") {
            name = "LoomLocalMinecraft"
        }
        maven(url = ".gradle/loom-cache/remapped_mods") {
            name = "LoomLocalRemappedMods"
        }
        gradlePluginPortal()
    }
}

pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = "https://repo.papermc.io/repository/maven-public/") {
            name = "papermc-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.papermc.io/repository/maven-snapshots/") {
            name = "papermc-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://repo.hboyd.dev/releases/") {
            name = "hboyd-dev-repo-releases"
            mavenContent { releasesOnly() }
        }
        maven(url = "https://repo.hboyd.dev/snapshots/") {
            name = "hboyd-dev-repo-snapshots"
            mavenContent { snapshotsOnly() }
        }
        maven(url = "https://maven.fabricmc.net/") {
            name = "fabric"
        }
        gradlePluginPortal()
    }
}

sequenceOf(
        "api",
        "plugin",
        "gametest"
).forEach {
    include("simplefreeze-$it")
    project(":simplefreeze-$it").projectDir = file(it)
}