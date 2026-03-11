import { useQuery } from "@tanstack/react-query";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../app/auth/AuthProvider";
import { queryKeys } from "../../api/queryKeys";
import { getMyCurrentWeekTitle } from "../../api/titles";
import "./AppShell.css";

const DEFAULT_MAIN_LINKS = [
  { to: "/checkin", label: "체크" },
  { to: "/board/weekly", label: "주간" },
  { to: "/pledges", label: "다짐" },
];

const DEFAULT_ADMIN_LINKS = [
  { to: "/admin/nicknames", label: "닉네임" },
  { to: "/admin/kakkdugi", label: "깍두기" },
  { to: "/admin/checkins", label: "관리" },
  { to: "/admin/stats", label: "통계" },
];

type AppShellProps = {
  me?: { uid?: string; loginId: string; role: "ADMIN" | "REGULAR" } | null;
  logout?: () => Promise<void> | void;
  mainLinks?: Array<{ to: string; label: string }>;
  adminLinks?: Array<{ to: string; label: string }>;
};

export function AppShell({
  me: meProp,
  logout: logoutProp,
  mainLinks = DEFAULT_MAIN_LINKS,
  adminLinks = DEFAULT_ADMIN_LINKS,
}: AppShellProps) {
  const auth = useAuth();
  const me = meProp ?? auth.me;
  const logout = logoutProp ?? auth.logout;

  const titleQuery = useQuery({
    queryKey: queryKeys.title.myCurrentWeek(),
    queryFn: getMyCurrentWeekTitle,
    enabled: Boolean(me),
  });

  const currentTitle = titleQuery.data?.effectiveBadges?.[0] ?? "타이틀 없음";
  const displayName = me?.loginId ?? "";

  return (
    <div className="app-shell bg-grid">
      <header className="topbar">
        {me ? (
          <div className="topbar__actions topbar__actions--compact">
            <div className="topbar__logout">
              <button type="button" className="btn-logout" onClick={() => void logout()}>
                로그아웃
              </button>
            </div>
            <div className="user-mini-card">
              <span className="user-mini-card__name">{displayName}</span>
              <span className="user-mini-card__title">{currentTitle}</span>
            </div>
          </div>
        ) : null}
      </header>

      <nav className="nav-card" aria-label="주 메뉴">
        <div className="nav-card__group" role="list">
          {mainLinks.map((link) => (
            <NavLink
              key={link.to}
              to={link.to}
              role="listitem"
              className={({ isActive }) => `nav-pill${isActive ? " nav-pill--active" : ""}`}
            >
              {link.label}
            </NavLink>
          ))}
        </div>

        {me?.role === "ADMIN" && adminLinks.length > 0 ? (
          <>
            <div className="nav-card__divider" aria-hidden="true" />
            <div className="nav-card__group nav-card__group--admin" role="list">
              {adminLinks.map((link) => (
                <NavLink
                  key={link.to}
                  to={link.to}
                  role="listitem"
                  className={({ isActive }) => `nav-pill${isActive ? " nav-pill--active" : ""}`}
                >
                  {link.label}
                </NavLink>
              ))}
            </div>
          </>
        ) : null}
      </nav>

      <Outlet />
    </div>
  );
}

export default AppShell;