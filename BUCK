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
# bazlets include those 3 bouncycastle jars in plugin API so this is temporary
# until this plugin is built with bazel.
# see https://gerrit-review.googlesource.com/#/c/102670/ for more info.
  ':bouncycastle_bcprov',
  ':bouncycastle_bcpg',
  ':bouncycastle_bcpkix',
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
  id = 'org.mockito:mockito-core:2.5.0',
  sha1 = 'be28d46a52c7f2563580adeca350145e9ce916f8',
  license = 'MIT',
  deps = [
    ':byte-buddy',
    ':objenesis',
  ],
)

maven_jar(
  name = 'byte-buddy',
  id = 'net.bytebuddy:byte-buddy:1.5.12',
  sha1 = 'b1ba1d15f102b36ed43b826488114678d6d413da',
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

BC_VERS = '1.56'

maven_jar(
  name = 'bouncycastle_bcprov',
  id = 'org.bouncycastle:bcprov-jdk15on:' + BC_VERS,
  sha1 = 'a153c6f9744a3e9dd6feab5e210e1c9861362ec7',
)

maven_jar(
  name = 'bouncycastle_bcpg',
  id = 'org.bouncycastle:bcpg-jdk15on:' + BC_VERS,
  sha1 = '9c3f2e7072c8cc1152079b5c25291a9f462631f1',
)

maven_jar(
  name = 'bouncycastle_bcpkix',
  id = 'org.bouncycastle:bcpkix-jdk15on:' + BC_VERS,
  sha1 = '4648af70268b6fdb24674fb1fd7c1fcc73db1231',
)
