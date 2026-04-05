#!/bin/bash

set -euo pipefail

COMPOSE_FILE="docker/docker-compose.yml"

if command -v docker-compose >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD=(docker-compose -f "$COMPOSE_FILE")
elif docker compose version >/dev/null 2>&1; then
  DOCKER_COMPOSE_CMD=(docker compose -f "$COMPOSE_FILE")
else
  echo "Errore: né 'docker-compose' né 'docker compose' sono disponibili." >&2
  exit 1
fi

usage() {
  echo "Usage: $(basename "$0") {build|build_start|start|stop|restart|purge|tail}"
}

compose() {
  "${DOCKER_COMPOSE_CMD[@]}" "$@"
}

build() {
  docker build . -t quizmaker:latest -f docker/Dockerfile
}

start() {
  compose up --build -d
}

down() {
  compose down
}

tail_logs() {
  compose logs -f
}

purge() {
  compose down --rmi local --remove-orphans
}

if [[ $# -eq 0 ]]; then
  usage
  exit 1
fi

case "${1}" in
  build)
    build
    ;;
  build_start)
    build
    start
    tail_logs
    ;;
  start)
    start
    tail_logs
    ;;
  stop)
    down
    ;;
  restart)
    down
    start
    tail_logs
    ;;
  purge)
    purge
    ;;
  tail)
    tail_logs
    ;;
  *)
    usage
    exit 1
    ;;
esac