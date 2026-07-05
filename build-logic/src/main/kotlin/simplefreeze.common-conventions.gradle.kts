/*
 * Simple Freeze
 * Copyright (c) 2026 Harrison Boyd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

val libs = extensions.getByType(org.gradle.accessors.dm.LibrariesForLibs::class)

plugins {
    java
    idea
    id("net.kyori.indra")
    id("net.kyori.indra.publishing")
    id("net.kyori.indra.checkstyle")
    id("net.kyori.indra.licenser.spotless")
}

dependencies {
    compileOnly(libs.paperAPI)
    compileOnly(libs.adventureSerializerConfigurate4)
    compileOnly(libs.indra)
    testImplementation(libs.junitJupiterApi)
    testRuntimeOnly(libs.bundles.junitJupiterRuntime)
}

indra {
    javaVersions {
        target(25)
    }

    github("hboyd2003", "SimpleFreeze") {
        ci(true)
        scm(true)
        publishing(false)
    }

    lgpl3OrLaterLicense()

    configurePublications {
        pom {
            developers {
                developer {
                    id.set("hboyd2003")
                    name.set("Harrison Boyd")
                    email.set("8950185+hboyd2003@users.noreply.github.com")
                    timezone = "America/New_York"
                }
            }
        }
    }

    checkstyle(libs.versions.checkstyle.get())
}

indraSpotlessLicenser {
    licenseHeaderFile(rootProject.file(".spotless/license_header_template.txt"))
    newLine(true)
}

spotless {
    java {
        targetExclude("build/generated/**/*.java")
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}