import { useEffect, useMemo, useRef, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { getMyPledge, getPledges, updateMyPledge } from "../../api/pledges";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import type { PledgeResponse } from "../../types/pledge";
import { Badge, Button, PageIntro, StateCard, SurfaceCard, Textarea, ToastStack } from "../../components/common/Ui";

type ToastTone = "success" | "error" | "info";

type ToastItem = {
  id: string;
  title: string;
  description: string;
  tone: ToastTone;
};

function formatUpdatedAt(value: string | null) {
  if (!value) {
    return "";
  }

  return new Intl.DateTimeFormat("ko-KR", {
    timeZone: "Asia/Seoul",
    month: "numeric",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  }).format(new Date(value));
}

function sortPledges(items: PledgeResponse[]) {
  return [...items].sort((left, right) => {
    if (left.mine !== right.mine) {
      return left.mine ? -1 : 1;
    }

    if (Boolean(left.text) !== Boolean(right.text)) {
      return left.text ? -1 : 1;
    }

    return left.nickname.localeCompare(right.nickname, "ko");
  });
}

export function PledgesPage() {
  const queryClient = useQueryClient();
  const [draft, setDraft] = useState("");
  const [toasts, setToasts] = useState<ToastItem[]>([]);
  const toastTimersRef = useRef<Record<string, number>>({});

  const myPledgeQuery = useQuery({
    queryKey: queryKeys.pledge.mine(),
    queryFn: getMyPledge,
  });

  const pledgesQuery = useQuery({
    queryKey: queryKeys.pledge.list(),
    queryFn: getPledges,
  });

  useEffect(() => {
    if (myPledgeQuery.data) {
      setDraft(myPledgeQuery.data.text);
    }
  }, [myPledgeQuery.data]);

  useEffect(() => {
    return () => {
      Object.values(toastTimersRef.current).forEach((timer) => window.clearTimeout(timer));
      toastTimersRef.current = {};
    };
  }, []);

  const removeToast = (id: string) => {
    const timer = toastTimersRef.current[id];
    if (timer) {
      window.clearTimeout(timer);
      delete toastTimersRef.current[id];
    }

    setToasts((current) => current.filter((toast) => toast.id !== id));
  };

  const pushToast = (title: string, description: string, tone: ToastTone) => {
    const id = `${Date.now()}-${Math.random()}`;

    setToasts((current) => [{ id, title, description, tone }, ...current].slice(0, 3));

    const timer = window.setTimeout(() => {
      removeToast(id);
    }, 3200);

    toastTimersRef.current[id] = timer;
  };

  const saveMutation = useMutation({
    mutationFn: () => updateMyPledge({ text: draft.trim() }),
    onSuccess: async () => {
      pushToast("저장 완료", "내 다짐을 저장했습니다.", "success");
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: queryKeys.pledge.mine() }),
        queryClient.invalidateQueries({ queryKey: queryKeys.pledge.list() }),
      ]);
    },
    onError: (error) => {
      const message = error instanceof ApiError ? error.message : "다짐 저장 중 오류가 발생했습니다.";
      pushToast("저장 실패", message, "error");
    },
  });

  const isLoading = myPledgeQuery.isLoading || pledgesQuery.isLoading;
  const isError = myPledgeQuery.isError || pledgesQuery.isError;

  const errorMessage = useMemo(() => {
    if (myPledgeQuery.error instanceof ApiError) {
      return myPledgeQuery.error.message;
    }

    if (pledgesQuery.error instanceof ApiError) {
      return pledgesQuery.error.message;
    }

    return "다짐 데이터를 불러오지 못했습니다.";
  }, [myPledgeQuery.error, pledgesQuery.error]);

  const sortedPledges = useMemo(() => sortPledges(pledgesQuery.data ?? []), [pledgesQuery.data]);
  const myPledge = myPledgeQuery.data;
  const isDraftChanged = draft !== (myPledge?.text ?? "");

  if (isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Pledges"
          tone="loading"
          title="다짐을 불러오는 중입니다"
          description="내 다짐과 전체 멤버 목록을 함께 준비하고 있습니다."
        />
      </main>
    );
  }

  if (isError || !myPledge || !pledgesQuery.data) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Pledges"
          tone="error"
          title="다짐 화면을 열 수 없습니다"
          description={errorMessage}
          action={
            <Button
              size="sm"
              onClick={() => {
                void myPledgeQuery.refetch();
                void pledgesQuery.refetch();
              }}
            >
              다시 시도
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
          eyebrow="Pledges"
          title="다짐"
          actions={""}
        />

        <section className="pledge-composer">
          <Textarea
            value={draft}
            onChange={(event) => setDraft(event.target.value)}
            placeholder="다짐을 입력해 주세요 ..."
          />
          <div className="pledge-composer__footer">
            <Button
              className="pledge-save-button"
              type="button"
              disabled={saveMutation.isPending || !isDraftChanged}
              onClick={() => saveMutation.mutate()}
            >
              {saveMutation.isPending ? "..." : "저장"}
            </Button>
          </div>
        </section>

        <section className="pledge-stream" aria-label="멤버 다짐 목록">
          {sortedPledges.map((pledge) => (
            <article key={pledge.uid} className={`pledge-stream__item${pledge.mine ? " pledge-stream__item--mine" : ""}`}>
              <div className="pledge-stream__head">
                <div className="pledge-stream__identity">
                  <strong className="pledge-stream__name">{pledge.nickname}</strong>
                  {pledge.mine ? <Badge variant="muted">내 다짐</Badge> : null}
                </div>
                <span className="pledge-stream__meta">{formatUpdatedAt(pledge.updatedAt)}</span>
              </div>
              <p className={`pledge-stream__text${pledge.text ? "" : " pledge-stream__text--empty"}`}>
                {pledge.text || "아직 작성한 다짐이 없습니다."}
              </p>
            </article>
          ))}
        </section>

        {toasts.length > 0 ? <ToastStack items={toasts} onClose={removeToast} /> : null}
      </SurfaceCard>
    </main>
  );
}