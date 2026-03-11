import { FormEvent, useState } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { createAdminNickname, getAdminNicknames } from "../../api/admin";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import { Badge, Button, Field, PageIntro, StateCard, TextInput } from "../../components/common/Ui";
import "./AdminNicknamesPage.css";

export function AdminNicknamesPage() {
  const queryClient = useQueryClient();
  const [display, setDisplay] = useState("");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const nicknamesQuery = useQuery({
    queryKey: queryKeys.admin.nicknames(),
    queryFn: getAdminNicknames,
  });

  const createMutation = useMutation({
    mutationFn: async () => createAdminNickname(display.trim()),
    onSuccess: async () => {
      setDisplay("");
      setErrorMessage(null);
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.nicknames() });
      await queryClient.invalidateQueries({ queryKey: queryKeys.nickname.active() });
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        setErrorMessage(error.message);
        return;
      }

      setErrorMessage("닉네임 생성 중 오류가 발생했습니다.");
    },
  });

  const nicknames = nicknamesQuery.data ?? [];

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!display.trim()) {
      setErrorMessage("생성할 닉네임을 입력해주세요.");
      return;
    }

    setErrorMessage(null);
    createMutation.mutate();
  };

  if (nicknamesQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="loading"
          title="닉네임 목록을 불러오는 중입니다"
          description="관리자가 생성한 닉네임을 조회하고 있습니다."
        />
      </main>
    );
  }

  if (nicknamesQuery.isError) {
    const message = nicknamesQuery.error instanceof ApiError ? nicknamesQuery.error.message : "닉네임 목록을 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="error"
          title="닉네임 관리 화면을 열 수 없습니다"
          description={message}
          action={<Button size="sm" onClick={() => void nicknamesQuery.refetch()}>다시 시도</Button>}
        />
      </main>
    );
  }

  return (
    <main className="page-shell admin-nicknames-page">
      <section className="surface-card admin-nicknames-page__surface">
        <PageIntro
          eyebrow="Admin"
          title="닉네임 관리"
          actions={<Badge variant="accent">총 {nicknames.length}개</Badge>}
        />

        <div className="admin-nicknames-page__grid">
          <form className="ui-card ui-stack admin-nicknames-page__form" onSubmit={handleSubmit}>
            <Field
              label=""
              error={errorMessage}
              hint=""
            >
              <TextInput
                value={display}
                onChange={(event) => setDisplay(event.target.value)}
                placeholder="닉네임 입력"
                disabled={createMutation.isPending}
              />
            </Field>
            <Button type="submit" disabled={createMutation.isPending}>
              {createMutation.isPending ? "생성 중..." : "추가"}
            </Button>
          </form>

          <div className="admin-nicknames-page__list">
            {nicknames.map((nickname) => (
              <div key={nickname.nicknameId} className="ui-list-item admin-nicknames-page__item">
                <div className="admin-nicknames-page__item-copy">
                  <strong>{nickname.display}</strong>
                  <p className="admin-nicknames-page__meta">
                    {nickname.assignedTo ? `배정됨: ${nickname.assignedTo}` : "미배정"}
                  </p>
                </div>
                <Badge variant={nickname.assignedTo ? "primary" : nickname.isActive ? "accent" : "muted"}>
                  {nickname.assignedTo ? "사용중" : nickname.isActive ? "선택 가능" : "비활성"}
                </Badge>
              </div>
            ))}
          </div>
        </div>
      </section>
    </main>
  );
}
