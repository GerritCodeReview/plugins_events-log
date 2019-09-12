#This script will be run by bazel when the build process starts to
# generate key-value information that represents the status of the
# workspace. The output should be like
#
# KEY1 VALUE1
# KEY2 VALUE2
#
# If the script exits with non-zero code, it's considered as a failure
# and the output will be discarded.

# WARNING: This script should work with both python2 and python3

from __future__ import print_function
from subprocess import check_output, CalledProcessError
from sys import stderr

try:
  output = check_output(
      ['git', 'describe', '--always', '--match', 'v[0-9].*', '--dirty']).strip().decode("utf-8")
except OSError as err:
  print('could not invoke git: %s' % err, file=stderr)
  exit(1)
except CalledProcessError as err:
  print('error using git: %s' % err, file=stderr)
  exit(1)

print("STABLE_BUILD_EVENTS-LOG_LABEL %s" % output)

