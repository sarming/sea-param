#!/bin/bash
# echo "expr={"
grep "^{" $1 |sort -u|sed 's_{_(_; s_|_)/(_; s_}_)_'
# echo "};"
