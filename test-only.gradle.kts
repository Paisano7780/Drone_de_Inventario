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

sourceSets {
    main {
        kotlin.srcDirs("app/src/main/java/com/paisano/droneinventoryscanner/data")
    }
    test {
        kotlin.srcDirs("app/src/test/java/com/paisano/droneinventoryscanner/data")
    }
}
