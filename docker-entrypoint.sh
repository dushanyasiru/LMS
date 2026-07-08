#!/bin/sh
# ============================================================
#  Container entrypoint (Vercel).
#
#  Rewrites SPRING_DATASOURCE_URL from Supabase's SESSION pooler
#  (port 5432 — hard cap of 15 clients, which Vercel's many container
#  instances exhaust -> "EMAXCONNSESSION max clients reached" and the
#  whole app 500s) to the TRANSACTION pooler (port 6543 — multiplexes
#  clients, no per-client cap). PgBouncer transaction mode doesn't
#  support server-side prepared statements, so prepareThreshold=0 is
#  appended too. Both steps are no-ops if the env var is already set
#  to the transaction pooler.
# ============================================================
if [ -n "$SPRING_DATASOURCE_URL" ]; then
  case "$SPRING_DATASOURCE_URL" in
    *:5432*) SPRING_DATASOURCE_URL=$(printf %s "$SPRING_DATASOURCE_URL" | sed 's/:5432/:6543/') ;;
  esac
  case "$SPRING_DATASOURCE_URL" in
    *prepareThreshold*) ;;
    *\?*) SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}&prepareThreshold=0" ;;
    *)    SPRING_DATASOURCE_URL="${SPRING_DATASOURCE_URL}?prepareThreshold=0" ;;
  esac
  export SPRING_DATASOURCE_URL
fi

# Boot with the CDS archive + fast-start flags (see Dockerfile.vercel).
exec /opt/java/openjdk/bin/java -XX:SharedArchiveFile=application.jsa \
  -XX:TieredStopAtLevel=1 -Dserver.port="${PORT:-80}" -jar app.jar
