architectury {
    platformSetupLoomIde()
    neoForge()
}

base.archivesName.set("${base.archivesName.get()}-neoforge")

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
}

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge/") {
        name = "KotlinForForge"
    }
    maven("https://maven.neoforged.net/releases/") {
        name = "NeoForged"
    }
    maven("https://cursemaven.com") {
        name = "Curse"
    }
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentNeoForge"].extendsFrom(this)
}

dependencies {
    neoForge("net.neoforged:neoforge:${project.properties["neoforge_version"]!!}")
    implementation("thedarkcolour:kotlinforforge-neoforge:${project.properties["kotlin_forge_version"]!!}")
    common(project(":common")) { isTransitive = false }
    shadowCommon(project(":common")) { isTransitive = false }
    api("me.shedaniel.cloth:cloth-config-neoforge:${project.properties["cloth_config_version"]}")
}

tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("META-INF/neoforge.mods.toml") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
    }
}
