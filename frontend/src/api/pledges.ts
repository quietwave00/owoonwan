import { adminFetch, apiFetch } from "./client";
import type { PledgeResponse, PledgeUpdateRequest } from "../types/pledge";

export function getMyPledge() {
  return apiFetch<PledgeResponse>("/api/pledges/me");
}

export function updateMyPledge(request: PledgeUpdateRequest) {
  return apiFetch<PledgeResponse>("/api/pledges/me", {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function getPledges() {
  return apiFetch<PledgeResponse[]>("/api/pledges");
}

export function deleteAdminPledge(uid: string) {
  return adminFetch<void>(`/api/admin/pledges/${uid}`, { method: "DELETE" });
}
