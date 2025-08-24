plugins {
    kotlin("jvm")
    application
}

group = "com.bscan.tools"
version = "1.0.0"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("org.apache.pdfbox:pdfbox:3.0.1")
    
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("junit:junit:4.13.2")
}

application {
    mainClass.set("com.bscan.tools.UpdateCatalogKt")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

// Create fat JAR for easy distribution
tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.bscan.tools.UpdateCatalogKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}