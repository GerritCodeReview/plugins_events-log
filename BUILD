load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_dependency_tests",
    "gerrit_plugin_tests",
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
    deps = [
        "@events-log_plugin_deps//:com_zaxxer_HikariCP",
    ],
)

gerrit_plugin_tests(
    name = "events-log_tests",
    testonly = 1,
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["events-log"],
    deps = [
        ":events-log__plugin",
        "@events-log_plugin_deps//:com_zaxxer_HikariCP",
        "@events-log_plugin_deps//:org_bouncycastle_bcpg_jdk18on",
    ],
)

gerrit_plugin_dependency_tests(plugin = "events-log")
