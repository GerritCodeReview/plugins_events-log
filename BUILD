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
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Gerrit-PluginName: events-log",
        "Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/events-log",
        "Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.sql.SQLModule",
        "Gerrit-HttpModule: com.ericsson.gerrit.plugins.eventslog.HttpModule",
    ],
    deps = [
        "@gson//jar:neverlink",
        "@commons_dbcp//jar:neverlink",
    ],
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
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-log"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":events-log__plugin",
        ":events-log__plugin_test_deps",
    ],
    testonly = 1,
)
