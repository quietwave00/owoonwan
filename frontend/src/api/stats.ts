import { adminFetch, apiFetch } from "./client";
import type { MonthlyBoardResponse, UserMonthlyCalendarResponse, UserSummaryResponse } from "../types/stats";
import type { WeeklyBoardResponse } from "../types/board";

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
  return adminFetch<WeeklyBoardResponse>(`/admin/stats/weekly${query}`);
}

export function getAdminMonthlyStats(month?: string) {
  const query = month ? `?month=${month}` : "";
  return adminFetch<MonthlyBoardResponse>(`/admin/stats/monthly${query}`);
}
