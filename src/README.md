# Owoonwan

Spring Boot + Firebase Firestore MVP.

Key docs:
- [Firestore MVP schema and rules](docs/firestore-mvp.md)

Admin endpoints (stubbed):
- `POST /admin/recompute` with headers `X-Role: ADMIN`, optional `X-Admin-Uid`, params `weekKey`, `monthKey`.
- `POST /admin/login-lock` with header `X-Role: ADMIN`, body `{ nickname, uid, sessionId }`.

Utility:
- `TimeKeyUtil` derives KST date/week/month keys for checkins/titles.

TODO:
- Replace stubs with Firestore Admin SDK (loginLocks transaction, checkins aggregation, auditLogs writes).
- Wire real auth instead of `X-Role`/`X-Admin-Uid` headers.
