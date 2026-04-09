#!/bin/bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SPRING_PROFILE="${SPRING_PROFILE:-dev}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:-changeme}"
AUTO_REGISTER_TEACHER_ON_LOGIN_FAIL="${AUTO_REGISTER_TEACHER_ON_LOGIN_FAIL:-1}"
START_APP="${START_APP:-1}"
DISABLE_TURNSTILE="${DISABLE_TURNSTILE:-1}"

WORKDIR="$(mktemp -d)"
TEACHER_COOKIES="$WORKDIR/teacher.cookies"
STUDENT_COOKIES="$WORKDIR/student.cookies"
APP_LOG="$WORKDIR/app.log"
APP_PID=""
QUIZ_ID=""
TEACHER_USERNAME="$ADMIN_USERNAME"
TEACHER_PASSWORD="$ADMIN_PASSWORD"
LAST_TEACHER_LOGIN_URL=""
TEACHER_CSRF_TOKEN=""
TEACHER_CSRF_HEADER=""
STUDENT_CSRF_TOKEN=""
STUDENT_CSRF_HEADER=""

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

extract_csrf_hidden_field() {
  local html="$1"
  echo "$html" | sed -n 's/.*name="_csrf"[[:space:]]\+value="\([^"]*\)".*/\1/p' | head -n1
}

extract_meta_content() {
  local html="$1"
  local meta_name="$2"
  echo "$html" | sed -n "s/.*<meta name=\"$meta_name\" content=\"\\([^\"]*\\)\".*/\\1/p" | head -n1
}

print_header() {
  echo
  echo "== $* =="
}

require_json_object_response() {
  local context="$1"
  local payload="$2"

  PAYLOAD="$payload" python3 - "$context" <<'PY'
import json
import os
import sys

context = sys.argv[1]
payload = os.environ.get("PAYLOAD", "")

if not payload.strip():
    print(f"Errore: risposta vuota durante '{context}'.", file=sys.stderr)
    sys.exit(1)

try:
    data = json.loads(payload)
except json.JSONDecodeError as exc:
    preview = payload[:300].replace("\n", "\\n")
    print(
        f"Errore: risposta non JSON durante '{context}' "
        f"(preview: {preview!r}, dettaglio: {exc}).",
        file=sys.stderr,
    )
    sys.exit(1)

if not isinstance(data, dict):
    print(
        f"Errore: risposta JSON inattesa durante '{context}' "
        f"(tipo: {type(data).__name__}).",
        file=sys.stderr,
    )
    sys.exit(1)
PY
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
  local -a run_args=()
  if [[ "$DISABLE_TURNSTILE" == "1" ]]; then
    env_prefix+=("TURNSTILE_ENABLED=false")
    env_prefix+=("APP_TURNSTILE_ENABLED=false")
    run_args+=("--app.turnstile.enabled=false")
  fi

  # shellcheck disable=SC2086
  env ${env_prefix[*]} mvn -q spring-boot:run \
    -Dspring-boot.run.profiles="$SPRING_PROFILE" \
    -Dspring-boot.run.arguments="${run_args[*]}" >"$APP_LOG" 2>&1 &
  APP_PID=$!

  wait_for_app
}

teacher_login() {
  print_header "Teacher login"

  attempt_teacher_login "$TEACHER_USERNAME" "$TEACHER_PASSWORD" && return 0

  if [[ "$AUTO_REGISTER_TEACHER_ON_LOGIN_FAIL" != "1" ]]; then
    echo "Errore: login teacher fallito con utente '$TEACHER_USERNAME'." >&2
    exit 1
  fi

  print_header "Teacher auto-register fallback"
  local attempt
  for attempt in $(seq 1 5); do
    register_teacher_for_smoke || exit 1
    attempt_teacher_login "$TEACHER_USERNAME" "$TEACHER_PASSWORD" && return 0
  done

  echo "Errore: login teacher fallito anche dopo 5 tentativi di auto-registrazione (ultimo utente '$TEACHER_USERNAME', url finale: '$LAST_TEACHER_LOGIN_URL')." >&2
  exit 1
}

attempt_teacher_login() {
  local username="$1"
  local password="$2"

  local login_html
  login_html="$(curl -fsS -c "$TEACHER_COOKIES" "$BASE_URL/teacher/login")"

  local csrf
  csrf="$(extract_csrf_hidden_field "$login_html")"
  [[ -n "$csrf" ]] || { echo "Errore: CSRF login teacher non trovato" >&2; exit 1; }

  curl -fsS -L \
    -b "$TEACHER_COOKIES" -c "$TEACHER_COOKIES" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "username=$username" \
    --data-urlencode "password=$password" \
    --data-urlencode "_csrf=$csrf" \
    "$BASE_URL/teacher/login" >/dev/null

  LAST_TEACHER_LOGIN_URL="$(curl -fsS -o /dev/null -w '%{url_effective}' -b "$TEACHER_COOKIES" -L "$BASE_URL/teacher")"
  [[ "$LAST_TEACHER_LOGIN_URL" != *"/teacher/login"* ]]
}

