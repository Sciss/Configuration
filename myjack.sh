#!/bin/bash

cd "$(dirname "$0")" # go into directory of script

jackd $@ # &> >(java -jar suicide/suicide.jar)
