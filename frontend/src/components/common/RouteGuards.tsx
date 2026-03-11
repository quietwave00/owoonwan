import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../app/auth/AuthProvider";
import { Button, StateCard } from "./Ui";

type GuardProps = {
  children: ReactNode;
};

export function ProtectedRoute({ children }: GuardProps) {
  const location = useLocation();
  const { isAuthenticated, isRestoring } = useAuth();

  if (isRestoring) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Session"
          tone="loading"
          title="세션을 복원하고 있습니다"
          description="저장된 로그인 정보를 확인한 뒤 보호 화면으로 다시 연결합니다."
        />
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}

export function AdminRoute({ children }: GuardProps) {
  const { isAuthenticated, isRestoring, me } = useAuth();

  if (isRestoring) {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="loading"
          title="관리자 권한을 확인하는 중입니다"
          description="현재 세션의 역할과 접근 범위를 다시 확인하고 있습니다."
        />
      </main>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (me?.role !== "ADMIN") {
    return (
      <main className="page-shell">
        <StateCard
          eyebrow="Admin"
          tone="warning"
          title="관리자만 접근할 수 있는 화면입니다"
          description="운영 화면은 관리자 세션에서만 열립니다."
          action={<Button size="sm" onClick={() => window.history.back()}>이전으로</Button>}
        />
      </main>
    );
  }

  return <>{children}</>;
}
