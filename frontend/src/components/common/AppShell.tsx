import { useQuery } from "@tanstack/react-query";
import { NavLink, Outlet } from "react-router-dom";
import { useAuth } from "../../app/auth/AuthProvider";
import { queryKeys } from "../../api/queryKeys";
import { getMyCurrentWeekTitle } from "../../api/titles";
import "./AppShell.css";

const DEFAULT_MAIN_LINKS = [
  { to: "/checkin", label: "\uCCB4\uD06C" },
  { to: "/board/weekly", label: "\uC8FC\uAC04" },
  { to: "/pledges", label: "\uB2E4\uC9D0" },
];

const DEFAULT_ADMIN_LINKS = [
  { to: "/admin/nicknames", label: "\uB2C9\uB124\uC784" },
  { to: "/admin/kakkdugi", label: "\uAE4D\uB450\uAE30" },
  { to: "/admin/checkins", label: "\uAD00\uB9AC" },
  { to: "/admin/stats", label: "\uD1B5\uACC4" },
];

type AppShellProps = {
  me?: { uid?: string; loginId: string; nicknameDisplay?: string | null; role: "ADMIN" | "REGULAR" } | null;
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

  const currentTitle = titleQuery.data?.effectiveBadges?.[0] ?? "\uD0C0\uC774\uD2C0 \uC5C6\uC74C";
  const displayName = me?.nicknameDisplay?.trim() || me?.loginId || "";

  return (
    <div className="app-shell bg-grid">
      <header className="topbar">
        {me ? (
          <div className="topbar__actions topbar__actions--compact">
            <div className="topbar__logout">
              <button type="button" className="btn-logout" onClick={() => void logout()}>
                {"\uB85C\uADF8\uC544\uC6C3"}
              </button>
            </div>
            <div className="user-mini-card">
              <span className="user-mini-card__name">{displayName}</span>
              <span className="user-mini-card__title">{currentTitle}</span>
            </div>
          </div>
        ) : null}
      </header>

      <nav className="nav-card" aria-label={"\uC8FC \uBA54\uB274"}>
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