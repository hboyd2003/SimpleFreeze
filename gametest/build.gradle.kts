plugins {
    id("simplefreeze.common-conventions")
    alias(libs.plugins.fabricLoom)
}

fabricApi {
    configureTests {
        createSourceSet = true
        modId = project.name
        enableGameTests = false
        enableClientGameTests = true
        eula = true
    }
}

loom {
    mods {
        register("untitled8") {
            sourceSet("test")
        }
    }
}

sourceSets {

}

dependencies {
    minecraft("com.mojang:minecraft:26.1.2")
    implementation("net.fabricmc:fabric-loader:0.19.3")
    implementation("net.fabricmc.fabric-api:fabric-api:0.154.0+26.1.2")
}