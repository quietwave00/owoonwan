import { adminFetch } from "./client";
import type { AdminSpecialTitleResponse, AdminUser, NicknameResponse } from "../types/admin";

export function getAdminUsers() {
  return adminFetch<AdminUser[]>("/api/admin/users");
}

export function getAdminNicknames() {
  return adminFetch<NicknameResponse[]>("/api/admin/nicknames");
}

export function createAdminNickname(display: string) {
  return adminFetch<NicknameResponse>("/api/admin/nicknames", {
    method: "POST",
    body: JSON.stringify({ display }),
  });
}

export function assignKakkdugi(uid: string) {
  return adminFetch<AdminSpecialTitleResponse>(`/api/admin/users/${uid}/special-titles/kakkdugi`, {
    method: "POST",
  });
}

export function revokeKakkdugi(uid: string) {
  return adminFetch<AdminSpecialTitleResponse>(`/api/admin/users/${uid}/special-titles/kakkdugi`, {
    method: "DELETE",
  });
}
