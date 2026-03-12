export type AuthLoginRequest = {
  loginId: string;
};

export type AuthLoginResponse = {
  sessionToken: string;
  expiresAt: string;
  uid: string;
  loginId: string;
  nicknameId: string;
  nicknameDisplay?: string | null;
};

export type AuthMeResponse = {
  uid: string;
  loginId: string;
  nicknameId: string;
  nicknameDisplay?: string | null;
  role: "ADMIN" | "REGULAR";
  expiresAt: string;
};