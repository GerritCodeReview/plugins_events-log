workspace(name = "events_log")

load("//:bazlets.bzl", "load_bazlets")

load_bazlets(
    commit = "2629e3802493a45cdc7a3e03deb3331743daa09c",
    #local_path = "/home/<user>/projects/bazlets",
)

load(
    "@com_googlesource_gerrit_bazlets//:gerrit_api.bzl",
    "gerrit_api",
)

gerrit_api()

load("//:external_plugin_deps.bzl", "external_plugin_deps")

external_plugin_deps()
