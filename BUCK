include_defs('//bucklets/gerrit_plugin.bucklet')
include_defs('//bucklets/java_sources.bucklet')
include_defs('//bucklets/maven_jar.bucklet')

SOURCES = glob(['src/main/java/**/*.java'])
RESOURCES = glob(['src/main/resources/**/*'])
DEPS = [
  '//lib:gson',
  '//lib/commons:dbcp',
]
TEST_DEPS = GERRIT_PLUGIN_API + DEPS + GERRIT_TESTS + [
  ':events-log__plugin',
  ':mockito',
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
  name = 'mockito',
  id = 'org.mockito:mockito-core:2.3.7',
  sha1 = '321d06f541671ccdc8631f4659e3df71d8b165d7',
  license = 'Apache2.0',
  deps = [
    ':byte-buddy',
    ':objenesis',
  ],
)

maven_jar(
  name = 'byte-buddy',
  id = 'net.bytebuddy:byte-buddy:1.5.5',
  sha1 = '8557b6465cea17f3769678235e77d5cb076c1170',
  license = 'DO_NOT_DISTRIBUTE',
  attach_source = False,
)

maven_jar(
  name = 'objenesis',
  id = 'org.objenesis:objenesis:2.4',
  sha1 = '2916b6c96b50c5b3ec4452ed99401db745aabb27',
  license = 'DO_NOT_DISTRIBUTE',
  attach_source = False,
)
