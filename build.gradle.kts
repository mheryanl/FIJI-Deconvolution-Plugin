import org.gradle.api.file.DuplicatesStrategy

plugins {
    java
}

group = "org.lmh.deconvolution"
version = "1.1"

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.scijava.org/content/repositories/public/")
    }
}

dependencies {
    implementation("net.imagej:imagej:2.16.0")
    implementation("net.imagej:imagej-legacy:2.0.1")
    implementation("net.imagej:ij:1.53k")
    implementation("com.github.wendykierp:JTransforms:3.1")
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/resources") {
        include("plugins.config")
        into("/")
    }
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "org.lmh.deconvolution.DeconvolutionBlindPlugin"
    }
    archiveBaseName.set("DeconvolutionPlugin")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from(sourceSets.main.get().output)
    from("src/main/resources") {
        include("plugins.config")
    }
}
