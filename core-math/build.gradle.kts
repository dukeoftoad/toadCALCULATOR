// SPDX-License-Identifier: GPL-3.0-or-later
//
// Pure Kotlin math engine module. ZERO Android imports allowed — enforced by
// the verifyNoAndroidImports task below and wired into `check`.

plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.truth)
}

tasks.test {
    useJUnit()
}

// Architectural guard: :core-math must remain a pure JVM module.
tasks.register("verifyNoAndroidImports") {
    group = "verification"
    description = "Fails the build if any Android/androidx import appears in :core-math sources."
    doLast {
        val srcRoots = listOf("src/main", "src/test")
        val offenders = srcRoots.flatMap { root ->
            val dir = layout.projectDirectory.dir(root)
            if (!dir.asFile.exists()) emptyList()
            else dir.asFileTree.matching { include("**/*.kt", "**/*.kts") }.files.flatMap { f ->
                f.readLines().mapIndexedNotNull { idx, line ->
                    val t = line.trimStart()
                    if (t.startsWith("import android.") || t.startsWith("import androidx.") ||
                        t.startsWith("import com.android.")
                    ) "${root}/${f.relativeTo(layout.projectDirectory.asFile)}:${idx + 1}: $t" else null
                }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                ":core-math must not depend on Android:\n" + offenders.joinToString("\n")
            )
        }
        println("verifyNoAndroidImports: OK — no Android imports in :core-math")
    }
}

tasks.named("check") {
    dependsOn("verifyNoAndroidImports")
}
