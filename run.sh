#!/bin/bash

cd "$(dirname "$0")" # go into directory of script

sleep 12
java -jar sound/Configuration.jar --volume -6 --minimal
