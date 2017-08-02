load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

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
)

java_library(
    name = "events-log__plugin_test_deps",
    visibility = ["//visibility:public"],
    exports = [
        "@mockito//jar",
    ],
)

junit_tests(
    name = "events_log_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-log"],
    deps = [
        ":events-log__plugin_test_deps",
    ],
)
