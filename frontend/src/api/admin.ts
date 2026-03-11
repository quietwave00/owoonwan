import { adminFetch } from "./client";
import type { AdminSpecialTitleResponse, AdminUser, NicknameResponse } from "../types/admin";

export function getAdminUsers() {
  return adminFetch<AdminUser[]>("/admin/users");
}

export function getAdminNicknames() {
  return adminFetch<NicknameResponse[]>("/admin/nicknames");
}

export function createAdminNickname(display: string) {
  return adminFetch<NicknameResponse>("/admin/nicknames", {
    method: "POST",
    body: JSON.stringify({ display }),
  });
}

export function assignKakkdugi(uid: string) {
  return adminFetch<AdminSpecialTitleResponse>(`/admin/users/${uid}/special-titles/kakkdugi`, {
    method: "POST",
  });
}

export function revokeKakkdugi(uid: string) {
  return adminFetch<AdminSpecialTitleResponse>(`/admin/users/${uid}/special-titles/kakkdugi`, {
    method: "DELETE",
  });
}
