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
    `maven-publish`
    idea
}

dependencies {
    compileOnly(libs.paperAPI)
    compileOnly(libs.adventureSerializerConfigurate4)
}

val targetJavaVersion = 25
java {
    withJavadocJar()
    withSourcesJar()

    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

publishing {
    repositories {
        maven {
            credentials {
                username = System.getenv("MAVEN_USERNAME")
                password = System.getenv("MAVEN_PASSWORD")
            }

            name = "hboyd-dev-repo"
            url = uri("https://repo.hboyd.dev/" + (if (version.toString().contains("SNAPSHOT")) "snapshots/" else "releases/"))
        }
    }
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}