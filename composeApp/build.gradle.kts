import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    jvm()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation("org.jetbrains.compose.material:material-icons-extended:1.7.3")
            implementation("com.materialkolor:material-kolor:4.1.1")
            implementation("io.github.ehsannarmani:compose-charts:0.2.5")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)
            implementation("com.github.oshi:oshi-core-ffm:7.3.0")
            implementation("org.xerial:sqlite-jdbc:3.46.1.3")
            runtimeOnly("org.slf4j:slf4j-simple:2.0.17")
            implementation("io.reactivex.rxjava3:rxjava:3.1.10")
            implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
            implementation("com.github.android-password-store:sublime-fuzzy:2.3.4")
        }
    }
}


compose.desktop {
    application {
        mainClass = "com.kay.cyberterrarium.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "Cyber Terrarium"
            packageVersion = "1.0.5"

            modules("java.sql", "java.naming", "java.management", "jdk.unsupported", "java.instrument", "java.security.jgss")

            windows {
                iconFile.set(project.file("src/jvmMain/resources/notification/tray.ico"))
            }

            macOS {
                iconFile.set(project.file("src/jvmMain/resources/app_icon.icns"))
                bundleID = "com.kay.cyberterrarium"
                packageName = "Cyber Terrarium"
                dockName = "Cyber Terrarium"
            }

            linux{
                iconFile.set(project.file("src/jvmMain/resources/app_icon.png"))
                packageName = "cyber-terrarium"
                debMaintainer = "123@gmail.com"
                menuGroup = "Utility"
                appRelease = "1.0.5"
                appCategory = "Utility"
                debPackageVersion = "1.0.5"
                rpmPackageVersion = "1.0.5"

            }
        }

        buildTypes.release.proguard {
            version.set("7.9.1")
            configurationFiles.from(project.file("proguard-rules.pro"))
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "skipped", "failed")
    }
}
