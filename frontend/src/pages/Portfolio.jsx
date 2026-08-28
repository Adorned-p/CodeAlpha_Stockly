import { useEffect, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  BriefcaseBusiness,
  IndianRupee,
  RefreshCw,
  TrendingUp,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";

function Portfolio() {
  const [portfolio, setPortfolio] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchPortfolio = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/portfolio");

      setPortfolio(response.data);
    } catch (error) {
      console.error("Failed to fetch portfolio:", error);

      setError(
        error.response?.data?.message ||
          "Unable to load your portfolio."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPortfolio();
  }, []);

  const formatMoney = (value) => {
    return Number(value || 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
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
          <p>Loading portfolio...</p>
        </div>
      </DashboardLayout>
    );
  }

  if (error || !portfolio) {
    return (
      <DashboardLayout>
        <div className="stock-details-state">
          <TrendingUp size={30} />

          <h2>Unable to load portfolio</h2>

          <p>
            {error || "Something went wrong."}
          </p>

          <button
            className="primary-button"
            onClick={fetchPortfolio}
          >
            Try Again
          </button>
        </div>
      </DashboardLayout>
    );
  }

  const holdings = portfolio.holdings || [];

  const totalProfitLoss =
    Number(portfolio.totalProfitLoss || 0);

  const isProfit = totalProfitLoss >= 0;

  return (
    <DashboardLayout>
      <div className="portfolio-page">

        {/* HEADER */}

        <div className="page-header">

          <div>
            <p className="eyebrow">
              PORTFOLIO
            </p>

            <h1>Your Portfolio</h1>

            <p className="page-description">
              Track your investments and portfolio
              performance.
            </p>
          </div>

          <button
            className="refresh-button"
            onClick={fetchPortfolio}
            disabled={loading}
          >
            <RefreshCw size={16} />
            Refresh
          </button>

        </div>

        {/* SUMMARY CARDS */}

        <div className="stats-grid">

          <div className="stat-card">

            <div className="stat-card-top">
              <span>Available Cash</span>

              <div className="stat-icon">
                <IndianRupee size={19} />
              </div>
            </div>

            <div className="stat-value">
              ₹{formatMoney(
                portfolio.virtualBalance
              )}
            </div>

            <div className="stat-subtext">
              Ready to invest
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-card-top">
              <span>Total Invested</span>

              <div className="stat-icon">
                <BriefcaseBusiness size={19} />
              </div>
            </div>

            <div className="stat-value">
              ₹{formatMoney(
                portfolio.totalInvested
              )}
            </div>

            <div className="stat-subtext">
              Across {holdings.length}{" "}
              {holdings.length === 1
                ? "holding"
                : "holdings"}
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-card-top">
              <span>Portfolio Value</span>

              <div className="stat-icon">
                <TrendingUp size={19} />
              </div>
            </div>

            <div className="stat-value">
              ₹{formatMoney(
                portfolio.currentPortfolioValue
              )}
            </div>

            <div className="stat-subtext">
              Current market value
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-card-top">
              <span>Total P/L</span>

              <div className="stat-icon">
                {isProfit ? (
                  <ArrowUpRight size={19} />
                ) : (
                  <ArrowDownRight size={19} />
                )}
              </div>
            </div>

            <div
              className={
                isProfit
                  ? "stat-value positive-text"
                  : "stat-value negative-text"
              }
            >
              {isProfit ? "+" : ""}
              ₹{formatMoney(totalProfitLoss)}
            </div>

            <div
              className={
                isProfit
                  ? "stat-change positive"
                  : "stat-change negative"
              }
            >
              {isProfit ? (
                <ArrowUpRight size={15} />
              ) : (
                <ArrowDownRight size={15} />
              )}

              {isProfit
                ? "Portfolio is up"
                : "Portfolio is down"}
            </div>

          </div>

        </div>

        {/* HOLDINGS */}

        <section className="dashboard-card">

          <div className="card-header">

            <div>
              <h2>Your Holdings</h2>

              <p>
                Stocks currently in your portfolio
              </p>
            </div>

          </div>

          {holdings.length === 0 ? (

            <div className="market-state">

              <BriefcaseBusiness size={30} />

              <h3>
                No holdings yet
              </h3>

              <p>
                Buy some stocks to see them
                appear here.
              </p>

            </div>

          ) : (

            <div className="holdings-table">

              <div className="table-header">
                <span>Stock</span>
                <span>Qty</span>
                <span>Avg. Price</span>
                <span>Current Price</span>
                <span>Value</span>
                <span>P/L</span>
              </div>

              {holdings.map((holding) => {

                const profitLoss =
                  Number(
                    holding.profitLoss || 0
                  );

                const holdingProfit =
                  profitLoss >= 0;

                return (
                  <div
                    className="holding-row"
                    key={holding.symbol}
                    onClick={() =>
                      window.location.href =
                        `/stocks/${holding.symbol}`
                    }
                    style={{
                      cursor: "pointer",
                    }}
                  >

                    <div className="holding-name">

                      <strong>
                        {holding.symbol}
                      </strong>

                      <span>
                        {holding.companyName}
                      </span>

                    </div>

                    <span>
                      {holding.quantity}
                    </span>

                    <span>
                      ₹{formatMoney(
                        holding.averageBuyPrice
                      )}
                    </span>

                    <span>
                      ₹{formatMoney(
                        holding.currentPrice
                      )}
                    </span>

                    <span>
                      ₹{formatMoney(
                        holding.currentValue
                      )}
                    </span>

                    <span
                      className={
                        holdingProfit
                          ? "positive"
                          : "negative"
                      }
                    >
                      {holdingProfit
                        ? "+"
                        : ""}
                      ₹{formatMoney(
                        profitLoss
                      )}
                    </span>

                  </div>
                );
              })}

            </div>

          )}

        </section>

      </div>
    </DashboardLayout>
  );
}

export default Portfolio;