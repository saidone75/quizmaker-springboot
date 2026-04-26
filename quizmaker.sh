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