#!/bin/bash

set -euo pipefail

DOCKER_COMPOSE="docker-compose -f docker/docker-compose.yml"

usage() {
  echo "Usage: $(basename "$0") {build|build_start|start|stop|restart|purge|tail}"
}

build() {
  docker build . -t quizmaker:latest -f docker/Dockerfile
}

start() {
  eval "$DOCKER_COMPOSE up --build -d"
}

down() {
  eval "$DOCKER_COMPOSE down"
}

tail_logs() {
  eval "$DOCKER_COMPOSE logs -f"
}

purge() {
  eval "$DOCKER_COMPOSE down --rmi local --remove-orphans"
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
    down
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
