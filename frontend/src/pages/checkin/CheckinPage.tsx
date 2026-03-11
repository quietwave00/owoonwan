import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { cancelDates, checkinDates, getMyWeek } from "../../api/checkins";
import { queryKeys } from "../../api/queryKeys";
import { getMyCurrentWeekTitle } from "../../api/titles";
import { ApiError } from "../../types/api";
import type { CheckinPeriodResponse } from "../../types/checkin";
import { Badge, Button, PageIntro, StateCard, ToastStack } from "../../components/common/Ui";

type ToastTone = "success" | "error" | "info";

type ToastItem = {
  id: string;
  title: string;
  description: string;
  tone: ToastTone;
};

const WEEKDAY_LABELS = ["월", "화", "수", "목", "금", "토", "일"];

function getKstToday() {
  const formatter = new Intl.DateTimeFormat("en", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  });

  const parts = formatter.formatToParts(new Date());
  const year = parts.find((part) => part.type === "year")?.value ?? "0000";
  const month = parts.find((part) => part.type === "month")?.value ?? "01";
  const day = parts.find((part) => part.type === "day")?.value ?? "01";

  return `${year}-${month}-${day}`;
}

function formatDisplayDate(date: string) {
  const day = date.split("-")[2] ?? date;
  return `${Number(day)}일`;
}

function formatWeekLabel(startDate: string) {
  const [, month, day] = startDate.split("-").map(Number);
  const weekOfMonth = Math.floor((day - 1) / 7) + 1;
  return `${month}월 ${weekOfMonth}주차`;
}

function isPastOrToday(date: string, today: string) {
  return date <= today;
}

function updateDayInWeek(period: CheckinPeriodResponse | undefined, targetDate: string, nextStatus: "PRESENT" | "ABSENT") {
  if (!period) {
    return period;
  }

  let presentCount = period.presentCount;

  const days = period.days.map((day) => {
    if (day.date !== targetDate || !day.canCheckinAction) {
      return day;
    }

    if (day.status === nextStatus) {
      return day;
    }

    presentCount += nextStatus === "PRESENT" ? 1 : -1;

    return {
      ...day,
      status: nextStatus,
    };
  });

  return {
    ...period,
    presentCount,
    days,
  };
}

