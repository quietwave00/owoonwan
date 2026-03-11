import { dispatchUnauthorizedEvent, getSessionToken } from "../app/auth/sessionStorage";
import { ApiError, type ApiResponse } from "../types/api";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request<T>(path: string, init: RequestInit = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, init);
  const body = (await response.json()) as ApiResponse<T>;

  if (!response.ok || !body.success) {
    const error = new ApiError(response.status, body.code, body.message);

    if (response.status === 401) {
      dispatchUnauthorizedEvent();
    }

    throw error;
  }

  return body.data;
}

export function apiFetch<T>(path: string, init: RequestInit = {}) {
  const sessionToken = getSessionToken();

  return request<T>(path, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(sessionToken ? { Authorization: `Bearer ${sessionToken}` } : {}),
      ...(init.headers ?? {}),
    },
  });
}

export function adminFetch<T>(path: string, init: RequestInit = {}) {
  return apiFetch<T>(path, {
    ...init,
    headers: {
      "X-Role": "ADMIN",
      ...(init.headers ?? {}),
    },
  });
}
