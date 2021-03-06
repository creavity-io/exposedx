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
        // mavenLocal()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/exposed")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/creavity-io/Exposed")
            credentials {
                username = project.findProperty("GPR_USER") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("GPR_KEY") as String? ?: System.getenv("TOKEN")
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/creavity-io/exposedx")
                credentials {
                    username = project.findProperty("GPR_USER") as String? ?: System.getenv("USERNAME")
                    password = project.findProperty("GPR_KEY") as String? ?: System.getenv("TOKEN")
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
