import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar
import dev.hboyd.git_simple_semver.GitSimpleSemverExtension

plugins {
    id("simplefreeze.common-conventions")
    alias(libs.plugins.runPaper)
    alias(libs.plugins.paperLoaderGen)
    //alias(libs.plugins.paperweight) // Should only be used for development
    alias(libs.plugins.gradleShadow)
    alias(libs.plugins.minotaur)
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
}


modrinth {
    token = providers.gradleProperty("hboydModrinthToken")
        .orElse(providers.environmentVariable("HBOYD_MODRINTH_TOKEN"))
        .orNull
    projectId = "simple_freeze"
    versionNumber = rootProject.extensions.getByType(GitSimpleSemverExtension::class.java).version.buildVersionString(
        includePreReleaseLabel = true,
        includeBuildMetadataLabel = version.toString().endsWith("-SNAPSHOT")
    )
    versionType = if (versionNumber.toString().endsWith("-SNAPSHOT")) "beta" else "release"
    changelog = providers.environmentVariable("CHANGELOG")
    uploadFile = tasks.shadowJar.get().archiveFile.get()
    loaders = listOf("paper", "purpur", "folia")
    gameVersions = listOf("1.21.8", "1.21.9", "1.21.10", "1.21.11", "26.1")
    dependencies {
        required.project("packetevents")
        // TODO: Add chasm companion plugin when it is published to modrinth
    }
    syncBodyFrom = rootProject.file("README.md").readText()
}