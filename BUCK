include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])
DEPS = [
  ':easymock',
  '//lib:gson',
  '//lib/commons:dbcp',
]
TEST_DEPS = GERRIT_PLUGIN_API + DEPS + GERRIT_TESTS + [
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
  deps = TEST_DEPS,
)

java_test(
  name = 'events-log_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['events-log'],
  deps = TEST_DEPS,
)

java_sources(
  name = 'events-log-sources',
  srcs = SOURCES + RESOURCES,
)

maven_jar(
  name = 'easymock',
  id = 'org.easymock:easymock:3.4',
  sha1 = '9fdeea183a399f25c2469497612cad131e920fa3',
  license = 'DO_NOT_DISTRIBUTE',
  deps = [
    ':cglib-2_2',
    ':objenesis',
  ],
)

maven_jar(
  name = 'cglib-2_2',
  id = 'cglib:cglib-nodep:2.2.2',
  sha1 = '00d456bb230c70c0b95c76fb28e429d42f275941',
  license = 'DO_NOT_DISTRIBUTE',
  attach_source = False,
)

maven_jar(
  name = 'objenesis',
  id = 'org.objenesis:objenesis:2.2',
  sha1 = '3fb533efdaa50a768c394aa4624144cf8df17845',
  license = 'DO_NOT_DISTRIBUTE',
  visibility = ['//lib/powermock:powermock-reflect'],
  attach_source = False,
)
