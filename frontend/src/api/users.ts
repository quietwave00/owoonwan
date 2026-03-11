import { apiFetch } from "./client";
import type { UserCreateRequest, UserResponse } from "../types/user";

export function createUser(request: UserCreateRequest) {
  return apiFetch<UserResponse>("/users", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function selectNickname(uid: string, nicknameId: string) {
  return apiFetch<UserResponse>(`/users/${uid}/nickname`, {
    method: "POST",
    headers: {
      "X-User-Id": uid,
    },
    body: JSON.stringify({ nicknameId }),
  });
}
