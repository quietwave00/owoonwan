import { FormEvent, useMemo, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { getActiveNicknames } from "../../api/nicknames";
import { queryKeys } from "../../api/queryKeys";
import { createUser, selectNickname } from "../../api/users";
import { ApiError } from "../../types/api";
import { Badge, Button, Field, PageIntro, StateCard } from "../../components/common/Ui";

type LocationState = {
  loginId?: string;
  from?: string;
};

export function NicknameSelectPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const queryClient = useQueryClient();
  const state = (location.state as LocationState | null) ?? null;
  const loginId = state?.loginId?.trim() ?? "";
  const nextPath = state?.from ?? "/checkin";
  const [selectedNicknameId, setSelectedNicknameId] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const nicknamesQuery = useQuery({
    queryKey: queryKeys.nickname.active(),
    queryFn: getActiveNicknames,
  });

  const availableNicknames = useMemo(
    () => (nicknamesQuery.data ?? []).filter((nickname) => nickname.isActive && !nickname.assignedTo),
    [nicknamesQuery.data],
  );

  const submitMutation = useMutation({
    mutationFn: async () => {
      const user = await createUser({ loginId });
      await selectNickname(user.uid, selectedNicknameId);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.nickname.active() });
      navigate("/login", {
        replace: true,
        state: {
          from: nextPath,
          loginId,
          nicknameSelected: true,
        },
      });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        return;
      }

      setErrorMessage("닉네임 선택 중 오류가 발생했습니다.");
    },
  });

  if (!loginId) {
    return <Navigate to="/login" replace />;
  }

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!selectedNicknameId) {
      setErrorMessage("사용할 닉네임을 선택해주세요.");
      return;
    }

    setErrorMessage(null);
    submitMutation.mutate();
  };

  if (nicknamesQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Nickname"
          tone="loading"
          title="닉네임 목록을 불러오는 중입니다"
          description="관리자가 등록한 사용 가능한 닉네임을 확인하고 있습니다."
        />
      </main>
    );
  }

  if (nicknamesQuery.isError) {
    const message = nicknamesQuery.error instanceof ApiError ? nicknamesQuery.error.message : "닉네임 목록을 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Nickname"
          tone="error"
          title="닉네임 선택 화면을 열 수 없습니다"
          description={message}
          action={<Button size="sm" onClick={() => void nicknamesQuery.refetch()}>다시 시도</Button>}
        />
      </main>
    );
  }

  return (
    <main className="page-shell">
      <section className="surface-card surface-card--narrow">
        <PageIntro
          eyebrow="Nickname"
          title="닉네임 선택"
          description="처음 로그인하는 계정입니다. 닉네임은 한 번만 선택할 수 있고 이후 변경할 수 없습니다."
          actions={<Badge variant="accent">{loginId}</Badge>}
        />

        <form className="ui-stack" style={{ marginTop: 20 }} onSubmit={handleSubmit}>
          <Field error={errorMessage} hint="이미 사용 중인 닉네임은 보이지 않습니다.">
            <div className="ui-stack">
              {availableNicknames.map((nickname) => (
                <label key={nickname.nicknameId} className={`ui-list-item${selectedNicknameId === nickname.nicknameId ? " ui-list-item--active" : ""}`}>
                  <div>
                    <strong>{nickname.display}</strong>
                  </div>
                  <input
                    type="radio"
                    name="nicknameId"
                    value={nickname.nicknameId}
                    checked={selectedNicknameId === nickname.nicknameId}
                    onChange={(event) => setSelectedNicknameId(event.target.value)}
                    disabled={submitMutation.isPending}
                  />
                </label>
              ))}
            </div>
          </Field>

          {availableNicknames.length === 0 ? (
            <StateCard
              eyebrow="Nickname"
              tone="warning"
              title="선택 가능한 닉네임이 없습니다"
              description="관리자가 닉네임을 추가한 뒤 다시 시도해주세요."
            />
          ) : null}

          <div className="page-intro__actions">
            <Button type="button" variant="ghost" onClick={() => navigate("/login", { replace: true, state: { from: nextPath, loginId } })}>
              돌아가기
            </Button>
            <Button type="submit" disabled={submitMutation.isPending || availableNicknames.length === 0}>
              {submitMutation.isPending ? "저장 중..." : "닉네임 선택"}
            </Button>
          </div>
        </form>
      </section>
    </main>
  );
}
