import { useMemo, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { toPng } from "html-to-image";
import { getAdminMonthlyStats, getAdminWeeklyStats } from "../../api/stats";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import { Badge, Button, PageIntro, StateCard, SurfaceCard } from "../../components/common/Ui";
import "./AdminStatsPage.css";

type StatsTab = "weekly" | "monthly";

type StatsRow = {
  uid: string;
  nickname: string;
  count: number;
  badges: string[];
};

const KKAKDUGI = "\uAE4D\uB450\uAE30";

function getVisibleBadges(badges: string[]) {
  return badges.includes(KKAKDUGI) ? [KKAKDUGI] : badges;
}

function getKstToday() {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  return formatter.format(new Date());
}

function getCurrentMonthKey() {
  return getKstToday().slice(0, 7);
}

function shiftDate(date: string, amount: number) {
  const [year, month, day] = date.split("-").map(Number);
  const next = new Date(Date.UTC(year, month - 1, day + amount));
  return next.toISOString().slice(0, 10);
}

function shiftMonth(monthKey: string, amount: number) {
  const [year, month] = monthKey.split("-").map(Number);
  const next = new Date(Date.UTC(year, month - 1 + amount, 1));
  return next.toISOString().slice(0, 7);
}

function formatWeekRange(startDate: string, endDate: string) {
  const [startYear, startMonth, startDay] = startDate.split("-").map(Number);
  const [, endMonth, endDay] = endDate.split("-").map(Number);

  if (startMonth === endMonth) {
    return `${startYear}.${startMonth}.${startDay} - ${endDay}`;
  }

  return `${startYear}.${startMonth}.${startDay} - ${endMonth}.${endDay}`;
}

function formatMonthLabel(monthKey: string) {
  const [year, month] = monthKey.split("-").map(Number);
  return `${year}.${month}`;
}

function buildWeeklyRows(members: Array<{ uid: string; nickname: string; count: number; badges?: string[] | null }>) {
  return [...members]
    .map<StatsRow>((member) => ({
      uid: member.uid,
      nickname: member.nickname,
      count: member.count,
      badges: Array.isArray(member.badges) ? member.badges : [],
    }))
    .sort((left, right) => right.count - left.count || left.nickname.localeCompare(right.nickname, "ko"));
}

function buildMonthlyRows(members: Array<{ uid: string; nickname: string; count: number; badges?: string[] | null }>) {
  return [...members]
    .map<StatsRow>((member) => ({
      uid: member.uid,
      nickname: member.nickname,
      count: member.count,
      badges: Array.isArray(member.badges) ? member.badges : [],
    }))
    .sort((left, right) => right.count - left.count || left.nickname.localeCompare(right.nickname, "ko"));
}

async function downloadElementAsImage(element: HTMLElement, fileName: string) {
  const width = element.scrollWidth;
  const height = element.scrollHeight;

  const dataUrl = await toPng(element, {
    cacheBust: false,
    pixelRatio: 1,
    backgroundColor: "#fff8f0",
    skipFonts: true,
    width,
    height,
    style: {
      width: `${width}px`,
      minWidth: `${width}px`,
    },
  });

  const link = document.createElement("a");
  link.download = fileName;
  link.href = dataUrl;
  document.body.appendChild(link);
  link.click();
  link.remove();
}

export function AdminStatsPage() {
  const captureRef = useRef<HTMLDivElement | null>(null);
  const today = useMemo(() => getKstToday(), []);
  const [tab, setTab] = useState<StatsTab>("weekly");
  const [selectedDate, setSelectedDate] = useState(today);
  const [selectedMonth, setSelectedMonth] = useState(getCurrentMonthKey());
  const [isExporting, setIsExporting] = useState(false);

  const weeklyStatsQuery = useQuery({
    queryKey: queryKeys.stats.adminWeekly(selectedDate),
    queryFn: () => getAdminWeeklyStats(selectedDate),
    enabled: tab === "weekly",
  });

  const monthlyStatsQuery = useQuery({
    queryKey: queryKeys.stats.adminMonthly(selectedMonth),
    queryFn: () => getAdminMonthlyStats(selectedMonth),
    enabled: tab === "monthly",
  });

  const isLoading = tab === "weekly" ? weeklyStatsQuery.isLoading : monthlyStatsQuery.isLoading;
  const isError = tab === "weekly" ? weeklyStatsQuery.isError : monthlyStatsQuery.isError;

  const errorMessage = useMemo(() => {
    const candidates = [weeklyStatsQuery.error, monthlyStatsQuery.error];
    const apiError = candidates.find((error): error is ApiError => error instanceof ApiError);
    return apiError?.message ?? "\uD1B5\uACC4 \uB370\uC774\uD130\uB97C \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4.";
  }, [monthlyStatsQuery.error, weeklyStatsQuery.error]);

  const rows = useMemo(() => {
    if (tab === "weekly") {
      return buildWeeklyRows(weeklyStatsQuery.data?.members ?? []);
    }

    return buildMonthlyRows(monthlyStatsQuery.data?.members ?? []);
  }, [monthlyStatsQuery.data?.members, tab, weeklyStatsQuery.data?.members]);

  const previewLabel = tab === "weekly"
    ? weeklyStatsQuery.data ? formatWeekRange(weeklyStatsQuery.data.weekStartDate, weeklyStatsQuery.data.weekEndDate) : "-"
    : formatMonthLabel(selectedMonth);

  const handleExport = async () => {
    if (!captureRef.current) {
      return;
    }

    try {
      setIsExporting(true);
      await downloadElementAsImage(
        captureRef.current,
        tab === "weekly" ? `admin-weekly-${selectedDate}.png` : `admin-monthly-${selectedMonth}.png`,
      );
    } catch {
      window.alert("\uC774\uBBF8\uC9C0 \uC800\uC7A5\uC5D0 \uC2E4\uD328\uD588\uC2B5\uB2C8\uB2E4.");
    } finally {
      setIsExporting(false);
    }
  };

  if (isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="loading"
          title={"\uD1B5\uACC4 \uD654\uBA74\uC744 \uC900\uBE44\uD558\uACE0 \uC788\uC2B5\uB2C8\uB2E4"}
          description={"\uC120\uD0DD\uD55C \uAE30\uAC04\uC758 \uC9D1\uACC4\uB97C \uBD88\uB7EC\uC624\uB294 \uC911\uC785\uB2C8\uB2E4."}
        />
      </main>
    );
  }

  if (isError) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="error"
          title={"\uD1B5\uACC4 \uD654\uBA74\uC744 \uBD88\uB7EC\uC624\uC9C0 \uBABB\uD588\uC2B5\uB2C8\uB2E4"}
          description={errorMessage}
          action={
            <Button
              size="sm"
              onClick={() => {
                void weeklyStatsQuery.refetch();
                void monthlyStatsQuery.refetch();
              }}
            >
              {"\uB2E4\uC2DC \uC2DC\uB3C4"}
            </Button>
          }
        />
      </main>
    );
  }

  return (
    <main className="page-shell admin-stats-page">
      <div ref={captureRef}>
        <SurfaceCard>
          <PageIntro
            eyebrow="Admin"
            title={"\uD1B5\uACC4"}
            actions={
              <div className="admin-stats-page__actions">
                <button
                  type="button"
                  className={`ui-badge admin-stats-page__tab${tab === "weekly" ? " admin-stats-page__tab--active" : ""}`}
                  onClick={() => setTab("weekly")}
                >
                  {"\uC8FC\uAC04"}
                </button>
                <button
                  type="button"
                  className={`ui-badge admin-stats-page__tab${tab === "monthly" ? " admin-stats-page__tab--active" : ""}`}
                  onClick={() => setTab("monthly")}
                >
                  {"\uC6D4\uAC04"}
                </button>
              </div>
            }
          />

          <div className="admin-stats-page__toolbar">
            <div className="weekly-board-nav">
              <button
                type="button"
                className="weekly-board-nav__arrow"
                aria-label={tab === "weekly" ? "\uC774\uC804 \uC8FC" : "\uC774\uC804 \uB2EC"}
                onClick={() => {
                  if (tab === "weekly") {
                    setSelectedDate(shiftDate(selectedDate, -7));
                    return;
                  }
                  setSelectedMonth(shiftMonth(selectedMonth, -1));
                }}
              >
                {"\u2039"}
              </button>
              <div className="weekly-board-nav__summary">
                <Badge variant="muted">{previewLabel}</Badge>
              </div>
              <button
                type="button"
                className="weekly-board-nav__arrow"
                aria-label={tab === "weekly" ? "\uB2E4\uC74C \uC8FC" : "\uB2E4\uC74C \uB2EC"}
                onClick={() => {
                  if (tab === "weekly") {
                    setSelectedDate(shiftDate(selectedDate, 7));
                    return;
                  }
                  setSelectedMonth(shiftMonth(selectedMonth, 1));
                }}
              >
                {"\u203A"}
              </button>
            </div>
          </div>

          <div className="ui-table admin-stats-page__table-shell">
            <div className="ui-table__inner admin-stats-page__capture">
              <table>
                <thead>
                  <tr>
                    <th>{"\uB2C9\uB124\uC784"}</th>
                    <th>{"\uD69F\uC218"}</th>
                    <th>{"\uD0C0\uC774\uD2C0"}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row, index) => {
                    const visibleBadges = getVisibleBadges(row.badges);
                    const isLeader = index === 0 && row.count > 0;

                    return (
                      <tr key={row.uid}>
                        <td><strong>{row.nickname}</strong>{isLeader ? <span aria-hidden="true"> {"\uD83D\uDC51"}</span> : null}</td>
                        <td>{`${row.count}\uD68C`}</td>
                        <td>
                          <div className="admin-stats-page__badges">
                            {visibleBadges.length > 0 ? visibleBadges.map((badge) => (
                              <Badge key={`${row.uid}-${badge}`} variant="accent">{badge}</Badge>
                            )) : <span className="admin-stats-page__empty">{"\uC5C6\uC74C"}</span>}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        </SurfaceCard>
      </div>

      <div className="admin-stats-page__export">
        <Button size="sm" onClick={() => void handleExport()} disabled={isExporting || rows.length === 0}>
          {isExporting ? "\uC800\uC7A5 \uC911." : "\uC774\uBBF8\uC9C0 \uC800\uC7A5"}
        </Button>
      </div>
    </main>
  );
}