import { BrowserRouter, Navigate, Outlet, Route, Routes } from "react-router-dom";
import { AppShell } from "../../components/common/AppShell";
import { AdminRoute, ProtectedRoute } from "../../components/common/RouteGuards";
import { AdminCheckinsPage } from "../../pages/admin/AdminCheckinsPage";
import { AdminKakkdugiPage } from "../../pages/admin/AdminKakkdugiPage";
import { AdminNicknamesPage } from "../../pages/admin/AdminNicknamesPage";
import { AdminStatsPage } from "../../pages/admin/AdminStatsPage";
import { CheckinPage } from "../../pages/checkin/CheckinPage";
import { LoginPage } from "../../pages/login/LoginPage";
import { NicknameSelectPage } from "../../pages/nickname-select/NicknameSelectPage";
import { PledgesPage } from "../../pages/pledges/PledgesPage";
import { WeeklyBoardPage } from "../../pages/weekly-board/WeeklyBoardPage";

export function AppRouter() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Navigate to="/login" replace />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/nickname-select" element={<NicknameSelectPage />} />
        <Route
          element={
            <ProtectedRoute>
              <AppShell />
            </ProtectedRoute>
          }
        >
          <Route path="/checkin" element={<CheckinPage />} />
          <Route path="/board/weekly" element={<WeeklyBoardPage />} />
          <Route path="/pledges" element={<PledgesPage />} />
          <Route
            element={
              <AdminRoute>
                <Outlet />
              </AdminRoute>
            }
          >
            <Route path="/admin/nicknames" element={<AdminNicknamesPage />} />
            <Route path="/admin/kakkdugi" element={<AdminKakkdugiPage />} />
            <Route path="/admin/checkins" element={<AdminCheckinsPage />} />
            <Route path="/admin/stats" element={<AdminStatsPage />} />
          </Route>
        </Route>
      </Routes>
    </BrowserRouter>
  );
}