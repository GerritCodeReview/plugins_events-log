load("//tools/bzl:maven_jar.bzl", "maven_jar")

def external_plugin_deps():
  maven_jar(
    name = 'mockito',
    artifact = 'org.mockito:mockito-core:2.5.1',
    sha1 = '9cda1bf1674c8de3a1116bae4d7ce0046a857d30',
    deps = [
      '@byte_buddy//jar',
      '@objenesis//jar',
    ],
  )

  maven_jar(
    name = 'byte_buddy',
    artifact = 'net.bytebuddy:byte-buddy:1.5.12',
    sha1 = 'b1ba1d15f102b36ed43b826488114678d6d413da',
    attach_source = False,
  )

  maven_jar(
    name = 'objenesis',
    artifact = 'org.objenesis:objenesis:2.4',
    sha1 = '2916b6c96b50c5b3ec4452ed99401db745aabb27',
    attach_source = False,
  )
