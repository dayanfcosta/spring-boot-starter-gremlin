import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.9"
    id("io.spring.dependency-management") version "1.1.7"
    `maven-publish`
    signing
}

group = "io.github.dayanfcosta"
version = "1.0.0"
description = "Spring Boot starter for Apache TinkerPop Gremlin"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Apache TinkerPop Gremlin
    implementation("org.apache.tinkerpop:gremlin-driver:3.8.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_17
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<BootJar>("bootJar") {
    enabled = false
}

tasks.named<Jar>("jar") {
    enabled = true
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])

            pom {
                name = "Spring Boot Starter Gremlin"
                description = "Spring Boot starter for Apache TinkerPop Gremlin graph database connectivity"
                url = "https://github.com/dayanfcosta/spring-boot-starter-gremlin"

                licenses {
                    license {
                        name = "The Apache License, Version 2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }

                developers {
                    developer {
                        id = "dayanfcosta"
                        name = "Dayan Costa"
                        url = "https://github.com/dayanfcosta"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/dayanfcosta/spring-boot-starter-gremlin.git"
                    developerConnection = "scm:git:ssh://github.com/dayanfcosta/spring-boot-starter-gremlin.git"
                    url = "https://github.com/dayanfcosta/spring-boot-starter-gremlin"
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = if (version.toString().endsWith("SNAPSHOT")) {
                uri("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                uri("https://central.sonatype.com/api/v1/publisher/upload")
            }
            credentials {
                username = System.getenv("MAVEN_USERNAME") ?: findProperty("ossrhUsername")?.toString()
                password = System.getenv("MAVEN_PASSWORD") ?: findProperty("ossrhPassword")?.toString()
            }
        }
    }
}

signing {
    val signingKey = System.getenv("GPG_PRIVATE_KEY")
    val signingPassword = System.getenv("GPG_PASSPHRASE")

    if (signingKey != null && signingPassword != null) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }

    sign(publishing.publications["mavenJava"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { !version.toString().endsWith("SNAPSHOT") }
}
