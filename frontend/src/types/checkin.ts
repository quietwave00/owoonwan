export type CheckinStatus = "PRESENT" | "ABSENT";

export type CheckinResponse = {
  uid: string;
  date: string;
  weekKey: string;
  monthKey: string;
  status: CheckinStatus;
  checkedAt: string;
};

export type CheckinDayResponse = {
  date: string;
  status: CheckinStatus;
  canCheckinAction: boolean;
};

export type CheckinPeriodResponse = {
  uid: string;
  periodKey: string;
  startDate: string;
  endDate: string;
  presentCount: number;
  days: CheckinDayResponse[];
};

export type UserBulkCheckinRequest = {
  date?: string;
  dates?: string[];
};

export type AdminCheckinUserStatusResponse = {
  uid: string;
  nickname: string;
  role: "ADMIN" | "REGULAR";
  status: CheckinStatus;
  checked: boolean;
  checkedAt: string | null;
};

export type AdminCheckinDateResponse = {
  date: string;
  checkedCount: number;
  users: AdminCheckinUserStatusResponse[];
};

export type AdminBulkCheckinRequest = {
  date: string;
  userIds: string[];
};

export type AdminBulkCheckinResponse = {
  date: string;
  processedCount: number;
  checkins: CheckinResponse[];
};
