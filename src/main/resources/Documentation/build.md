Build
=====

This plugin is built with Bazel. Two build modes are supported:
 * Standalone
 * In Gerrit tree.

Standalone build mode is recommended, as this mode doesn't require local Gerrit
tree to exist. Moreover, there are some limitations and additional manual steps
required when building in Gerrit tree mode (see corresponding sections).

## Build standalone

### Bazel

To build the plugin, issue the following command:

```
  bazel build @PLUGIN@
```

The output is created in

```
  bazel-genfiles/@PLUGIN@.jar
```

To execute the tests run:

```
  bazel test //...
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.sh
```

## Build in Gerrit tree

### Bazel

Clone (or link) this plugin to the `plugins` directory of Gerrit's source tree.

Put the external dependency Bazel build file into the Gerrit /plugins directory,
replacing the existing empty one.

```
  cd gerrit/plugins
  rm external_plugin_deps.bzl
  ln -s @PLUGIN@/external_plugin_deps.bzl .
```

Then issue

```
  bazel build plugins/@PLUGIN@
```

in the root of Gerrit's source tree to build

The output is created in

```
  bazel-genfiles/plugins/@PLUGIN@/@PLUGIN@.jar
```

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` and to the
`CUSTOM_PLUGINS_TEST_DEPS` set in Gerrit core in
`tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  bazel test plugins/@PLUGIN@:events_log_tests
```

More information about Buck can be found in the [Gerrit
documentation](../../../Documentation/dev-bazel.html).
