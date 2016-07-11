include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])
DEPS = [
  '//lib:gson',
  '//lib/commons:dbcp',
]
TEST_DEPS = GERRIT_PLUGIN_API + GERRIT_TESTS + [
  ':events-log__plugin',
]

gerrit_plugin(
  name = 'events-log',
  srcs = SOURCES,
  resources = RESOURCES,
  manifest_entries = [
    'Gerrit-PluginName: events-log',
    'Implementation-Vendor: Ericsson',
    'Implementation-URL: https://gerrit-review.googlesource.com/#/admin/projects/plugins/events-log',
    'Gerrit-Module: com.ericsson.gerrit.plugins.eventslog.sql.SQLModule',
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
  deps = list(set(DEPS) | set(TEST_DEPS)),
)

java_sources(
  name = 'events-log-sources',
  srcs = SOURCES + RESOURCES,
)
