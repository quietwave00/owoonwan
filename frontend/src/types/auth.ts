export type AuthLoginRequest = {
  loginId: string;
};

export type AuthLoginResponse = {
  sessionToken: string;
  expiresAt: string;
  uid: string;
  loginId: string;
  nicknameId: string;
};

export type AuthMeResponse = {
  uid: string;
  loginId: string;
  nicknameId: string;
  role: "ADMIN" | "REGULAR";
  expiresAt: string;
};
