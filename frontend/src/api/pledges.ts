import { adminFetch, apiFetch } from "./client";
import type { PledgeResponse, PledgeUpdateRequest } from "../types/pledge";

export function getMyPledge() {
  return apiFetch<PledgeResponse>("/pledges/me");
}

export function updateMyPledge(request: PledgeUpdateRequest) {
  return apiFetch<PledgeResponse>("/pledges/me", {
    method: "PUT",
    body: JSON.stringify(request),
  });
}

export function getPledges() {
  return apiFetch<PledgeResponse[]>("/pledges");
}

export function deleteAdminPledge(uid: string) {
  return adminFetch<void>(`/admin/pledges/${uid}`, { method: "DELETE" });
}
