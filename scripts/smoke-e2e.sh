#!/bin/bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SPRING_PROFILE="${SPRING_PROFILE:-dev}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-changeme}"
START_APP="${START_APP:-1}"
DISABLE_TURNSTILE="${DISABLE_TURNSTILE:-1}"

WORKDIR="$(mktemp -d)"
TEACHER_COOKIES="$WORKDIR/teacher.cookies"
STUDENT_COOKIES="$WORKDIR/student.cookies"
APP_LOG="$WORKDIR/app.log"
APP_PID=""
QUIZ_ID=""

cleanup() {
  if [[ -n "${APP_PID}" ]]; then
    kill "${APP_PID}" >/dev/null 2>&1 || true
  fi
  rm -rf "$WORKDIR"
}
trap cleanup EXIT

require_cmd() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Errore: comando richiesto non trovato: $1" >&2
    exit 1
  }
}

extract_xsrf_from_cookie_jar() {
  local cookie_jar="$1"
  awk '$6=="XSRF-TOKEN"{print $7}' "$cookie_jar" | tail -n1
}

print_header() {
  echo
  echo "== $* =="
}

wait_for_app() {
  for _ in $(seq 1 90); do
    if curl -fsS "$BASE_URL/" >/dev/null 2>&1; then
      return 0
    fi
    sleep 1
  done

  echo "Errore: applicazione non disponibile su $BASE_URL entro 90 secondi." >&2
  if [[ -f "$APP_LOG" ]]; then
    echo "--- ultimi log app ---" >&2
    tail -n 60 "$APP_LOG" >&2 || true
  fi
  return 1
}

start_app_if_requested() {
  if [[ "$START_APP" != "1" ]]; then
    return 0
  fi

  print_header "Start app ($SPRING_PROFILE)"

  local -a env_prefix=()
  if [[ "$DISABLE_TURNSTILE" == "1" ]]; then
    env_prefix+=("TURNSTILE_ENABLED=false")
  fi

  # shellcheck disable=SC2086
  env ${env_prefix[*]} mvn -q spring-boot:run -Dspring-boot.run.profiles="$SPRING_PROFILE" >"$APP_LOG" 2>&1 &
  APP_PID=$!

  wait_for_app
}

teacher_login() {
  print_header "Teacher login"

  curl -fsS -c "$TEACHER_COOKIES" "$BASE_URL/teacher/login" >/dev/null

  local xsrf
  xsrf="$(extract_xsrf_from_cookie_jar "$TEACHER_COOKIES")"
  [[ -n "$xsrf" ]] || { echo "Errore: XSRF teacher non trovato" >&2; exit 1; }

  curl -fsS -L \
    -b "$TEACHER_COOKIES" -c "$TEACHER_COOKIES" \
    -H "X-XSRF-TOKEN: $xsrf" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "username=$ADMIN_USERNAME" \
    --data-urlencode "password=$ADMIN_PASSWORD" \
    --data-urlencode "_csrf=$xsrf" \
    "$BASE_URL/teacher/login" >/dev/null
}

create_student() {
  print_header "Create student"

  local xsrf
  xsrf="$(extract_xsrf_from_cookie_jar "$TEACHER_COOKIES")"

  local student_json
  student_json="$(curl -fsS \
    -b "$TEACHER_COOKIES" \
    -H "X-XSRF-TOKEN: $xsrf" \
    -H "Content-Type: application/json" \
    -d '{"fullName":"Smoke Student"}' \
    "$BASE_URL/api/students")"

  STUDENT_ID="$(echo "$student_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')"
  STUDENT_KEYWORD="$(echo "$student_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["loginKeyword"])')"

  echo "Student ID: $STUDENT_ID"
  echo "Student keyword: $STUDENT_KEYWORD"
}

create_and_publish_quiz() {
  print_header "Create quiz"

  local xsrf
  xsrf="$(extract_xsrf_from_cookie_jar "$TEACHER_COOKIES")"

  local quiz_json
  quiz_json="$(curl -fsS \
    -b "$TEACHER_COOKIES" \
    -H "X-XSRF-TOKEN: $xsrf" \
    -H "Content-Type: application/json" \
    -d '{
      "title":"Smoke Quiz",
      "emoji":"🧪",
      "questions":[
        {
          "text":"2+2?",
          "emoji":"➕",
          "options":["3","4","5","6"],
          "answer":1,
          "feedback":"2+2=4"
        }
      ]
    }' \
    "$BASE_URL/api/quizzes")"

  QUIZ_ID="$(echo "$quiz_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')"
  echo "Quiz ID: $QUIZ_ID"

  print_header "Publish quiz"
  xsrf="$(extract_xsrf_from_cookie_jar "$TEACHER_COOKIES")"

  curl -fsS \
    -X PUT \
    -b "$TEACHER_COOKIES" \
    -H "X-XSRF-TOKEN: $xsrf" \
    -H "Content-Type: application/json" \
    -d '{"published":true}' \
    "$BASE_URL/api/quizzes/$QUIZ_ID/publication" >/dev/null
}

student_login_and_submit() {
  print_header "Student login"

  curl -fsS -c "$STUDENT_COOKIES" "$BASE_URL/" >/dev/null

  local xsrf
  xsrf="$(extract_xsrf_from_cookie_jar "$STUDENT_COOKIES")"
  [[ -n "$xsrf" ]] || { echo "Errore: XSRF studente non trovato" >&2; exit 1; }

  curl -fsS -L \
    -b "$STUDENT_COOKIES" -c "$STUDENT_COOKIES" \
    -H "X-XSRF-TOKEN: $xsrf" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "keyword=$STUDENT_KEYWORD" \
    --data-urlencode "_csrf=$xsrf" \
    "$BASE_URL/student/login" >/dev/null

  print_header "Student list published quizzes"
  local quizzes_json
  quizzes_json="$(curl -fsS -b "$STUDENT_COOKIES" "$BASE_URL/api/quizzes")"

  QUIZZES_JSON="$quizzes_json" python3 - <<'PY'
import json
import os

arr = json.loads(os.environ["QUIZZES_JSON"])
print(f"Published quizzes: {len(arr)}")
for q in arr:
    print(f"- {q['id']} {q['title']}")
PY

  print_header "Student submit"
  local submit_json
  submit_json="$(curl -fsS \
    -b "$STUDENT_COOKIES" \
    -H "Content-Type: application/json" \
    -d '{"answers":[1]}' \
    "$BASE_URL/api/quizzes/$QUIZ_ID/submit")"

  SUBMIT_JSON="$submit_json" python3 - <<'PY'
import json
import os

j = json.loads(os.environ["SUBMIT_JSON"])
print(f"Score: {j['score']}/{j['total']}, locked={j['locked']}")
PY
}

main() {
  require_cmd curl
  require_cmd python3

  start_app_if_requested
  teacher_login
  create_student
  create_and_publish_quiz
  student_login_and_submit

  echo
  echo "✅ SMOKE E2E COMPLETATO"
}

main "$@"
