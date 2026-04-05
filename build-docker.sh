#!/bin/bash

set -euo pipefail

DIST_DIR="quizmaker"
IMAGE_NAME="quizmaker:latest"

get_pom_value() {
  local tag="$1"
  awk -v tag="$tag" '
    /<parent>/ { in_parent=1; next }
    /<\/parent>/ { in_parent=0; next }
    !in_parent {
      pattern = "<" tag ">[[:space:]]*([^<]+)[[:space:]]*</" tag ">"
      if (match($0, pattern, m)) {
        print m[1]
        exit
      }
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
