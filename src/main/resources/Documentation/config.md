@PLUGIN@ Configuration
===================

File 'gerrit.config'
--------------------

```
  [plugin "@PLUGIN@"]
    maxAge = 20
    startTime = Sun 00:00
    interval = 1 day
    returnLimit = 10000
    storeUrl = jdbc:h2:<gerrit_site>/data/db
    urlOptions = loglevel=INFO
    urlOptions = logUnclosedConnections=true
    copyLocal = true
```

plugin.@PLUGIN@.maxAge
:    Specify the maximum allowed age in days of the entries in the database.
     Any entries that are older than this value will be removed periodically
     starting on 'startTime' and with a period defined by 'interval'. If any
     of these values is not defined in the configuration, the removal will be
     performed on server startup.
     When not specified, the default value is 30 days.

plugin.@PLUGIN@.startTime
:       start time to define the first execution of the old entries removal
from the database. Expressed as &lt;day of week> &lt;hours>:&lt;minutes>.
By default, disabled.

This setting should be expressed using the following time units:

  * &lt;day of week> : Mon, Tue, Wed, Thu, Fri, Sat, Sun
  * &lt;hours> : 00-23
  * &lt;minutes> : 00-59

plugin.@PLUGIN@.interval
:       interval for periodic repetition of the old entries removal
from the database. By default, disabled.

The following suffixes are supported to define the time unit for the interval:

 * h, hour, hours
 * d, day, days
 * w, week, weeks (1 week is treated as 7 days)
 * mon, month, months (1 month is treated as 30 days)

If no time unit is specified, days are assumed.

plugin.@PLUGIN@.returnLimit
:    Specify the max amount of events that will be returned for each query.
     When not specified, the default value is 5000.

plugin.@PLUGIN@.storeUrl
:    Specify the path to the directory in which to keep the database. When not
     specified, the default path is jdbc:h2:\<gerrit_site>/data/db.

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
