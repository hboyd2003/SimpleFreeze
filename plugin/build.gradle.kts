import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar.Companion.shadowJar

plugins {
    id("simplefreeze.common-conventions")
    alias(libs.plugins.runPaper)
    alias(libs.plugins.paperLoaderGen)
    //alias(libs.plugins.paperweight) // Should only be used for development
    alias(libs.plugins.gradleShadow)
}

runPaper.folia.registerTask {  }

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