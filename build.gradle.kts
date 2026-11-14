// Root project: shared conventions only. Each MC version lives in versions/<mc>/.
plugins {
    base
    // Declared once here so both version nodes share one plugin version.
    id("net.neoforged.moddev") version "2.0.148" apply false
}

group = providers.gradleProperty("mod_group").getOrElse("cn.howxu")

// `-Pmod_version=1.2.3` (CI passes the release tag) overrides gradle.properties.
version = providers.gradleProperty("mod_version").getOrElse("0.1.7").removePrefix("v")
