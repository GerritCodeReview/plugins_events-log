@PLUGIN@ Configuration
===================

File 'gerrit.config'
--------------------

  [plugin "@PLUGIN@"]
    maxAge = 20
    returnLimit = 10000
    storeUrl = jdbc:h2:~/gerrit/db
    urlOptions = DB_CLOSE_DELAY=10

plugin.@PLUGIN@.maxAge
:    Specify the maximum allowed age in days of the entries in the database.
     Any entries that are older than this value will be removed on server startup.
     When not specified, the default value is 30 days.

plugin.@PLUGIN@.returnLimit
:    Specify the max amount of events that will be returned for each query.
     When not specified, the default value is 5000.

plugin.@PLUGIN@.storeDriver
:    Specify the driver of the database. When not specified, the default driver is
     org.h2.Driver.

plugin.@PLUGIN@.storeUrl
:    Specify the path to the directory in which to keep the database. When not
     specified, the default path is jdbc:h2:~/db/.

plugin.@PLUGIN@.storeUsername
:    Username to connect to the database, not defined by default. This value can
     also be defined in secure.config.

plugin.@PLUGIN@.storePassword
:    Password to connect to the database, not defined by default. This value can
     also be defined in secure.config.

plugin.@PLUGIN@.urlOptions
:    Options to append to the database url.

plugin.@PLUGIN@.threadPoolSize
:    Number of threads to allocate for storing events to the database. Defaults
     to 1 if not provided.
