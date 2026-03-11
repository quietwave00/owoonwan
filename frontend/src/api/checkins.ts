import { adminFetch, apiFetch } from "./client";
import type {
  AdminBulkCheckinRequest,
  AdminBulkCheckinResponse,
  AdminCheckinDateResponse,
  CheckinPeriodResponse,
  CheckinResponse,
  UserBulkCheckinRequest,
} from "../types/checkin";

export function getMyWeek(date?: string) {
  const query = date ? `?date=${date}` : "";
  return apiFetch<CheckinPeriodResponse>(`/checkins/me/week${query}`);
}

export function getMyMonth(month?: string) {
  const query = month ? `?month=${month}` : "";
  return apiFetch<CheckinPeriodResponse>(`/checkins/me/month${query}`);
}

export function getUserMonth(uid: string, month?: string) {
  const query = month ? `?month=${month}` : "";
  return apiFetch<CheckinPeriodResponse>(`/checkins/users/${uid}/month${query}`);
}

export function checkinDates(request: UserBulkCheckinRequest) {
  return apiFetch<CheckinResponse[]>("/checkins", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function cancelDates(request: UserBulkCheckinRequest) {
  return apiFetch<CheckinResponse[]>("/checkins", {
    method: "DELETE",
    body: JSON.stringify(request),
  });
}

export function checkinToday() {
  return apiFetch<CheckinResponse>("/checkins/today", { method: "POST" });
}

export function cancelToday() {
  return apiFetch<CheckinResponse>("/checkins/today", { method: "DELETE" });
}

export function getAdminCheckinsByDate(date: string) {
  return adminFetch<AdminCheckinDateResponse>(`/admin/checkins?date=${date}`);
}

export function bulkAdminCheckin(request: AdminBulkCheckinRequest) {
  return adminFetch<AdminBulkCheckinResponse>("/admin/checkins", {
    method: "POST",
    body: JSON.stringify(request),
  });
}
