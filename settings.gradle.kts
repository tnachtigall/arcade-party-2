pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        maven {
            name = "LCLPNetwork Maven"
            url = uri("https://repo.lclpnet.work/repository/internal")
        }
        mavenCentral()
        gradlePluginPortal()
    }

    plugins {
        val loom_version: String by settings
        id("fabric-loom") version loom_version
    }
}

rootProject.name = "arcade-party-2"