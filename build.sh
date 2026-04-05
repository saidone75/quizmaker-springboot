#!/bin/bash

set -euo pipefail

mvn clean package -DskipTests -Dlicense.skip=true -Pprod