export function CheckinPage() {
  const queryClient = useQueryClient();
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const toastTimersRef = useRef<Record<string, number>>({});
  const today = useMemo(() => getKstToday(), []);
  const [selectedDate, setSelectedDate] = useState(today);

  const weekQuery = useQuery({
    queryKey: queryKeys.checkin.myWeek(today),
    queryFn: () => getMyWeek(today),
  });

  const titleQuery = useQuery({
    queryKey: queryKeys.title.myCurrentWeek(),
    queryFn: getMyCurrentWeekTitle,
  });

  const selectedDay = weekQuery.data?.days.find((day) => day.date === selectedDate) ?? null;
  const canToggleSelectedDay = Boolean(selectedDay && (selectedDay.canCheckinAction || isPastOrToday(selectedDay.date, today)));
  const isCheckedSelectedDay = selectedDay?.status === "PRESENT";
  const weekLabel = weekQuery.data ? formatWeekLabel(weekQuery.data.startDate) : "이번 주";

  const removeToast = (id: string) => {
    const timer = toastTimersRef.current[id];
    if (timer) {
      window.clearTimeout(timer);
      delete toastTimersRef.current[id];
    }

    setToasts((current) => current.filter((toast) => toast.id !== id));
  };

  useEffect(() => {
    return () => {
      Object.values(toastTimersRef.current).forEach((timer) => window.clearTimeout(timer));
      toastTimersRef.current = {};
    };
  }, []);

  const pushToast = (title: string, description: string, tone: ToastTone) => {
    const id = `${Date.now()}-${Math.random()}`;

    setToasts((current) => [{ id, title, description, tone }, ...current].slice(0, 3));

    const timer = window.setTimeout(() => {
      removeToast(id);
    }, 3000);

    toastTimersRef.current[id] = timer;
  };

  const toggleMutation = useMutation({
    mutationFn: async () => (
      isCheckedSelectedDay
        ? cancelDates({ date: selectedDate })
        : checkinDates({ date: selectedDate })
    ),
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: queryKeys.checkin.myWeek(today) });
      const previousWeek = queryClient.getQueryData<CheckinPeriodResponse>(queryKeys.checkin.myWeek(today));

      queryClient.setQueryData<CheckinPeriodResponse>(
        queryKeys.checkin.myWeek(today),
        updateDayInWeek(previousWeek, selectedDate, isCheckedSelectedDay ? "ABSENT" : "PRESENT"),
      );

      return { previousWeek };
    },
    onError: (error, _variables, context) => {
      if (context?.previousWeek) {
        queryClient.setQueryData(queryKeys.checkin.myWeek(today), context.previousWeek);
      }

      const message = error instanceof ApiError ? error.message : "체크인 처리 중 오류가 발생했습니다.";
      pushToast("체크인 실패", message, "error");
    },
    onSuccess: () => {
      pushToast(
        isCheckedSelectedDay ? "체크 완료" : "체크 취소",
        isCheckedSelectedDay ? "당신은 짱입니다" : "체크를 취소했습니다",
        "success",
      );
    },
    onSettled: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.checkin.myWeek(today) }),
        queryClient.invalidateQueries({ queryKey: queryKeys.title.myCurrentWeek() }),
      ]);
    },
  });

  if (weekQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Check-in"
          tone="loading"
          title="이번 주 출석 현황을 불러오는 중입니다"
          description="체크인 데이터를 가져오고 있습니다."
        />
      </main>
    );
  }

  if (weekQuery.isError || !weekQuery.data) {
    const message = weekQuery.error instanceof ApiError ? weekQuery.error.message : "체크인 데이터를 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Check-in"
          tone="error"
          title="체크인 화면을 열 수 없습니다"
          description={message}
          action={
            <Button size="sm" onClick={() => void weekQuery.refetch()}>
              다시 시도
            </Button>
          }
        />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <section className="surface-card">
        <PageIntro
          eyebrow="Check-in"
          title="체크인"
          actions={
            <>
              <Badge variant="accent">{weekLabel}</Badge>
            </>
          }
        />

        <div className="ui-stack" style={{ marginTop: 20 }}>
          <div style={{ display: "grid", gridTemplateColumns: "repeat(7, minmax(0, 1fr))", gap: 8 }}>
            {weekQuery.data.days.map((day, index) => {
              const isSelected = day.date === selectedDate;
              const isToday = day.date === today;
              const isPresent = day.status === "PRESENT";

              return (
                <button
                  key={day.date}
                  type="button"
                  className={`nav-pill${isSelected ? " nav-pill--active" : ""}`}
                  onClick={() => setSelectedDate(day.date)}
                  style={{
                    width: "100%",
                    minHeight: 64,
                    flexDirection: "column",
                    justifyContent: "center",
                    background: isSelected ? undefined : isPresent ? "var(--gray-pale)" : undefined,
                    borderColor: isToday ? "var(--ink)" : undefined,
                  }}
                >
                  <span>{WEEKDAY_LABELS[index]}</span>
                  <span style={{ fontSize: 12 }}>{formatDisplayDate(day.date)}</span>
                </button>
              );
            })}
          </div>


            <Button
              type="button"
              disabled={!canToggleSelectedDay || toggleMutation.isPending}
              onClick={() => toggleMutation.mutate()}
            >
              {toggleMutation.isPending ? "처리 중..." : isCheckedSelectedDay ? "체크 취소" : "체크"}
            </Button>


          {toasts.length > 0 ? <ToastStack items={toasts} onClose={removeToast} /> : null}
        </div>
      </section>
    </main>
  );
}



