Build
=====

This plugin is built with Buck.

Buck
----

Two build modes are supported: Standalone and in Gerrit tree.
The standalone build mode is recommended, as this mode doesn't require
the Gerrit tree to exist locally.


### Build standalone

Clone bucklets library:

```
  git clone https://gerrit.googlesource.com/bucklets

```
and link it to events-log plugin directory:

```
  cd events-log && ln -s ../bucklets .
```

Add link to the .buckversion file:

```
  cd events-log && ln -s bucklets/buckversion .buckversion
```

Add link to the .watchmanconfig file:

```
  cd events-log && ln -s bucklets/watchmanconfig .watchmanconfig
```

To build the plugin, issue the following command:

```
  buck build plugin
```

The output is created in

```
  buck-out/gen/events-log.jar
```

This project can be imported into the Eclipse IDE:

```
  ./bucklets/tools/eclipse.py
```

To execute the tests run:

```
  buck test
```

### Build in Gerrit tree

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/events-log
```

The output is created in

```
  buck-out/gen/plugins/events-log/events-log.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  buck test --include events-log
```

More information about Buck can be found in the [Gerrit
documentation](../../../Documentation/dev-buck.html).
