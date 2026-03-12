import { adminFetch, apiFetch } from "./client";
import type {
  AdminMonthlyStatsResponse,
  AdminWeeklyStatsResponse,
  MonthlyBoardResponse,
  UserMonthlyCalendarResponse,
  UserSummaryResponse,
} from "../types/stats";

export function getMonthlyBoard(month?: string) {
  const query = month ? `?month=${month}` : "";
  return apiFetch<MonthlyBoardResponse>(`/stats/monthly-board${query}`);
}

export function getUserCalendar(uid: string, month?: string) {
  const query = month ? `?month=${month}` : "";
  return apiFetch<UserMonthlyCalendarResponse>(`/stats/users/${uid}/calendar${query}`);
}

export function getUserSummary(uid: string) {
  return apiFetch<UserSummaryResponse>(`/stats/users/${uid}/summary`);
}

export function getAdminWeeklyStats(date?: string) {
  const query = date ? `?date=${date}` : "";
  return adminFetch<AdminWeeklyStatsResponse>(`/admin/stats/weekly${query}`);
}

export function getAdminMonthlyStats(month?: string) {
  const query = month ? `?month=${month}` : "";
  return adminFetch<AdminMonthlyStatsResponse>(`/admin/stats/monthly${query}`);
}
