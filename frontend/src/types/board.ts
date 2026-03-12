import type { CheckinDayResponse } from "./checkins";

export type WeeklyBoardMemberResponse = {
  uid: string;
  nickname: string;
  role: "ADMIN" | "REGULAR";
  days: CheckinDayResponse[];
  weeklyCount: number;
  badges: string[];
};

export type WeeklyBoardResponse = {
  weekKey: string;
  weekStartDate: string;
  weekEndDate: string;
  members: WeeklyBoardMemberResponse[];
};
