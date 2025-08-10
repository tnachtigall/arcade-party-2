import org.gradle.jvm.tasks.Jar
import work.lclpnet.build.task.GithubDeploymentTask
import java.util.*

plugins {
    id("java")
    id("fabric-loom")
    id("maven-publish")
    id("gradle-build-utils") version "1.7.0"
    id("io.freefair.lombok") version "8.11"
}

val props: Properties = buildUtils.loadProperties("publish.properties")

group = property("maven_group") as String
version = "${property("mod_version")}+${property("minecraft_version")}"

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    mavenCentral()

    maven(url = "https://repo.lclpnet.work/repository/internal")
    maven(url = "https://repo.lclpnet.work/repository/snapshots")

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }

        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven(url = "https://maven.nucleoid.xyz/")
    maven(url = "https://maven.terraformersmc.com/releases/")
    maven(url = "https://maven.shedaniel.me/")
}

loom {
    // if you want to develop a plugin for dedicated servers, uncomment the following block
    serverOnlyMinecraftJar()

    runs {
        remove(getByName("client"))

        named("server") {
            vmArgs("-Dfabric-tag-conventions-v2.missingTagTranslationWarning=SILENCED")
        }
    }

    accessWidenerPath.set(file("src/main/resources/ap2.accesswidener"))
}

dependencies {
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")

    // extra dependencies required for compilation
    implementation("org.json:json:20231013")
    include(implementation("work.lclpnet:json-config4j:1.0.0") as Any)
    include(implementation("org.apache.commons:commons-compress:1.26.2") as Any)
    include(implementation("org.ejml:ejml-core:0.44.0") as Any)
    include(implementation("org.ejml:ejml-simple:0.44.0") as Any)

    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    modImplementation("work.lclpnet.mods.kibu:kibu:${property("kibu_version")}")
    modImplementation("work.lclpnet.mods.kibu:kibu-world-api:${property("kibu_world_api_version")}")
    modImplementation("work.lclpnet.mods.kibu:kibu-physics:${property("kibu_physics_version")}")
    modImplementation("xyz.nucleoid:fantasy:${property("fantasy_version")}")
    modImplementation("maven.modrinth:sqlite-jdbc:3.41.2.1+20230506")
    modImplementation("work.lclpnet.mods:combat-control:${property("combat_control_version")}")
    modImplementation("work.lclpnet.mods:notica:${property("notica_version")}")
    modImplementation("work.lclpnet.mods:mg-lobby:${property("lobby_version")}")

    // optional runtime dependencies (not explicitly required for compilation, but needed to play the game)
    modLocalRuntime("work.lclpnet.mods:pal:${property("pal_version")}")
    modLocalRuntime("work.lclpnet.mods:ruler:${property("ruler_version")}")
    modLocalRuntime("work.lclpnet.mcserver-api:mcserver-api-fabric:${property("mcserver_api_version")}")
    implementation("work.lclpnet:translations4j:${property("translations4j_version")}")
    implementation("work.lclpnet:translations4j:${property("translations4j_version")}:network")

    // optional runtime dependencies (not required to play)
    modLocalRuntime("work.lclpnet.mods:mc-debug-sender-impl:${property("mc_debug_sender_impl_version")}")
    modLocalRuntime("maven.modrinth:anti-xray:${property("anti_xray_version")}")
    localRuntime("com.moandjiezana.toml:toml4j:${property("toml_version")}")

    // test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
    testImplementation("org.mockito:mockito-core:5.6.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.8.1")
    testImplementation("net.fabricmc:fabric-loader-junit:${property("loader_version")}")
    testImplementation("org.xerial:sqlite-jdbc:3.41.2.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
    systemProperty("fabric.side", "server")
}

tasks.withType<ProcessResources> {
    inputs.property("version", version)

    filesMatching("fabric.mod.json") {
        expand(mapOf("version" to version))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

java {
    // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

listOf(tasks.named<Jar>("jar"), tasks.named<Jar>("sourcesJar")).forEach {
    it.configure {
        from("LICENSE") {
            rename { "${it}_${base.archivesName.get()}" }
        }
    }
}

val env: Map<String, String> = System.getenv()

tasks.register<GithubDeploymentTask>("github") {
    dependsOn(tasks.named("remapJar"))

    config {
        token = env["GITHUB_TOKEN"]
        repository = env["GITHUB_REPOSITORY"]
    }

    release {
        title = "[${property("minecraft_version")}] ${project.name} ${project.version}"
        tag = project.version.toString()
    }

    assets.add(tasks.named<Jar>("remapJar").get().archiveFile.get())
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()

            from(components["java"])

            pom {
                name.set("Arcade Party 2")
                description.set("Reworked ArcadeParty as Fabric mini-game.")
            }
        }
    }

    buildUtils.setupPublishRepository(repositories, props)
}