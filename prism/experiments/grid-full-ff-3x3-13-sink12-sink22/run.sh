#!/bin/bash
PRISM=../../bin/prism
PARAMS="ne=0:1,nw=0:1,se=0:1,sw=0:1,en=0:1,es=0:1,wn=0:1,ws=0:1"
CMD="$PRISM grid.prism Fgoal.props -param $PARAMS -javamaxmem 50g"

NOW=$(date -u +"%Y-%m-%dT%H-%M-%SZ")
nohup $CMD 1>out.$NOW 2>err.$NOW 
