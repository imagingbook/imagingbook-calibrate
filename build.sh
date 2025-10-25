#!/bin/bash

mvn clean install -Dmaven.test.skip=true

echo ""
read -rsp $'Done. Press any key to quit...\n' -n1 key
