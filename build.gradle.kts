import tanvd.kosogor.proxy.publishJar

plugins {
    kotlin("jvm") version "1.3.61" apply true
    id("tanvd.kosogor") version "1.0.7" apply true
    id("maven-publish")
}

subprojects {
    apply(plugin = "tanvd.kosogor")
    apply(plugin = "maven-publish")
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/exposed")
    }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/creavity-io/exposedx")
                credentials {
                    username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                    password = project.findProperty("gpr.key") as String? ?: System.getenv("TOKEN")
                }
            }
        }
    }
}

repositories {
    // mavenLocal()
    mavenCentral()
    jcenter()
}
