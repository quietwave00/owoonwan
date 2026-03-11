export type UserCreateRequest = {
  loginId: string;
};

export type UserSelectNicknameRequest = {
  nicknameId: string;
};

export type UserResponse = {
  uid: string;
  loginId: string;
  nicknameId: string | null;
  role: "ADMIN" | "REGULAR";
  status: "ACTIVE" | "DELETED";
  createdAt: string;
  deletedAt: string | null;
};
