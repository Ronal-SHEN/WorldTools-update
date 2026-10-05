architectury {
    platformSetupLoomIde()
    fabric()
}

base.archivesName.set("${base.archivesName.get()}-fabric")

loom {
    accessWidenerPath.set(project(":common").loom.accessWidenerPath)
    enableTransitiveAccessWideners.set(true)
    runs {
        getByName("client") {
            ideConfigGenerated(true)
        }
    }
}

val common: Configuration by configurations.creating {
    configurations.compileClasspath.get().extendsFrom(this)
    configurations.runtimeClasspath.get().extendsFrom(this)
    configurations["developmentFabric"].extendsFrom(this)
}

dependencies {
    common(project(":common")) { isTransitive = false }
    shadowCommon(project(":common")) { isTransitive = false }
    implementation("net.fabricmc:fabric-loader:${project.properties["fabric_loader_version"]!!}")
    implementation("net.fabricmc.fabric-api:fabric-api:${project.properties["fabric_api_version"]!!}")
    // loom-no-remap doesn't extract fabric-api's jar-in-jar modules, so depend on the used ones directly
    implementation("net.fabricmc.fabric-api:fabric-command-api-v2:3.1.2+fcdff87f5d")
    implementation("net.fabricmc.fabric-api:fabric-key-mapping-api-v1:2.0.8+3434d6d95d")
    implementation("net.fabricmc.fabric-api:fabric-lifecycle-events-v1:4.1.9+ffef5f675d")
    implementation("net.fabricmc.fabric-api:fabric-networking-api-v1:6.3.8+fcdff87f5d")
    implementation("net.fabricmc.fabric-api:fabric-screen-api-v1:5.2.4+48607d035d")
    implementation("net.fabricmc:fabric-language-kotlin:${project.properties["fabric_kotlin_version"]!!}")
    api("me.shedaniel.cloth:cloth-config-fabric:${project.properties["cloth_config_version"]}")
    api("com.terraformersmc:modmenu:${project.properties["mod_menu_version"]}")
}

tasks {
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") {
            expand(getProperties())
            expand(mutableMapOf("version" to project.version))
        }
        // dev runs resolve the access widener inside the fabric mod's own root, so mirror
        // it out of :common — without it the loader fails with "Missing classTweaker file"
        from(project(":common").sourceSets["main"].resources.srcDirs) {
            include("worldtools.accesswidener")
        }
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    // the same file also arrives via shadowCommon; keep only one copy in the jar
    shadowJar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
