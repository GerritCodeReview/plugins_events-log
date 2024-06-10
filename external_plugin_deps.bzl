load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
    maven_jar(
        name = "hikaricp",
        artifact = "com.zaxxer:HikariCP:5.1.0",
        sha1 = "8c96e36c14461fc436bb02b264b96ef3ca5dca8c",
    )
