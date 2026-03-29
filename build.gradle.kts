import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    base
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
}

allprojects {
    group = "cat.daisy"
    version = providers.gradleProperty("version").get()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    extensions.configure<KotlinJvmProjectExtension> {
        jvmToolchain(21)
    }

    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        withSourcesJar()
    }

    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xjsr305=strict")
        }
    }

    dependencies {
        add("implementation", "org.jetbrains.kotlin:kotlin-stdlib:2.3.20")
        add("testImplementation", "org.jetbrains.kotlin:kotlin-test:2.3.20")
        add("testImplementation", "org.junit.jupiter:junit-jupiter:5.14.3")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        maxParallelForks = 1
        maxHeapSize = "256m"
    }
}

project(":config-yaml") {
    dependencies {
        add("api", project(":config-base"))
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        add("testImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

project(":config-managed") {
    dependencies {
        add("api", project(":config-base"))
        add("api", project(":config-yaml"))
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        add("testImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

project(":config-modules") {
    dependencies {
        add("api", project(":config-managed"))
        add("api", project(":config-daisycore"))
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        add("testImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

project(":config-daisycore") {
    dependencies {
        add("api", project(":config-yaml"))
        add("api", "com.github.DaisyCatTs:DaisyCore:0.1.0-SNAPSHOT")
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        add("testImplementation", project(":config-managed"))
        add("testImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

project(":config-all") {
    dependencies {
        add("api", project(":config-base"))
        add("api", project(":config-yaml"))
        add("api", project(":config-managed"))
        add("api", project(":config-modules"))
        add("api", project(":config-daisycore"))
    }
}

project(":example-plugin") {
    dependencies {
        add("implementation", project(":config-all"))
        add("implementation", "com.github.DaisyCatTs:DaisySeries:0.1.0-SNAPSHOT")
        add("implementation", "com.github.DaisyCatTs:DaisyCore:0.1.0-SNAPSHOT")
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

project(":config-base") {
    dependencies {
        add("api", "com.github.DaisyCatTs:DaisySeries:0.1.0-SNAPSHOT")
        add("compileOnly", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
        add("testImplementation", "io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    }
}

tasks.register("quality") {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Runs the DaisyConfig verification suite."
    dependsOn(subprojects.map { "${it.path}:check" })
}
