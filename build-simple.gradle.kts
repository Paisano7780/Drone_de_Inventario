plugins {
    kotlin("jvm") version "1.8.0"
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

tasks.test {
    useJUnit()
}

tasks.register<Copy>("prepareTests") {
    from("app/src/main/java")
    into("src/main/kotlin")
}

tasks.register<Copy>("prepareTestClasses") {
    from("app/src/test/java")
    into("src/test/kotlin")
}

sourceSets {
    main {
        kotlin.srcDirs("app/src/main/java")
    }
    test {
        kotlin.srcDirs("app/src/test/java")
    }
}
