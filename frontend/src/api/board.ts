import { apiFetch } from "./client";
import type { WeeklyBoardResponse } from "../types/board";

export function getWeeklyBoard(date?: string) {
  const query = date ? `?date=${date}` : "";
  return apiFetch<WeeklyBoardResponse>(`/board/weekly${query}`);
}
