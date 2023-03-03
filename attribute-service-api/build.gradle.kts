import com.google.protobuf.gradle.id

plugins {
  `java-library`
  id("com.google.protobuf") version "0.9.2"
  id("org.hypertrace.publish-plugin")
}

protobuf {
  protoc {
    artifact = "com.google.protobuf:protoc:3.21.12"
  }
  plugins {
    id("grpc") {
      artifact = "io.grpc:protoc-gen-grpc-java:1.50.0"
    }
  }
  generateProtoTasks {
    ofSourceSet("main").configureEach {
      plugins {
        id("grpc")
      }
    }
  }
}

sourceSets {
  main {
    java {
      srcDirs("build/generated/source/proto/main/java", "build/generated/source/proto/main/grpc_java")
    }
  }
}

dependencies {
  api(platform("io.grpc:grpc-bom:1.50.0"))
  api("io.grpc:grpc-stub")
  api("io.grpc:grpc-protobuf")

  implementation("javax.annotation:javax.annotation-api:1.3.2")
}
