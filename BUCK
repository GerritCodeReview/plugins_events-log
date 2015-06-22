include_defs('//bucklets/gerrit_plugin.bucklet')

DEPS = [
  '//lib:gson',
  '//lib/commons:dbcp',
]

TEST_DEPS = GERRIT_PLUGIN_API + [
  ':events-log__plugin',
  '//lib/easymock:easymock',
  '//lib:gson',
  '//lib:junit',
  '//lib:truth',
]

gerrit_plugin(
  name = 'events-log',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Gerrit-PluginName: events-log',
    'Implementation-Vendor: Ericsson',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/events-log',
    'Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.sql.SqlModule',
    'Gerrit-HttpModule: com.ericsson.gerrit.plugins.eventslog.HttpModule',
  ],
  provided_deps = DEPS,
)

java_library(
  name = 'classpath',
  deps = list(set(DEPS) | set(TEST_DEPS))
)

java_test(
  name = 'events-log_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['events-log'],
  source_under_test = [':events-log__plugin'],
  deps = TEST_DEPS,
)
