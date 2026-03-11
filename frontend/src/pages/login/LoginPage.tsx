import { FormEvent, useState } from "react";
import { Navigate, useLocation, useNavigate } from "react-router-dom";
import { login } from "../../api/auth";
import { useAuth } from "../../app/auth/AuthProvider";
import { ApiError } from "../../types/api";
import { Button, Field, InlineNotice, TextInput } from "../../components/common/Ui";

type LoginLocationState = {
  from?: string;
  loginId?: string;
  nicknameSelected?: boolean;
};

export function LoginPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const { completeLogin, isAuthenticated, isRestoring } = useAuth();
  const state = (location.state as LoginLocationState | null) ?? null;
  const nextPath = state?.from ?? "/checkin";
  const [loginId, setLoginId] = useState(state?.loginId ?? "");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (isAuthenticated) {
    return <Navigate to={nextPath} replace />;
  }

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    const trimmedLoginId = loginId.trim();
    if (!trimmedLoginId) {
      setErrorMessage("아이디를 입력해주세요.");
      return;
    }

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      const response = await login({ loginId: trimmedLoginId });
      completeLogin(response);
      navigate("/checkin", { replace: true });
    } catch (error) {
      if (error instanceof ApiError && error.code === "USER_NOT_FOUND") {
        navigate("/nickname-select", {
          replace: true,
          state: {
            loginId: trimmedLoginId,
            from: nextPath,
          },
        });
        return;
      }

      if (error instanceof ApiError) {
        setErrorMessage(error.message);
      } else {
        setErrorMessage("로그인 중 알 수 없는 오류가 발생했습니다.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <main className="login-page bg-grid">
      <form className="login-form" onSubmit={handleSubmit}>
        {state?.nicknameSelected ? (
          <span>닉네임 선택 완료.. 재로그인 ㄱㄱ</span>
        ) : null}

        <Field hint={isRestoring ? "세션 복원 중입니다." : undefined} error={errorMessage}>
          <TextInput
            value={loginId}
            onChange={(event) => setLoginId(event.target.value)}
            placeholder="loginId"
            autoComplete="username"
            disabled={isSubmitting || isRestoring}
          />
        </Field>

        <Button type="submit" block disabled={isSubmitting || isRestoring}>
          {isSubmitting ? "로그인 중..." : "로그인"}
        </Button>
      </form>
    </main>
  );
}
