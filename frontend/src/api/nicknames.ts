import { apiFetch } from "./client";
import type { NicknameResponse } from "../types/admin";

export function getActiveNicknames() {
  return apiFetch<NicknameResponse[]>("/nicknames");
}
