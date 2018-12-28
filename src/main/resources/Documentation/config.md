@PLUGIN@ Configuration
===================

File 'gerrit.config'
--------------------

```
  [plugin "@PLUGIN@"]
    maxAge = 20
    returnLimit = 10000
    storeUrl = jdbc:h2:<gerrit_site>/data/db
    urlOptions = loglevel=INFO
    urlOptions = logUnclosedConnections=true
    copyLocal = true
```

plugin.@PLUGIN@.maxAge
:    Specify the maximum allowed age in days of the entries in the database.
     Any entries that are older than this value will be removed every day at
     23:00 hours. When not specified, the default value is 30 days.

plugin.@PLUGIN@.returnLimit
:    Specify the max amount of events that will be returned for each query.
     When not specified, the default value is 5000.

plugin.@PLUGIN@.storeUrl
:    Specify the path to the directory in which to keep the database. When not
     specified, the default path is jdbc:h2:\<gerrit_site>/data/db.

     Supported database engines:
     * h2 (default)
     * postgresql
     * mysql

plugin.@PLUGIN@.localStorePath
:    Specify the path to the directory in which to keep the back up database.
     When not specified, the default path is \<gerrit_site>/events-db/.

plugin.@PLUGIN@.storeUsername
:    Username to connect to the database, not defined by default. This value can
     also be defined in secure.config.

plugin.@PLUGIN@.storePassword
:    Password to connect to the database, not defined by default. This value can
     also be defined in secure.config.

plugin.@PLUGIN@.urlOptions
:    Options to append to the database url. Each option should be specified in a
     separate line using the option=value format. For example:
       urlOptions = loglevel=INFO
       urlOptions = logUnclosedConnections=true

     When using `mysql`, you must also specify:
       urlOptions = allowMultiQueries=true

plugin.@PLUGIN@.maxTries
:    Maximum number of times the plugin should attempt to store the event if a
     loss in database connection occurs. Setting this value to 0 will disable
     retries. When not specified, the default value is 3. After this number of
     failed tries, events shall be stored in the back up database until connection
     can be established.

plugin.@PLUGIN@.retryTimeout
:    Amount of time in milliseconds for which the plugin should wait in between
     event storage retries. When not specified, the default value is set to 1000ms.

plugin.@PLUGIN@.connectTimeout
:    Interval of time in milliseconds for which the plugin should try to reconnect
     to the database. When not specified, the default value is set to 1000ms.

plugin.@PLUGIN@.copyLocal
:    To keep a copy of the backup database once main database connection is
     restored, set to true. The file will be copied to the same location as the
     backup database with a timestamp appended. Note that the copied file will
     not be deleted and must be removed manually. When not specified, the default
     value is set to false.

plugin.@PLUGIN@.maxConnections
:    Maximum number of instances in the connection pool to the database. Includes
     active and idle connections. By default 8.
