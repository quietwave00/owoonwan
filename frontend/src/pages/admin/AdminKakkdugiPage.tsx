import { useMemo } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { assignKakkdugi, getAdminNicknames, getAdminUsers, revokeKakkdugi } from "../../api/admin";
import { queryKeys } from "../../api/queryKeys";
import { ApiError } from "../../types/api";
import type { AdminUser } from "../../types/admin";
import { Badge, Button, PageIntro, StateCard } from "../../components/common/Ui";
import "./AdminKakkdugiPage.css";

type KakkdugiMember = {
  uid: string;
  displayName: string;
  loginId: string;
  role: "ADMIN" | "REGULAR";
  status: "ACTIVE" | "DELETED";
  kakkdugi: boolean;
};

function mergeMembers(users: AdminUser[], nicknameMap: Map<string, string>): KakkdugiMember[] {
  return users
    .filter((user) => user.status === "ACTIVE")
    .map((user) => ({
      uid: user.uid,
      displayName: user.nicknameId ? nicknameMap.get(user.nicknameId) ?? user.loginId : user.loginId,
      loginId: user.loginId,
      role: user.role,
      status: user.status,
      kakkdugi: user.kakkdugi,
    }))
    .sort((left, right) => left.displayName.localeCompare(right.displayName, "ko"));
}

export function AdminKakkdugiPage() {
  const queryClient = useQueryClient();

  const usersQuery = useQuery({
    queryKey: queryKeys.admin.users(),
    queryFn: getAdminUsers,
  });

  const nicknamesQuery = useQuery({
    queryKey: queryKeys.admin.nicknames(),
    queryFn: getAdminNicknames,
  });

  const nicknameMap = useMemo(
    () => new Map((nicknamesQuery.data ?? []).map((nickname) => [nickname.nicknameId, nickname.display])),
    [nicknamesQuery.data],
  );

  const members = useMemo(
    () => mergeMembers(usersQuery.data ?? [], nicknameMap),
    [nicknameMap, usersQuery.data],
  );

  const toggleMutation = useMutation({
    mutationFn: async ({ uid, nextChecked }: { uid: string; nextChecked: boolean }) => (
      nextChecked ? assignKakkdugi(uid) : revokeKakkdugi(uid)
    ),
    onMutate: async ({ uid, nextChecked }) => {
      await queryClient.cancelQueries({ queryKey: queryKeys.admin.users() });
      const previousUsers = queryClient.getQueryData<AdminUser[]>(queryKeys.admin.users());

      queryClient.setQueryData<AdminUser[]>(queryKeys.admin.users(), (current = []) =>
        current.map((user) => (user.uid === uid ? { ...user, kakkdugi: nextChecked } : user)),
      );

      return { previousUsers };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousUsers) {
        queryClient.setQueryData(queryKeys.admin.users(), context.previousUsers);
      }
    },
    onSettled: async () => {
      await queryClient.invalidateQueries({ queryKey: queryKeys.admin.users() });
    },
  });

  if (usersQuery.isLoading || nicknamesQuery.isLoading) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="loading"
          title="멤버 목록을 불러오는 중입니다"
          description="사용자와 닉네임 정보를 합치는 중입니다."
        />
      </main>
    );
  }

  if (usersQuery.isError || nicknamesQuery.isError) {
    const message = usersQuery.error instanceof ApiError
      ? usersQuery.error.message
      : nicknamesQuery.error instanceof ApiError
        ? nicknamesQuery.error.message
        : "깍두기 지정 화면을 불러오지 못했습니다.";

    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="error"
          title="깍두기 지정 화면을 열 수 없습니다"
          description={message}
          action={<Button size="sm" onClick={() => { void usersQuery.refetch(); void nicknamesQuery.refetch(); }}>다시 시도</Button>}
        />
      </main>
    );
  }

  return (
    <main className="page-shell admin-kakkdugi-page">
      <section className="surface-card">
        <PageIntro
          eyebrow="Admin"
          title="깍두기 지정"
          actions={""}
        />

        <div className="ui-stack admin-kakkdugi-page__list" style={{ marginTop: 20 }}>
          {members.map((member) => {
            const isPending = toggleMutation.isPending && toggleMutation.variables?.uid === member.uid;

            return (
              <label key={member.uid} className="ui-list-item admin-kakkdugi-page__item" style={{ cursor: isPending ? "progress" : "pointer" }}>
                <div className="admin-kakkdugi-page__copy">
                  <strong>{member.displayName}</strong>
                  <p className="admin-kakkdugi-page__meta">
                    {member.loginId} · {member.role === "ADMIN" ? "관리자" : "일반 멤버"}
                  </p>
                </div>
                <div className="admin-kakkdugi-page__control">
                  <Badge variant={member.kakkdugi ? "primary" : "muted"}>
                    {member.kakkdugi ? "깍두기" : "일반"}
                  </Badge>
                  <span className="admin-kakkdugi-page__switch-wrap">
                    <input
                      className="admin-kakkdugi-page__checkbox"
                      type="checkbox"
                      checked={member.kakkdugi}
                      disabled={isPending}
                      onChange={(event) => {
                        toggleMutation.mutate({ uid: member.uid, nextChecked: event.target.checked });
                      }}
                    />
                    <span className="admin-kakkdugi-page__switch" aria-hidden="true" />
                  </span>
                </div>
              </label>
            );
          })}
        </div>
      </section>
    </main>
  );
}
