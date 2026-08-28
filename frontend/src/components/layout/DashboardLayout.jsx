import {
  BarChart3,
  BriefcaseBusiness,
  History,
  LayoutDashboard,
  LogOut,
  Search,
  Star,
  User,
} from "lucide-react";

import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

function DashboardLayout({ children }) {
  const navigate = useNavigate();
  const { logout, user } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const navigation = [
    {
      name: "Dashboard",
      path: "/dashboard",
      icon: LayoutDashboard,
    },
    {
      name: "Markets",
      path: "/markets",
      icon: BarChart3,
    },
    {
      name: "Portfolio",
      path: "/portfolio",
      icon: BriefcaseBusiness,
    },
    {
      name: "Watchlist",
      path: "/watchlist",
      icon: Star,
    },
    {
      name: "Transactions",
      path: "/transactions",
      icon: History,
    },
  ];

  // Get user's name safely
  const userName = user?.name || "User";

  // Get first letter for avatar
  const userInitial = userName
    .charAt(0)
    .toUpperCase();

  // Convert backend role into readable text
  const userRole =
    user?.role === "CUSTOMER"
      ? "Investor"
      : user?.role || "Investor";

  return (
    <div className="app-shell">

      {/* =========================
          SIDEBAR
      ========================= */}

      <aside className="sidebar">

        <div className="sidebar-brand">

          <div className="sidebar-logo">
            <BarChart3 size={22} />
          </div>

          <span>STOCKLY</span>

        </div>


        {/* NAVIGATION */}

        <nav className="sidebar-nav">

          <div className="nav-section-title">
            MAIN
          </div>

          {navigation.map((item) => {
            const Icon = item.icon;

            return (
              <NavLink
                key={item.path}
                to={item.path}
                className={({ isActive }) =>
                  `nav-item ${isActive ? "active" : ""}`
                }
              >
                <Icon size={19} />

                <span>
                  {item.name}
                </span>

              </NavLink>
            );
          })}

        </nav>


        {/* SIDEBAR BOTTOM */}

        <div className="sidebar-bottom">

          <NavLink
            to="/profile"
            className={({ isActive }) =>
              `nav-item ${isActive ? "active" : ""}`
            }
          >
            <User size={19} />

            <span>
              Profile
            </span>

          </NavLink>


          <button
            className="nav-item logout-button"
            onClick={handleLogout}
          >
            <LogOut size={19} />

            <span>
              Logout
            </span>

          </button>

        </div>

      </aside>


      {/* =========================
          MAIN CONTENT
      ========================= */}

      <main className="main-content">

        {/* TOP BAR */}

        <header className="topbar">

          {/* SEARCH */}

          <div className="topbar-search">

            <Search size={18} />

            <input
              type="text"
              placeholder="Search stocks..."
            />

          </div>


          {/* USER */}

          <div className="topbar-user">

            <div className="user-avatar">
              {userInitial}
            </div>

            <div className="user-info">

              <strong>
                {userName}
              </strong>

              <span>
                {userRole}
              </span>

            </div>

          </div>

        </header>


        {/* PAGE */}

        <div className="page-content">
          {children}
        </div>

      </main>

    </div>
  );
}

export default DashboardLayout;