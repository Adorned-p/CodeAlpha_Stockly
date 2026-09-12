import { useEffect, useState } from "react";
import {
  User,
  Mail,
  ShieldCheck,
  IndianRupee,
  RefreshCw,
  CalendarDays,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";

function Profile() {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchProfile = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/users/me");

      setUser(response.data);
    } catch (error) {
      console.error("Failed to fetch profile:", error);

      setError(
        error.response?.data?.message ||
          "Unable to load your profile."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const formatMoney = (value) => {
    return Number(value || 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  const formatDate = (date) => {
    if (!date) return "—";

    return new Date(date).toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "long",
      year: "numeric",
    });
  };

  if (loading) {
    return (
      <DashboardLayout>
        <div className="stock-details-state">
          <RefreshCw
            size={28}
            className="refresh-spinning"
          />

          <p>Loading profile...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !user) {
    return (
      <DashboardLayout>
        <div className="stock-details-state">
          <User size={30} />

          <h2>Unable to load profile</h2>

          <p>
            {error || "Something went wrong."}
          </p>

          <button
            className="primary-button"
            onClick={fetchProfile}
          >
            Try Again
          </button>
        </div>
      </DashboardLayout>
    );
  }

  return (
    <DashboardLayout>
      <div className="profile-page">

        {/* HEADER */}

        <div className="page-header">
          <div>
            <p className="eyebrow">
              ACCOUNT
            </p>

            <h1>Your Profile</h1>

            <p className="page-description">
              View your account information and
              investment details.
            </p>
          </div>

          <button
            className="refresh-button"
            onClick={fetchProfile}
            disabled={loading}
          >
            <RefreshCw
              size={16}
              className={
                loading
                  ? "refresh-spinning"
                  : ""
              }
            />

            Refresh
          </button>
        </div>


        {/* PROFILE */}

        <section className="profile-card">

          <div className="profile-card-header">

            <div className="profile-avatar">
              {user.name
                ?.charAt(0)
                .toUpperCase()}
            </div>

            <div>
              <h2>{user.name}</h2>

              <p>
                {user.role || "CUSTOMER"}
              </p>
            </div>

          </div>


          <div className="profile-details">

            {/* NAME */}

            <div className="profile-detail">

              <div className="profile-detail-icon">
                <User size={19} />
              </div>

              <div>
                <span>Full Name</span>
                <strong>
                  {user.name || "—"}
                </strong>
              </div>

            </div>


            {/* EMAIL */}

            <div className="profile-detail">

              <div className="profile-detail-icon">
                <Mail size={19} />
              </div>

              <div>
                <span>Email Address</span>
                <strong>
                  {user.email || "—"}
                </strong>
              </div>

            </div>


            {/* ROLE */}

            <div className="profile-detail">

              <div className="profile-detail-icon">
                <ShieldCheck size={19} />
              </div>

              <div>
                <span>Account Type</span>
                <strong>
                  {user.role || "CUSTOMER"}
                </strong>
              </div>

            </div>


            {/* BALANCE */}

            <div className="profile-detail">

              <div className="profile-detail-icon">
                <IndianRupee size={19} />
              </div>

              <div>
                <span>Available Balance</span>
                <strong>
                  ₹{formatMoney(
                    user.virtualBalance
                  )}
                </strong>
              </div>

            </div>


            {/* CREATED */}

            <div className="profile-detail">

              <div className="profile-detail-icon">
                <CalendarDays size={19} />
              </div>

              <div>
                <span>Member Since</span>
                <strong>
                  {formatDate(user.createdAt)}
                </strong>
              </div>

            </div>

          </div>

        </section>

      </div>
    </DashboardLayout>
  );
}

export default Profile;