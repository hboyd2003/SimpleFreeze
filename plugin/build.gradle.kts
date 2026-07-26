import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import dev.hboyd.git_simple_semver.GitSimpleSemverExtension
import io.papermc.hangarpublishplugin.HangarPublishTask

plugins {
    id("simplefreeze.common-conventions")
    alias(libs.plugins.runPaper)
    alias(libs.plugins.paperLoaderGen)
    //alias(libs.plugins.paperweight) // Should only be used for development
    alias(libs.plugins.gradleShadow)
    alias(libs.plugins.minotaur)
    alias(libs.plugins.hangarPublish)
}

runPaper.folia.registerTask { }

dependencies {
    //paperweight.paperDevBundle(libs.versions.paperAPI) // Should only be used for development

    compileOnly(libs.packetEvents)

    paperRuntime(libs.adventureSerializerConfigurate4)
    paperRuntime(libs.classGraph)
    paperRuntime(libs.sqliteJDBC)
    paperRuntime(libs.mySQLJDBC)
    paperRuntime(libs.mariaDBJDBC)
    paperRuntime(libs.chasm)
    paperRuntime(libs.bundles.configurate)
    paperRuntime(libs.bundles.prismatic)

    implementation(projects.simplefreezeApi)

    implementation(libs.bundles.doma)
    annotationProcessor(libs.domaProcessor)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }

    processResources {
        val props = mapOf("version" to version,
            "paperAPIVersion" to libs.versions.paperAPI.get().replace(Regex("-.*$"), ""))
        inputs.properties(props)
        filteringCharset = "UTF-8"
        expand(props)
    }

    generatePaperLoader {
        classPath = "dev.hboyd.simplefreeze.SimpleFreezeLoader"
    }

    shadowJar {
        archiveClassifier = ""
    }

    jar {
        enabled = false // Only shadowed jar
    }

    withType(HangarPublishTask::class.java) {
        inputs.file(shadowJar.flatMap { it.archiveFile })
    }
}

val supportedMinecraftVersions = listOf("1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1", "26.1.1", "26.1.2")
val fullVersion = rootProject.extensions.getByType(GitSimpleSemverExtension::class.java).version
val publishVersion = fullVersion.buildVersionString(
    includePreReleaseLabel = true,
    includeBuildMetadataLabel = version.toString().endsWith("-SNAPSHOT")
)

modrinth {
    token = providers.gradleProperty("modrinthToken")
        .orElse(providers.environmentVariable("MODRINTH_TOKEN"))
        .orNull
    projectId = "simple_freeze"
    versionNumber = publishVersion
    versionType = if (fullVersion.preReleaseLabel.contains("SNAPSHOT")) "beta" else "release"
    changelog = providers.environmentVariable("CHANGELOG")
    uploadFile = tasks.shadowJar.get().archiveFile.get()
    loaders = listOf("paper", "purpur", "folia")
    gameVersions = supportedMinecraftVersions
    dependencies {
        required.project("packetevents")
        // TODO: Add chasm companion plugin when it is published to modrinth
    }
    syncBodyFrom = rootProject.file("README.md").readText()
}

hangarPublish {
    publications.register("plugin") {
        version = fullVersion.toString() // Full version as releases cannot be replaced
        id = "simple-freeze"
        channel = if (fullVersion.preReleaseLabel.contains("SNAPSHOT")) "snapshot" else "release"
        changelog = providers.environmentVariable("CHANGELOG")
        apiKey = providers.gradleProperty("hangarKey")
            .orElse(providers.environmentVariable("HANGAR_KEY"))
            .orNull
        platforms {
            paper {
                jar = tasks.shadowJar.get().archiveFile.get()
                platformVersions = supportedMinecraftVersions
                dependencies {
                    url("PacketEvents", "https://modrinth.com/plugin/packetevents")
                    // TODO: Add chasm companion plugin when it is published to hangar
                }
            }
        }
    }
}
