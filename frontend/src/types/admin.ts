export type AdminUser = {
  uid: string;
  loginId: string;
  nicknameId: string | null;
  kakkdugi: boolean;
  role: "ADMIN" | "REGULAR";
  status: "ACTIVE" | "DELETED";
  createdAt: string;
  deletedAt: string | null;
};

export type NicknameResponse = {
  nicknameId: string;
  display: string;
  isActive: boolean;
  assignedTo: string | null;
};

export type AdminSpecialTitleResponse = {
  uid: string;
  kakkdugi: boolean;
};
