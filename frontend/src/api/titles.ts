import { adminFetch, apiFetch } from "./client";
import type { AdminTitleVerificationResponse, TitleResponse } from "../types/title";

export function getMyCurrentWeekTitle() {
  return apiFetch<TitleResponse>("/titles/me/current-week");
}

export function getMyCurrentMonthTitle() {
  return apiFetch<TitleResponse>("/titles/me/current-month");
}

export function getUserTitles(uid: string, weekKey?: string, monthKey?: string) {
  const search = new URLSearchParams();
  if (weekKey) search.set("weekKey", weekKey);
  if (monthKey) search.set("monthKey", monthKey);
  const query = search.toString() ? `?${search.toString()}` : "";
  return apiFetch<TitleResponse>(`/titles/users/${uid}${query}`);
}

export function verifyAdminTitles(weekKey?: string, monthKey?: string) {
  const search = new URLSearchParams();
  if (weekKey) search.set("weekKey", weekKey);
  if (monthKey) search.set("monthKey", monthKey);
  const query = search.toString() ? `?${search.toString()}` : "";
  return adminFetch<AdminTitleVerificationResponse>(`/admin/titles/verify${query}`);
}
