gerrit_plugin(
  name = 'events-log',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: events-log',
    'Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.Module',
    'Gerrit-HttpModule: com.ericsson.gerrit.plugins.eventslog.HttpModule',
  ],
  provided_deps = [
    '//lib:gson',
    '//lib/commons:dbcp'
  ],
)
