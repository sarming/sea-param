#!/bin/bash
PRISM=../../bin/prism
PARAMS="p=0:1"
CMD="$PRISM grid.prism Fgoal.props -param $PARAMS -javamaxmem 50g"

NOW=$(date -u +"%Y-%m-%dT%H-%M-%SZ")
nohup $CMD 1>out.$NOW 2>err.$NOW &
