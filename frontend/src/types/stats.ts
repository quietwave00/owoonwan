import type { CheckinDayResponse } from "./checkins";

export type MonthlyBoardMemberResponse = {
  uid: string;
  nickname: string;
  role: "ADMIN" | "REGULAR";
  monthlyCount: number;
};

export type MonthlyBoardResponse = {
  monthKey: string;
  members: MonthlyBoardMemberResponse[];
};

export type UserMonthlyCalendarResponse = {
  uid: string;
  nickname: string;
  monthKey: string;
  days: CheckinDayResponse[];
  monthlyCount: number;
  weeklyCounts: number[];
};

export type UserSummaryResponse = {
  uid: string;
  nickname: string;
  currentWeekKey: string;
  currentWeekCount: number;
  previousWeekKey: string;
  previousWeekCount: number;
  currentMonthKey: string;
  currentMonthCount: number;
  previousMonthKey: string;
  previousMonthCount: number;
};
