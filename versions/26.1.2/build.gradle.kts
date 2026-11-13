// Minecraft 26.1.2 / NeoForge 26.1.x node.
//
// Identical source tree to 1.21.1 apart from the version stub (Identifier vs
// ResourceLocation) and the Java language level.
plugins {
    `java-library`
    id("net.neoforged.moddev")
}

group = rootProject.group
version = rootProject.version

val mcVersion = property("minecraft_version") as String
val neoVersion = property("neo_version") as String
val javaLevel = (property("java_version") as String).toInt()
val nekojsCoordinate = property("nekojsCoordinate") as String
val mmcrCoordinate = property("mmcrCoordinate") as String

base { archivesName.set("mmcr_nekojs-$mcVersion") }

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(javaLevel))
    withSourcesJar()
}

neoForge {
    version = neoVersion
    // Compile-only mod: no access transformers, mixins or datagen required.
}

sourceSets {
    main {
        java.srcDir("../../common/src/main/java")
        resources.srcDir("../../common/src/main/resources")
    }
}

repositories {
    mavenCentral()
    maven("https://maven.neoforged.net/releases")
    maven("https://cursemaven.com") { content { includeGroup("curse.maven") } }
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
}

dependencies {
    // isTransitive = false: the upstream POMs pull in a wall of optional mod deps we
    // never touch, and CurseMaven/Modrinth POMs are not always resolvable offline.
    compileOnly(nekojsCoordinate) { isTransitive = false }
    compileOnly(mmcrCoordinate) { isTransitive = false }
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(javaLevel)
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "Implementation-Title" to (project.findProperty("mod_name") ?: "MMCR NekoJS Bridge"),
            "Implementation-Version" to project.version,
        )
    }
}

tasks.named("build") {
    doLast {
        val jar = tasks.named<Jar>("jar").get().archiveFile.get().asFile
        require(jar.isFile) { "jar was not produced at $jar" }
        logger.lifecycle("built ${jar.name} (${jar.length() / 1024} KiB)")
    }
}
