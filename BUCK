include_defs('//bucklets/gerrit_plugin.bucklet')

gerrit_plugin(
  name = 'events-log',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: events-log',
    'Implementation-Vendor: Ericsson',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/events-log',
    'Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.Module',
    'Gerrit-HttpModule: com.ericsson.gerrit.plugins.eventslog.HttpModule',
  ],
  provided_deps = [
    '//lib:gson',
    '//lib/commons:dbcp'
  ],
)

java_library(
  name = 'classpath',
  deps = [':events-log__plugin'],
)

java_test(
  name = 'events-log_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['events-log'],
  source_under_test = [':events-log__plugin'],
  deps = GERRIT_PLUGIN_API + [
    ':events-log__plugin',
    '//lib/easymock:easymock',
    '//lib:gson',
    '//lib:junit',
    '//lib:truth',
  ],
)
