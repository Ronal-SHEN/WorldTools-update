import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version ("2.3.21")
    id("architectury-plugin") version "3.5.167"
    id("dev.architectury.loom-no-remap") version "1.17-SNAPSHOT" apply false
    id("com.gradleup.shadow") version "9.4.2" apply false
}

architectury {
    minecraft = project.properties["minecraft_version"]!! as String
}

subprojects {
    apply(plugin = "dev.architectury.loom-no-remap")
    dependencies {
        "minecraft"("com.mojang:minecraft:${project.properties["minecraft_version"]!!}")
        // Minecraft 26.x ships deobfuscated; the loom-no-remap plugin needs no mappings
    }
    if (path != ":common") {
        apply(plugin = "com.gradleup.shadow")

        val shadowCommon by configurations.creating {
            isCanBeConsumed = false
            isCanBeResolved = true
        }
        val mcDisplayVersion = (project.properties["mc_display_version"] ?: project.properties["minecraft_version"])!!
        val versionWithMCVersion = "${project.properties["mod_version"]!!}+$mcDisplayVersion"

        tasks.withType<JavaCompile> {
            options.encoding = "UTF-8"
            options.release = 25
        }

        tasks {
            // loom-no-remap: no remapJar step, so shadowJar produces the final artifact
            val shadowJarTask = named("shadowJar", ShadowJar::class)
            shadowJarTask {
                archiveVersion = versionWithMCVersion
                archiveClassifier = ""
                configurations = listOf(shadowCommon)
            }
            jar {
                enabled = false
            }
        }
    }
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")
    apply(plugin = "org.jetbrains.kotlin.jvm")
    base.archivesName.set(project.properties["archives_base_name"]!! as String)
    group = project.properties["maven_group"]!!
    version = project.properties["mod_version"]!!

    repositories {
        maven("https://api.modrinth.com/maven")
        maven("https://jitpack.io")
        maven("https://server.bbkr.space/artifactory/libs-release") {
            name = "CottonMC"
        }
        maven("https://maven.shedaniel.me/")
        maven("https://maven.terraformersmc.com/releases/")
    }

    tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        }
    }

    tasks.withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
        options.release = 25
    }
}
