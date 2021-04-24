import org.siouan.frontendgradleplugin.GradleTestListener

val fgpGroup: String by extra
val fgpVersion: String by extra
val fgpDisplayName: String by extra
val fgpDescription: String by extra
val fgpPluginId: String by extra
val fgpImplementationClass: String by extra
val fgpWebsiteUrl: String by extra
val fgpVcsUrl: String by extra
val fgpGradlePluginPortalTags: String by extra

plugins {
    id("java-gradle-plugin")
    id("jacoco")
    id("com.gradle.plugin-publish")
    id("maven-publish")
    id("org.sonarqube")
}

repositories {
    mavenCentral()
}

group = fgpGroup
version = fgpVersion
description = fgpDescription

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withJavadocJar()
    withSourcesJar()
}

sourceSets {
    create("intTest") {
        compileClasspath += sourceSets.main.get().output + sourceSets.test.get().output
        runtimeClasspath += sourceSets.main.get().output + sourceSets.test.get().output
    }
}

val intTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.implementation.get())
    extendsFrom(configurations.testImplementation.get())
}

configurations["intTestRuntimeOnly"]
    .extendsFrom(configurations.runtimeOnly.get())
    .extendsFrom(configurations.testRuntimeOnly.get())

dependencies {
    implementation(gradleApi())
    implementation("org.apache.httpcomponents.client5:httpclient5:5.0.3")
    implementation("org.apache.commons:commons-compress:1.20")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testImplementation("org.mockito:mockito-core:3.7.7")
    testImplementation("org.mockito:mockito-junit-jupiter:3.7.7")
    testImplementation("org.assertj:assertj-core:3.18.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    intTestImplementation("com.github.tomakehurst:wiremock:2.27.2")
}

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.ALL
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["intTest"].output.classesDirs
    classpath = sourceSets["intTest"].runtimeClasspath
    shouldRunAfter("test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Task>("check") {
    dependsOn(tasks.named("integrationTest"))
}

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.named("integrationTest"))
    executionData.setFrom(file("${project.buildDir}/jacoco/test.exec"), file("${project.buildDir}/jacoco/integrationTest.exec"))
    reports {
        xml.isEnabled = true
        xml.destination = file("${buildDir}/reports/jacoco/report.xml")
    }
}

gradle.addListener(GradleTestListener(logger))

gradlePlugin {
    plugins {
        create("frontendPlugin") {
            id = fgpPluginId
            implementationClass = fgpImplementationClass
        }
    }
}

pluginBundle {
    website = fgpWebsiteUrl
    vcsUrl = fgpVcsUrl

    (plugins) {
        "frontendPlugin" {
            displayName = fgpDisplayName
            description = fgpDescription
            tags = fgpGradlePluginPortalTags.split(",")
        }
    }
}

sonarqube {
    properties {
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.organization", "siouan")
        property("sonar.projectKey", "siouan_frontend-gradle-plugin")
        property("sonar.projectName", "frontend-gradle-plugin")
        property("sonar.projectVersion", "5.0.1-jdk11")

        property("sonar.links.homepage", "https://github.com/siouan/frontend-gradle-plugin")
        property("sonar.links.ci", "https://travis-ci.com/siouan/frontend-gradle-plugin")
        property("sonar.links.scm", "https://github.com/siouan/frontend-gradle-plugin")
        property("sonar.links.issue", "https://github.com/siouan/frontend-gradle-plugin/issues")

        property("sonar.sources", "src/main")
        property("sonar.tests", "src/test,src/intTest")

        property("sonar.java.binaries", "build/classes/java/main")
        property("sonar.java.test.binaries", "build/classes/java/test,build/classes/java/intTest")
        property("sonar.junit.reportPaths", "build/test-results/test/,build/test-results/integrationTest/")
        property("sonar.coverage.jacoco.xmlReportPaths", "${buildDir}/reports/jacoco/report.xml")

        // Unrelevant duplications detected on task inputs
        property("sonar.cpd.exclusions", "**/org/siouan/frontendgradleplugin/domain/model/*.java,**/org/siouan/frontendgradleplugin/domain/usecase/Get*ExecutablePath.java")

    }
}
