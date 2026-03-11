import { apiFetch } from "./client";
import type { AuthLoginRequest, AuthLoginResponse, AuthMeResponse } from "../types/auth";

export function login(request: AuthLoginRequest) {
  return apiFetch<AuthLoginResponse>("/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function logout() {
  return apiFetch<void>("/auth/logout", {
    method: "POST",
  });
}

export function getMe() {
  return apiFetch<AuthMeResponse>("/auth/me");
}
