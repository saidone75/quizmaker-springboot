#!/bin/bash

#
# Alice's Simple Quiz Maker - fun quizzes for curious minds
# Copyright (C) 2026 Miss Alice & Saidone
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#

set -euo pipefail

DIST_DIR="quizmaker"
IMAGE_NAME="quizmaker:latest"

get_pom_value() {
  local tag="$1"
  awk -v tag="$tag" '
    /<parent>/ { in_parent=1; next }
    /<\/parent>/ { in_parent=0; next }
    !in_parent && $0 ~ "<" tag ">" {
      line = $0
      sub(".*<" tag ">[[:space:]]*", "", line)
      sub("[[:space:]]*</" tag ">.*", "", line)
      print line
      exit
    }
  ' pom.xml
}

ARTIFACT_ID="$(get_pom_value "artifactId")"
PROJECT_VERSION="$(get_pom_value "version")"

if [[ -z "${ARTIFACT_ID}" || -z "${PROJECT_VERSION}" ]]; then
  echo "❌ Unable to infer artifactId/version from pom.xml"
  exit 1
fi

JAR_NAME="${ARTIFACT_ID}-${PROJECT_VERSION}.jar"

rm -rf "${DIST_DIR}"
mkdir -p "${DIST_DIR}/log"

echo "[1/4] Building Docker image..."
docker build -t "${IMAGE_NAME}" . -f docker/Dockerfile

echo "[2/4] Creating temporary container..."
CONTAINER_ID="$(docker create "${IMAGE_NAME}")"

echo "[3/4] Extracting ${JAR_NAME} file from container..."
docker cp "${CONTAINER_ID}:/app.jar" "./${DIST_DIR}/${JAR_NAME}"

echo "[4/4] Cleaning up temporary container..."
docker rm "${CONTAINER_ID}" >/dev/null

echo
echo "✅ ${JAR_NAME} successfully extracted in directory ${DIST_DIR}!"
