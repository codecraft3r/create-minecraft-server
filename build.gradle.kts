plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("8.1.1")
}

group = "org.codecraft"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("org.apache.logging.log4j:log4j-api:2.21.0")
    implementation("org.apache.logging.log4j:log4j-core:2.21.0")
}

tasks {
    shadowJar {
        mergeServiceFiles()
        manifest {
            attributes["Main-Class"] = "org.codecraft.createminecraftserver.MinecraftServerLauncher" // Replace 'your.main.Class' with the fully qualified name of your main class
        }
    }
    jar {
        manifest {
            attributes["Main-Class"] = buildString {
                append("org.codecraft.createminecraftserver.MinecraftServerLauncher")
            }
        }
    }
    test {
        useJUnitPlatform()
    }
}