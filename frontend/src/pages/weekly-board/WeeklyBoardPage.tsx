import { useMemo, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { getWeeklyBoard } from "../../api/board";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import type { WeeklyBoardResponse } from "../../types/board";
import { Badge, Button, PageIntro, StateCard, SurfaceCard } from "../../components/common/Ui";

type WeeklyColumn = {
  date: string;
  dayLabel: string;
  dateLabel: string;
  nicknames: string[];
  count: number;
  isToday: boolean;
};

const WEEKDAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

function getKstToday() {
  const formatter = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  return formatter.format(new Date());
}

function shiftDate(date: string, amount: number) {
  const [year, month, day] = date.split("-").map(Number);
  const next = new Date(Date.UTC(year, month - 1, day + amount));

  return next.toISOString().slice(0, 10);
}

function formatDateLabel(date: string) {
  const [, month, day] = date.split("-").map(Number);
  return `${month}.${day}`;
}

function formatRangeLabel(startDate: string, endDate: string) {
  const [startYear, startMonth, startDay] = startDate.split("-").map(Number);
  const [endYear, endMonth, endDay] = endDate.split("-").map(Number);

  if (startYear === endYear) {
    if (startMonth === endMonth) {
      return `${startYear}.${startMonth}.${startDay} - ${endDay}`;
    }

    return `${startYear}.${startMonth}.${startDay} - ${endMonth}.${endDay}`;
  }

  return `${startYear}.${startMonth}.${startDay} - ${endYear}.${endMonth}.${endDay}`;
}

function buildColumns(board: WeeklyBoardResponse, today: string) {
  return board.members.reduce<WeeklyColumn[]>((columns, member) => {
    member.days.forEach((day, index) => {
      if (!columns[index]) {
        columns[index] = {
          date: day.date,
          dayLabel: WEEKDAY_LABELS[index] ?? `${index + 1}`,
          dateLabel: formatDateLabel(day.date),
          nicknames: [],
          count: 0,
          isToday: day.date === today,
        };
      }

      if (day.status === "PRESENT") {
        columns[index].nicknames.push(member.nickname);
      }
    });

    return columns;
  }, []).map((column) => ({
    ...column,
    nicknames: column.nicknames.sort((left, right) => left.localeCompare(right, "ko")),
    count: column.nicknames.length,
  }));
}

export function WeeklyBoardPage() {
  const today = useMemo(() => getKstToday(), []);
  const [selectedDate, setSelectedDate] = useState(today);

  const weeklyBoardQuery = useQuery({
    queryKey: queryKeys.board.weekly(selectedDate),
    queryFn: () => getWeeklyBoard(selectedDate),
  });

  const columns = weeklyBoardQuery.data ? buildColumns(weeklyBoardQuery.data, today) : [];
  const totalMembers = weeklyBoardQuery.data?.members.length ?? 0;

  if (weeklyBoardQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Weekly Board"
          tone="loading"
          title="주간 보드를 불러오는 중입니다"
          description="선택한 주차의 체크인 현황을 정리하고 있습니다."
        />
      </main>
    );
  }

  if (weeklyBoardQuery.isError) {
    const message =
      weeklyBoardQuery.error instanceof ApiError
        ? weeklyBoardQuery.error.message
        : "주간 보드를 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Weekly Board"
          tone="error"
          title="주간 조회 화면을 열 수 없습니다"
          description={message}
          action={
            <Button size="sm" onClick={() => void weeklyBoardQuery.refetch()}>
              다시 시도
            </Button>
          }
        />
      </main>
    );
  }

  if (!weeklyBoardQuery.data || columns.length === 0) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Weekly Board"
          tone="warning"
          title="이번 주에 표시할 데이터가 없습니다"
          description="주간 보드 응답이 비어 있습니다. 다른 주차로 이동해 확인해보세요."
          action={
            <Button size="sm" onClick={() => setSelectedDate(today)}>
              이번 주로 이동
            </Button>
          }
        />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <SurfaceCard>
        <PageIntro
          eyebrow="Weekly Board"
          title="주간 조회"
        />

        <div className="weekly-board-nav">
          <button
            type="button"
            className="weekly-board-nav__arrow"
            aria-label="이전 주"
            onClick={() => setSelectedDate(shiftDate(selectedDate, -7))}
          >
            ‹
          </button>
          <div className="weekly-board-nav__summary">
            <Badge variant="muted">
              {formatRangeLabel(weeklyBoardQuery.data.weekStartDate, weeklyBoardQuery.data.weekEndDate)}
            </Badge>
          </div>
          <button
            type="button"
            className="weekly-board-nav__arrow"
            aria-label="다음 주"
            onClick={() => setSelectedDate(shiftDate(selectedDate, 7))}
          >
            ›
          </button>
        </div>

        <section className="weekly-board-table" aria-label="요일별 체크인 닉네임 표">
          {columns.map((column) => (
            <article key={column.date} className={`weekly-board-row${column.isToday ? " weekly-board-row--today" : ""}`}>
              <div className="weekly-board-row__day">
                <span className="weekly-board-row__weekday">{column.dayLabel}</span>
                <strong className="weekly-board-row__date">{column.dateLabel}</strong>
                <span className="weekly-board-row__count">{column.count}명</span>
              </div>
              <div className="weekly-board-row__members">
                {column.nicknames.length > 0 ? column.nicknames.join(" / ") : "-"}
              </div>
            </article>
          ))}
        </section>
      </SurfaceCard>
    </main>
  );
}