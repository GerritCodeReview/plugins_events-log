@PLUGIN@ - /events/ REST API
==================

Events Endpoints
----------------

### List Events

'GET /events/'

Lists events that have happened. Will list the _n_ most recent events where _n_
is the limit specified in the plugin configuration. See stream-events command
documentation for the types definition:
[cmd-stream-events](../../../Documentation/cmd-stream-events.html#events)

Request

```
  GET /events/ HTTP/1.0
```

Response

```
  HTTP/1.1 200 OK
  Content-Disposition: attachment
  Content-Type: application/json;charset=UTF-8
  {
    "type":"patchset-created",
    ...
  }
  {
    "type":"comment-added",
    ...
  }
```
#### Options

--From Date/Time (t1)
: Limit the results to the events that happened after the specified date/time.


--To Date/Time (t2)
: Limit the results to the events that happened before the specified date/time.

Format: the date/time arguments are formatted as follows - "yyyy-MM-dd HH:mm:ss".
Can optionally only specify the date - "yyyy-MM-dd".


ACCESS
-------
Any authenticated user.

EXAMPLES
--------

Query the change events which happened between 2014-09-01 and 2014-10-01

>    curl --user joe:secret http://host:port/plugins/@PLUGIN@/events/?t1=2014-09-01;t2=2014-10-01

Query the change events which happened between 2014-10-29 10:00:00 and 2014-10-29 11:00:00

>    curl --user joe:secret "http://host:port/plugins/@PLUGIN@/cevents/?t1=2014-10-29%2010%3A00%3A00.000;t2=2014-10-29%2011%3A00%3A00"

