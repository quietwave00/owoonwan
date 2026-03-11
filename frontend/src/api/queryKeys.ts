export const queryKeys = {
  auth: {
    all: ["auth"] as const,
    me: (sessionToken: string | null) => ["auth", "me", sessionToken] as const,
  },
  nickname: {
    active: () => ["nickname", "active"] as const,
  },
  checkin: {
    all: ["checkin"] as const,
    myWeek: (date?: string) => ["checkin", "me", "week", date ?? "current"] as const,
    myMonth: (month?: string) => ["checkin", "me", "month", month ?? "current"] as const,
    userMonth: (uid: string, month?: string) => ["checkin", "users", uid, "month", month ?? "current"] as const,
  },
  board: {
    weekly: (date?: string) => ["board", "weekly", date ?? "current"] as const,
  },
  stats: {
    monthlyBoard: (month?: string) => ["stats", "monthly-board", month ?? "current"] as const,
    userCalendar: (uid: string, month?: string) => ["stats", "users", uid, "calendar", month ?? "current"] as const,
    userSummary: (uid: string) => ["stats", "users", uid, "summary"] as const,
    adminWeekly: (date?: string) => ["admin", "stats", "weekly", date ?? "current"] as const,
    adminMonthly: (month?: string) => ["admin", "stats", "monthly", month ?? "current"] as const,
  },
  title: {
    myCurrentWeek: () => ["title", "me", "current-week"] as const,
    myCurrentMonth: () => ["title", "me", "current-month"] as const,
    user: (uid: string, weekKey?: string, monthKey?: string) => ["title", "users", uid, weekKey ?? "current-week", monthKey ?? "current-month"] as const,
    adminVerify: (weekKey?: string, monthKey?: string) => ["admin", "title", "verify", weekKey ?? "current-week", monthKey ?? "current-month"] as const,
  },
  pledge: {
    mine: () => ["pledge", "me"] as const,
    list: () => ["pledge", "list"] as const,
  },
  admin: {
    users: () => ["admin", "users"] as const,
    nicknames: () => ["admin", "nicknames"] as const,
    checkins: (date: string) => ["admin", "checkins", date] as const,
    kakkdugi: () => ["admin", "kakkdugi"] as const,
  },
} as const;
