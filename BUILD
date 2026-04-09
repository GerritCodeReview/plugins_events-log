load("@rules_java//java:defs.bzl", "java_library")
load("@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl", "gerrit_plugin", "gerrit_plugin_tests")

gerrit_plugin(
    name = "events-log",
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Gerrit-PluginName: events-log",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/events-log",
        "Implementation-Title: events-log plugin",
        "Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.sql.SQLModule",
        "Gerrit-HttpModule: com.ericsson.gerrit.plugins.eventslog.HttpModule",
    ],
    resources = glob(["src/main/resources/**/*"]),
    deps = ["@events_log_plugin_deps//:com_zaxxer_HikariCP"],
)

gerrit_plugin_tests(
    name = "events-log_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-log"],
    deps = [
        ":events-log__plugin_test_deps",
    ],
)

java_library(
    name = "events-log__plugin_test_deps",
    testonly = 1,
    visibility = ["//visibility:public"],
    exports = [
        ":events-log__plugin",
        "@events_log_plugin_deps//:com_zaxxer_HikariCP",
    ],
)
