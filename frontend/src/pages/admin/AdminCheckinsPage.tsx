import { useEffect, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getAdminNicknames, getAdminUsers } from "../../api/admin";
import { bulkAdminCheckin, getAdminCheckinsByDate } from "../../api/checkins";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import type { AdminUser } from "../../types/admin";
import { Badge, Button, PageIntro, StateCard } from "../../components/common/Ui";
import "./AdminCheckinsPage.css";

type MemberRow = {
  uid: string;
  displayName: string;
  loginId: string;
  role: "ADMIN" | "REGULAR";
  checked: boolean;
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

function startOfWeek(date: string) {
  const weekday = new Date(`${date}T00:00:00+09:00`).getDay();
  const diff = weekday === 0 ? -6 : 1 - weekday;
  return shiftDate(date, diff);
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

function buildWeekDates(selectedDate: string) {
  const weekStart = startOfWeek(selectedDate);

  return Array.from({ length: 7 }, (_, index) => {
    const date = shiftDate(weekStart, index);
    return {
      date,
      dayLabel: WEEKDAY_LABELS[index],
      dateLabel: formatDateLabel(date),
      isSelected: date === selectedDate,
    };
  });
}

function buildMembers(
  users: AdminUser[],
  nicknameMap: Map<string, string>,
  checkedMap: Map<string, boolean>,
): MemberRow[] {
  return users
    .filter((user) => user.status === "ACTIVE")
    .map((user) => ({
      uid: user.uid,
      displayName: user.nicknameId ? nicknameMap.get(user.nicknameId) ?? user.loginId : user.loginId,
      loginId: user.loginId,
      role: user.role,
      checked: checkedMap.get(user.uid) ?? false,
    }))
    .sort((left, right) => left.displayName.localeCompare(right.displayName, "ko"));
}

export function AdminCheckinsPage() {
  const queryClient = useQueryClient();
  const today = useMemo(() => getKstToday(), []);
  const [selectedDate, setSelectedDate] = useState(today);
  const [selectedUserIds, setSelectedUserIds] = useState<string[]>([]);

  const usersQuery = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: getAdminUsers,
  });

  const nicknamesQuery = useQuery({
    queryKey: queryKeys.admin.nicknames(),
    queryFn: getAdminNicknames,
  });

  const checkinsQuery = useQuery({
    queryKey: queryKeys.admin.checkins(selectedDate),
    queryFn: () => getAdminCheckinsByDate(selectedDate),
  });

  const nicknameMap = useMemo(
    () => new Map((nicknamesQuery.data ?? []).map((nickname) => [nickname.nicknameId, nickname.display])),
    [nicknamesQuery.data],
  );

  const checkedMap = useMemo(
    () => new Map((checkinsQuery.data?.users ?? []).map((user) => [user.uid, user.checked])),
    [checkinsQuery.data],
  );

  const members = useMemo(
    () => buildMembers(usersQuery.data ?? [], nicknameMap, checkedMap),
    [checkedMap, nicknameMap, usersQuery.data],
  );

  useEffect(() => {
    setSelectedUserIds(
      (checkinsQuery.data?.users ?? [])
        .filter((user) => user.checked)
        .map((user) => user.uid),
    );
  }, [checkinsQuery.data]);

  const saveMutation = useMutation({
    mutationFn: async () => bulkAdminCheckin({ date: selectedDate, userIds: selectedUserIds }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.checkins(selectedDate) });
    },
  });

  const weekDates = useMemo(() => buildWeekDates(selectedDate), [selectedDate]);
  const weekRangeLabel = useMemo(
    () => formatRangeLabel(weekDates[0]?.date ?? selectedDate, weekDates[6]?.date ?? selectedDate),
    [selectedDate, weekDates],
  );

  const toggleUser = (uid: string) => {
    setSelectedUserIds((current) => (
      current.includes(uid)
        ? current.filter((currentUid) => currentUid !== uid)
        : [...current, uid]
    ));
  };

  if (usersQuery.isLoading || nicknamesQuery.isLoading || checkinsQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="loading"
          title="체크인 조정 정보를 불러오는 중입니다"
          description="사용자 목록과 선택 날짜의 체크인 상태를 가져오고 있습니다."
        />
      </main>
    );
  }

  if (usersQuery.isError || nicknamesQuery.isError || checkinsQuery.isError) {
    const message = usersQuery.error instanceof ApiError
      ? usersQuery.error.message
      : nicknamesQuery.error instanceof ApiError
        ? nicknamesQuery.error.message
        : checkinsQuery.error instanceof ApiError
          ? checkinsQuery.error.message
          : "체크인 조정 화면을 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="error"
          title="체크인 조정 화면을 열 수 없습니다"
          description={message}
          action={<Button size="sm" onClick={() => { void usersQuery.refetch(); void nicknamesQuery.refetch(); void checkinsQuery.refetch(); }}>다시 시도</Button>}
        />
      </main>
    );
  }

  return (
    <main className="page-shell admin-checkins-page">
      <section className="surface-card">
        <PageIntro
          eyebrow="Admin"
          title="체크인 조정"
          actions={
            <>
              <Badge variant="accent">{selectedDate}</Badge>
              <Badge variant="muted">선택 {selectedUserIds.length}명</Badge>
            </>
          }
        />

        <div className="weekly-board-nav admin-checkins-page__nav">
          <button
            type="button"
            className="weekly-board-nav__arrow"
            aria-label="이전 주"
            onClick={() => setSelectedDate(shiftDate(selectedDate, -7))}
          >
            ‹
          </button>
          <div className="weekly-board-nav__summary">
            <Badge variant="muted">{weekRangeLabel}</Badge>
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

        <div className="admin-checkins-page__week">
          {weekDates.map((day) => (
            <button
              key={day.date}
              type="button"
              className={`admin-checkins-page__day${day.isSelected ? " admin-checkins-page__day--active" : ""}`}
              onClick={() => setSelectedDate(day.date)}
            >
              <span>{day.dayLabel}</span>
              <strong>{day.dateLabel}</strong>
            </button>
          ))}
        </div>

        <div className="admin-checkins-page__list">
          {members.map((member) => {
            const checked = selectedUserIds.includes(member.uid);

            return (
              <label key={member.uid} className={`ui-list-item admin-checkins-page__item${checked ? " admin-checkins-page__item--checked" : ""}`}>
                <div className="admin-checkins-page__copy">
                  <strong>{member.displayName}</strong>
                  <p className="admin-checkins-page__meta">
                    {member.loginId} · {member.role === "ADMIN" ? "관리자" : "일반 멤버"}
                  </p>
                </div>
                <input
                  className="admin-checkins-page__checkbox"
                  type="checkbox"
                  checked={checked}
                  onChange={() => toggleUser(member.uid)}
                />
              </label>
            );
          })}
        </div>

        <div className="admin-checkins-page__footer">
          <Button type="button" disabled={saveMutation.isPending || selectedUserIds.length === 0} onClick={() => saveMutation.mutate()}>
            {saveMutation.isPending ? "..." : "저장"}
          </Button>
        </div>
      </section>
    </main>
  );
}

