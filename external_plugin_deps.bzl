load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "hikaricp",
        artifact = "com.zaxxer:HikariCP:3.2.0",
        sha1 = "6c66db1c636ee90beb4c65fe34abd8ba9396bca6",
    )
