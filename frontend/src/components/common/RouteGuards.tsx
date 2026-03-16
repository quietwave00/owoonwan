import { ReactNode } from "react";
import { Navigate, useLocation } from "react-router-dom";
import { useAuth } from "../../app/auth/AuthProvider";
import { Button, StateCard } from "./Ui";

type GuardProps = {
  children: ReactNode;
};

export function ProtectedRoute({ children }: GuardProps) {
  const location = useLocation();
  const { isAuthenticated } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  return <>{children}</>;
}

export function AdminRoute({ children }: GuardProps) {
  const { isAuthenticated, me } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (me && me.role !== "ADMIN") {
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