register_teacher_for_smoke() {
  local suffix
  suffix="$(date +%s)-$RANDOM"
  TEACHER_USERNAME="smoke${suffix//-/}"
  TEACHER_PASSWORD="smoke${suffix//-/}"

  local register_html
  register_html="$(curl -fsS -c "$TEACHER_COOKIES" "$BASE_URL/teacher/register")"

  local csrf
  csrf="$(extract_csrf_hidden_field "$register_html")"
  [[ -n "$csrf" ]] || { echo "Errore: CSRF register teacher non trovato" >&2; exit 1; }

  local register_body
  register_body="$(curl -fsS -L \
    -b "$TEACHER_COOKIES" -c "$TEACHER_COOKIES" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "username=$TEACHER_USERNAME" \
    --data-urlencode "password=$TEACHER_PASSWORD" \
    --data-urlencode "confirmPassword=$TEACHER_PASSWORD" \
    --data-urlencode "_csrf=$csrf" \
    "$BASE_URL/teacher/register")"

  if [[ "$register_body" == *"Verifica CAPTCHA non riuscita"* ]]; then
    echo "Errore: auto-registrazione fallita (CAPTCHA attivo). Imposta credenziali valide via ADMIN_USERNAME/ADMIN_PASSWORD oppure disabilita Turnstile." >&2
    return 1
  fi

  echo "Teacher registrato automaticamente: $TEACHER_USERNAME"
  return 0
}

refresh_teacher_csrf_headers() {
  local page_html
  page_html="$(curl -fsS -b "$TEACHER_COOKIES" -c "$TEACHER_COOKIES" "$BASE_URL/teacher/students")"

  TEACHER_CSRF_TOKEN="$(extract_meta_content "$page_html" "quizmaker-csrf-token")"
  TEACHER_CSRF_HEADER="$(extract_meta_content "$page_html" "quizmaker-csrf-header")"
  [[ -n "$TEACHER_CSRF_TOKEN" && -n "$TEACHER_CSRF_HEADER" ]] || {
    echo "Errore: CSRF meta teacher non trovati nella pagina admin." >&2
    exit 1
  }
}

refresh_student_csrf_headers() {
  local page_html
  page_html="$(curl -fsS -b "$STUDENT_COOKIES" -c "$STUDENT_COOKIES" "$BASE_URL/")"

  STUDENT_CSRF_TOKEN="$(extract_meta_content "$page_html" "quizmaker-csrf-token")"
  STUDENT_CSRF_HEADER="$(extract_meta_content "$page_html" "quizmaker-csrf-header")"
  [[ -n "$STUDENT_CSRF_TOKEN" && -n "$STUDENT_CSRF_HEADER" ]] || {
    echo "Errore: CSRF meta studente non trovati nella pagina student." >&2
    exit 1
  }
}

create_student() {
  print_header "Create student"
  refresh_teacher_csrf_headers

  local student_json
  student_json="$(curl -fsSL \
    -b "$TEACHER_COOKIES" \
    -H "$TEACHER_CSRF_HEADER: $TEACHER_CSRF_TOKEN" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -d '{"fullName":"Smoke Student"}' \
    "$BASE_URL/api/students")"

  require_json_object_response "create student" "$student_json"

  STUDENT_ID="$(echo "$student_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')"
  STUDENT_KEYWORD="$(echo "$student_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["loginKeyword"])')"

  echo "Student ID: $STUDENT_ID"
  echo "Student keyword: $STUDENT_KEYWORD"
}

create_and_publish_quiz() {
  print_header "Create quiz"
  refresh_teacher_csrf_headers

  local quiz_json
  quiz_json="$(curl -fsSL \
    -b "$TEACHER_COOKIES" \
    -H "$TEACHER_CSRF_HEADER: $TEACHER_CSRF_TOKEN" \
    -H "Accept: application/json" \
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

  require_json_object_response "create quiz" "$quiz_json"

  QUIZ_ID="$(echo "$quiz_json" | python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])')"
  echo "Quiz ID: $QUIZ_ID"

  print_header "Publish quiz"
  refresh_teacher_csrf_headers

  curl -fsS \
    -X PUT \
    -b "$TEACHER_COOKIES" \
    -H "$TEACHER_CSRF_HEADER: $TEACHER_CSRF_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{"published":true}' \
    "$BASE_URL/api/quizzes/$QUIZ_ID/publication" >/dev/null
}

student_login_and_submit() {
  print_header "Student login"

  local login_html
  login_html="$(curl -fsS -c "$STUDENT_COOKIES" "$BASE_URL/")"

  local csrf
  csrf="$(extract_csrf_hidden_field "$login_html")"
  [[ -n "$csrf" ]] || { echo "Errore: CSRF studente non trovato" >&2; exit 1; }

  curl -fsS -L \
    -b "$STUDENT_COOKIES" -c "$STUDENT_COOKIES" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    --data-urlencode "keyword=$STUDENT_KEYWORD" \
    --data-urlencode "_csrf=$csrf" \
    "$BASE_URL/student/login" >/dev/null

  print_header "Student list published quizzes"
  refresh_student_csrf_headers
  local quizzes_json
  quizzes_json="$(curl -fsSL \
    -b "$STUDENT_COOKIES" \
    -H "$STUDENT_CSRF_HEADER: $STUDENT_CSRF_TOKEN" \
    -H "Accept: application/json" \
    "$BASE_URL/api/quizzes")"

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
  submit_json="$(curl -fsSL \
    -b "$STUDENT_COOKIES" \
    -H "$STUDENT_CSRF_HEADER: $STUDENT_CSRF_TOKEN" \
    -H "Accept: application/json" \
    -H "Content-Type: application/json" \
    -d '{"answers":[1]}' \
    "$BASE_URL/api/quizzes/$QUIZ_ID/submit")"

  require_json_object_response "submit quiz" "$submit_json"

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
