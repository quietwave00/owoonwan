import { createContext, PropsWithChildren, useContext, useEffect, useMemo, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { getMe, logout as logoutApi } from "../../api/auth";
import { queryKeys } from "../../api/queryKeys";
import type { AuthLoginResponse, AuthMeResponse } from "../../types/auth";
import {
  clearSessionToken,
  getSessionToken,
  setSessionToken,
  subscribeUnauthorized,
} from "./sessionStorage";

type AuthContextValue = {
  sessionToken: string | null;
  me: AuthMeResponse | null;
  isAuthenticated: boolean;
  isRestoring: boolean;
  completeLogin: (response: AuthLoginResponse) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: PropsWithChildren) {
  const queryClient = useQueryClient();
  const [sessionToken, setSessionTokenState] = useState<string | null>(() => getSessionToken());

  const meQuery = useQuery({
    queryKey: queryKeys.auth.me(sessionToken),
    queryFn: getMe,
    enabled: Boolean(sessionToken),
    retry: false,
  });

  useEffect(() => {
    return subscribeUnauthorized(() => {
      clearSessionToken();
      setSessionTokenState(null);
      queryClient.removeQueries({ queryKey: queryKeys.auth.all });
    });
  }, [queryClient]);

  const completeLogin = (response: AuthLoginResponse) => {
    setSessionToken(response.sessionToken);
    setSessionTokenState(response.sessionToken);
    queryClient.invalidateQueries({ queryKey: queryKeys.auth.all });
  };

  const logout = async () => {
    try {
      if (sessionToken) {
        await logoutApi();
      }
    } finally {
      clearSessionToken();
      setSessionTokenState(null);
      queryClient.removeQueries({ queryKey: queryKeys.auth.all });
    }
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      sessionToken,
      me: meQuery.data ?? null,
      isAuthenticated: Boolean(sessionToken && meQuery.data),
      isRestoring: Boolean(sessionToken) && meQuery.isLoading,
      completeLogin,
      logout,
    }),
    [meQuery.data, meQuery.isLoading, sessionToken],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used within AuthProvider");
  }

  return context;
}
