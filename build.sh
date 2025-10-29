#!/bin/bash

mvn clean install -Dmaven.test.skip=true
# mvn clean install -Dimagingbook.skipjavadoc=true
# mvn clean install -Dmaven.test.skip=true -Dimagingbook.skipjavadoc=true

echo ""
read -rsp $'Done. Press any key to quit...\n' -n1 key
