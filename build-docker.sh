#!/bin/bash

set -euo pipefail

DIST_DIR="quizmaker"
IMAGE_NAME="quizmaker:latest"
JAR_NAME="quizmaker.jar"

rm -rf "${DIST_DIR}"
mkdir -p "${DIST_DIR}/log"

echo "[1/4] Building Docker image..."
docker build -t "${IMAGE_NAME}" . -f docker/Dockerfile

echo "[2/4] Creating temporary container..."
CONTAINER_ID="$(docker create "${IMAGE_NAME}")"

echo "[3/4] Extracting ${JAR_NAME} file from container..."
docker cp "${CONTAINER_ID}:/${JAR_NAME}" "./${DIST_DIR}/${JAR_NAME}"

echo "[4/4] Cleaning up temporary container..."
docker rm "${CONTAINER_ID}" >/dev/null

echo
echo "✅ ${JAR_NAME} successfully extracted in directory ${DIST_DIR}!"
