plugins {
    idea
    alias(libs.plugins.gitSimpleSemver)
    alias(libs.plugins.indra).apply(false)
    alias(libs.plugins.indraLicenserSpotless).apply(false)
}

tasks {
    jar {
        enabled = false
    }
}
