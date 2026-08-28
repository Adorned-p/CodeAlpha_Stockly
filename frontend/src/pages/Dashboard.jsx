import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";

import api from "../services/api";

import {
  ArrowDownRight,
  ArrowUpRight,
  BarChart3,
  BriefcaseBusiness,
  ChevronDown,
  Eye,
  Gift,
  IndianRupee,
  RefreshCw,
  Star,
  TrendingUp,
  UserPlus,
  Wallet,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import dashboardBg from "../assets/dashboard-bg.jpg";


function Dashboard() {

    const handleInvite = async () => {
      const referralLink =
        `${window.location.origin}/register?ref=STOCKLY`;

      try {
        await navigator.clipboard.writeText(
          referralLink
        );

        setInviteMessage(
          "Referral link copied to clipboard!"
        );

      } catch (error) {

        setInviteMessage(
          "Your referral link is ready to share."
        );

      }

      setShowInviteMessage(true);

      setTimeout(() => {
        setShowInviteMessage(false);
      }, 3000);
    };

    const [marketData, setMarketData] = useState({
      nifty: {
        name: "NIFTY 50",
        value: 24813.75,
        change: 0.86,
      },
      sensex: {
        name: "SENSEX",
        value: 81330.43,
        change: 0.74,
      },
    });

    const [inviteMessage, setInviteMessage] =
      useState("");

    const [showInviteMessage, setShowInviteMessage] =
      useState(false);

  const navigate = useNavigate();

  const [user, setUser] = useState(null);
  const [portfolio, setPortfolio] = useState(null);
  const [watchlist, setWatchlist] = useState([]);

  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const [error, setError] = useState("");

  /*
   * Portfolio chart period
   */
  const [chartPeriod, setChartPeriod] =
    useState("1D");

  const [showPeriodMenu, setShowPeriodMenu] =
    useState(false);


  /*
   * =====================================================
   * LOAD DASHBOARD
   * =====================================================
   */

  const loadDashboard = async (isRefresh = false) => {
    try {
      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const [
        userResponse,
        portfolioResponse,
        watchlistResponse,
        marketResponse,
      ] = await Promise.all([
        api.get("/users/me"),
        api.get("/portfolio"),
        api.get("/watchlist"),
        api.get("/market/indices"),
      ]);

      setUser(userResponse.data);

      setPortfolio(
        portfolioResponse.data
      );

      setWatchlist(
        Array.isArray(
          watchlistResponse.data
        )
          ? watchlistResponse.data
          : []
      );

      const indices =
        Array.isArray(marketResponse.data)
          ? marketResponse.data
          : [];

      const nifty =
        indices.find(
          (index) =>
            index.symbol === "NIFTY50"
        );

      const sensex =
        indices.find(
          (index) =>
            index.symbol === "SENSEX"
        );

      if (nifty) {
        setMarketData((current) => ({
          ...current,

          nifty: {
            name: nifty.name,
            value: Number(
              nifty.value
            ),
            change: Number(
              nifty.changePercentage
            ),
          },
        }));
      }

      if (sensex) {
        setMarketData((current) => ({
          ...current,

          sensex: {
            name: sensex.name,
            value: Number(
              sensex.value
            ),
            change: Number(
              sensex.changePercentage
            ),
          },
        }));
      }

      /*
       * Keep market cards stable for now.
       *
       * When you create a real market API,
       * replace this section with:
       *
       * const marketResponse =
       *   await api.get("/market/indices");
       *
       * setMarketData(marketResponse.data);
       */

    } catch (err) {
      console.error(
        "Dashboard loading failed:",
        err
      );

      setError(
        err?.response?.data?.message ||
        "Unable to load your dashboard."
      );

    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };


  useEffect(() => {
    loadDashboard();
  }, []);


  /*
   * =====================================================
   * PORTFOLIO VALUES
   * =====================================================
   */

  const portfolioValue = Number(
    portfolio?.currentPortfolioValue ?? 0
  );

  const investedAmount = Number(
    portfolio?.totalInvested ?? 0
  );

  const availableBalance = Number(
    portfolio?.virtualBalance ?? 0
  );

  const totalProfitLoss = Number(
    portfolio?.totalProfitLoss ?? 0
  );

  const holdings = Array.isArray(
    portfolio?.holdings
  )
    ? portfolio.holdings
    : [];


  /*
   * Today's P&L
   */

  const todayProfitLoss = Number(
    portfolio?.todayProfitLoss ??
    portfolio?.dailyProfitLoss ??
    portfolio?.todaysProfitLoss ??
    portfolio?.todayPnL ??
    0
  );


  /*
   * =====================================================
   * RETURN %
   * =====================================================
   */

  const allTimeReturnPercentage =
    investedAmount > 0
      ? (totalProfitLoss /
          investedAmount) *
        100
      : 0;


  const todayReturnPercentage =
    portfolio?.todayReturnPercentage ??
    portfolio?.dailyReturnPercentage ??
    portfolio?.todaysReturnPercentage ??
    (
      investedAmount > 0
        ? (todayProfitLoss /
            investedAmount) *
          100
        : 0
    );


  /*
   * =====================================================
   * FORMATTERS
   * =====================================================
   */

  const formatMoney = (value) => {

    return new Intl.NumberFormat(
      "en-IN",
      {
        style: "currency",
        currency: "INR",
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    ).format(Number(value || 0));

  };


  const formatCompactMoney = (
    value
  ) => {

    const number =
      Number(value || 0);

    if (number >= 10000000) {
      return `₹${(
        number / 10000000
      ).toFixed(1)}Cr`;
    }

    if (number >= 100000) {
      return `₹${(
        number / 100000
      ).toFixed(1)}L`;
    }

    if (number >= 1000) {
      return `₹${(
        number / 1000
      ).toFixed(1)}K`;
    }

    return `₹${number.toFixed(0)}`;

  };


  const formatPercent = (
    value
  ) => {

    const number =
      Number(value || 0);

    return `${number >= 0 ? "+" : ""}${number.toFixed(
      2
    )}%`;

  };


  const formatSignedMoney = (
    value
  ) => {

    const number =
      Number(value || 0);

    return `${number >= 0 ? "+" : "-"}${formatMoney(
      Math.abs(number)
    )}`;

  };


  /*
   * =====================================================
   * USER
   * =====================================================
   */

  const firstName =
    user?.name
      ?.trim()
      ?.split(/\s+/)[0] ||
    "Investor";


  /*
   * =====================================================
   * ALLOCATION
   * =====================================================
   */

  const allocationData =
    useMemo(() => {

      const sorted =
        [...holdings]
          .map((holding) => ({
            symbol:
              holding.symbol ||
              "UNKNOWN",

            value:
              Number(
                holding.currentValue ||
                0
              ),
          }))
          .filter(
            (item) =>
              item.value > 0
          )
          .sort(
            (a, b) =>
              b.value - a.value
          );

      if (!sorted.length) {
        return [];
      }

      const topThree =
        sorted.slice(0, 3);

      const othersValue =
        sorted
          .slice(3)
          .reduce(
            (sum, item) =>
              sum + item.value,
            0
          );

      const result =
        topThree.map(
          (item) => ({
            ...item,

            percentage:
              portfolioValue > 0
                ? (
                    item.value /
                    portfolioValue
                  ) * 100
                : 0,
          })
        );

      if (othersValue > 0) {

        result.push({
          symbol: "Others",
          value: othersValue,

          percentage:
            portfolioValue > 0
              ? (
                  othersValue /
                  portfolioValue
                ) * 100
              : 0,
        });

      }

      return result;

    }, [
      holdings,
      portfolioValue,
    ]);


  /*
   * =====================================================
   * TOP HOLDINGS
   * =====================================================
   */

  const topHoldings =
    useMemo(() => {

      return [...holdings]
        .sort(
          (a, b) =>
            Number(
              b.currentValue || 0
            ) -
            Number(
              a.currentValue || 0
            )
        )
        .slice(0, 3);

    }, [holdings]);


  /*
   * =====================================================
   * MARKET MOVERS
   * =====================================================
   */

  const marketMovers =
    useMemo(() => {

      return watchlist
        .map((stock) => {

          const currentPrice =
            Number(
              stock.currentPrice ??
              stock.price ??
              stock.lastPrice ??
              0
            );

          const previousPrice =
            Number(
              stock.previousPrice ??
              stock.previousClose ??
              stock.prevClose ??
              0
            );

          let change =
            Number(
              stock.changePercentage ??
              stock.percentChange ??
              stock.changePercent ??
              stock.percentageChange ??
              0
            );

          if (
            !change &&
            previousPrice > 0
          ) {

            change =
              (
                (
                  currentPrice -
                  previousPrice
                ) /
                previousPrice
              ) * 100;

          }

          return {
            ...stock,
            currentPrice,
            change,
          };

        })
        .sort(
          (a, b) =>
            Math.abs(b.change) -
            Math.abs(a.change)
        )
        .slice(0, 4);

    }, [watchlist]);


  /*
   * =====================================================
   * CHART PERIOD
   * =====================================================
   */

  const periodLabels = {
    "1D": "Today",
    "1W": "1 Week",
    "1M": "1 Month",
    "1Y": "1 Year",
  };


  const changePeriod = (
    period
  ) => {

    setChartPeriod(period);
    setShowPeriodMenu(false);

  };


  /*
   * =====================================================
   * LOADING
   * =====================================================
   */

  if (loading) {

    return (
      <DashboardLayout>

        <div className="dashboard-loading">

          <div className="loading-orb" />

          <span>
            Loading your portfolio...
          </span>

        </div>

      </DashboardLayout>
    );

  }


  /*
   * =====================================================
   * ERROR
   * =====================================================
   */

  if (
    error &&
    !portfolio
  ) {

    return (
      <DashboardLayout>

        <div className="dashboard-error">

          <BarChart3 size={38} />

          <h2>
            Dashboard unavailable
          </h2>

          <p>
            {error}
          </p>

          <button
            className="retry-button"
            onClick={() =>
              loadDashboard()
            }
          >
            Try Again
          </button>

        </div>

      </DashboardLayout>
    );

  }


  /*
   * =====================================================
   * DASHBOARD
   * =====================================================
   */

  return (

    <DashboardLayout>

      <div
        className="dashboard-page"
        style={{
          "--dashboard-bg":
            `url("${dashboardBg}")`,
        }}
      >

        <div className="dashboard-background" />


        <main className="dashboard-content">


          {/* =================================================
              HEADER
          ================================================= */}

          <header className="dashboard-header">

            <p className="dashboard-eyebrow">
              OVERVIEW
            </p>

            <h1>
              Good evening, {firstName}

              <span className="wave">
                👋
              </span>
            </h1>

            <p>
              Here's what's happening with
              your investments today.
            </p>

          </header>


          {/* =================================================
              ERROR
          ================================================= */}

          {error && (

            <div className="dashboard-inline-error">

              <span>
                {error}
              </span>

              <button
                onClick={() =>
                  loadDashboard(true)
                }
              >
                Retry
              </button>

            </div>

          )}


          {/* =================================================
              PORTFOLIO HERO
          ================================================= */}

          <section className="portfolio-hero">


            <div className="hero-left">

              <div className="hero-label">

                <span>
                  Portfolio Value
                </span>

                <Eye size={15} />

              </div>


              <div className="hero-value">

                {formatMoney(
                  portfolioValue
                )}

              </div>


              <div className="hero-return-label">
                Total Returns
              </div>


              <div
                className={`hero-return ${
                  totalProfitLoss >= 0
                    ? "positive"
                    : "negative"
                }`}
              >

                {totalProfitLoss >= 0 ? (
                  <ArrowUpRight
                    size={17}
                  />
                ) : (
                  <ArrowDownRight
                    size={17}
                  />
                )}

                {formatSignedMoney(
                  totalProfitLoss
                )}

                <span>
                  (
                  {formatPercent(
                    allTimeReturnPercentage
                  )}
                  )
                </span>

              </div>

            </div>


            {/* =================================================
                PERFORMANCE
            ================================================= */}

            <div className="hero-chart">

              <div className="chart-top">

                <span>
                  Portfolio Performance
                </span>


                {/* WORKING PERIOD SELECTOR */}

                <div className="period-selector">

                  <button
                    type="button"
                    className="chart-period"
                    onClick={() =>
                      setShowPeriodMenu(
                        (value) => !value
                      )
                    }
                  >

                    {periodLabels[
                      chartPeriod
                    ]}

                    <ChevronDown
                      size={14}
                    />

                  </button>


                  {showPeriodMenu && (

                    <div className="period-menu">

                      {Object.entries(
                        periodLabels
                      ).map(
                        ([
                          value,
                          label,
                        ]) => (

                          <button
                            key={value}
                            type="button"
                            className={
                              chartPeriod ===
                              value
                                ? "active"
                                : ""
                            }
                            onClick={() =>
                              changePeriod(
                                value
                              )
                            }
                          >
                            {label}
                          </button>

                        )
                      )}

                    </div>

                  )}

                </div>

              </div>


              <svg
                viewBox="0 0 600 180"
                preserveAspectRatio="none"
                className="portfolio-svg"
              >

                <defs>

                  <linearGradient
                    id="chartFill"
                    x1="0"
                    y1="0"
                    x2="0"
                    y2="1"
                  >

                    <stop
                      offset="0%"
                      stopOpacity="0.25"
                    />

                    <stop
                      offset="100%"
                      stopOpacity="0"
                    />

                  </linearGradient>

                </defs>


                <path
                  className="chart-area"
                  d="
                    M0 150
                    L35 142
                    L70 145
                    L105 128
                    L140 135
                    L175 108
                    L210 116
                    L245 92
                    L280 104
                    L315 82
                    L350 96
                    L385 72
                    L420 82
                    L455 58
                    L490 70
                    L525 43
                    L560 55
                    L600 20
                    L600 180
                    L0 180
                    Z
                  "
                />


                <path
                  className="chart-line"
                  pathLength="1"
                  d="
                    M0 150
                    L35 142
                    L70 145
                    L105 128
                    L140 135
                    L175 108
                    L210 116
                    L245 92
                    L280 104
                    L315 82
                    L350 96
                    L385 72
                    L420 82
                    L455 58
                    L490 70
                    L525 43
                    L560 55
                    L600 20
                  "
                />


                <circle
                  className="chart-dot"
                  cx="600"
                  cy="20"
                  r="4"
                />

              </svg>

            </div>

          </section>


          {/* =================================================
              KPI
          ================================================= */}

          <section className="dashboard-stats">

            <DashboardStat
              icon={
                <Wallet size={18} />
              }
              label="Invested Amount"
              value={formatMoney(
                investedAmount
              )}
              subtitle="Total invested"
            />


            <DashboardStat
              icon={
                <TrendingUp size={18} />
              }
              label="Today's Return"
              value={formatSignedMoney(
                todayProfitLoss
              )}
              subtitle={formatPercent(
                todayReturnPercentage
              )}
              positive={
                todayProfitLoss >= 0
              }
            />


            <DashboardStat
              icon={
                <IndianRupee size={18} />
              }
              label="Available Cash"
              value={formatMoney(
                availableBalance
              )}
              subtitle="Available to invest"
            />


            <DashboardStat
              icon={
                <BarChart3 size={18} />
              }
              label="All Time Returns"
              value={formatSignedMoney(
                totalProfitLoss
              )}
              subtitle={`${formatPercent(
                allTimeReturnPercentage
              )} all time`}
              positive={
                totalProfitLoss >= 0
              }
            />

          </section>


          {/* =================================================
              LOWER GRID
          ================================================= */}

          <section className="dashboard-main-grid">


            {/* ALLOCATION */}

            <div className="dashboard-panel allocation-panel">

              <div className="panel-header">

                <div>

                  <h2>
                    Portfolio Allocation
                  </h2>

                  <p>
                    How your portfolio is distributed
                  </p>

                </div>

              </div>


              {allocationData.length === 0 ? (

                <EmptyDashboardState
                  icon={
                    <BriefcaseBusiness
                      size={28}
                    />
                  }
                  title="Your portfolio is empty"
                  message="Start investing to see your allocation here."
                />

              ) : (

                <>

                  <div className="allocation-content">

                    <div className="allocation-chart">

                      <div
                        className="allocation-ring"
                        style={{
                          background:
                            createDonutGradient(
                              allocationData
                            ),
                        }}
                      >

                        <div className="allocation-center">

                          <strong>
                            {formatCompactMoney(
                              portfolioValue
                            )}
                          </strong>

                          <span>
                            Total
                          </span>

                        </div>

                      </div>

                    </div>


                    <div className="allocation-list">

                      {allocationData.map(
                        (
                          item,
                          index
                        ) => (

                          <div
                            className="allocation-row"
                            key={
                              item.symbol
                            }
                          >

                            <div className="allocation-name">

                              <span
                                className={`allocation-dot dot-${index}`}
                              />

                              <strong>
                                {
                                  item.symbol
                                }
                              </strong>

                            </div>


                            <div className="allocation-number">

                              <strong>
                                {item.percentage.toFixed(
                                  1
                                )}
                                %
                              </strong>

                              <span>
                                {formatMoney(
                                  item.value
                                )}
                              </span>

                            </div>

                          </div>

                        )
                      )}

                    </div>

                  </div>


                  <button
                    className="panel-link"
                    type="button"
                    onClick={() =>
                      navigate(
                        "/portfolio"
                      )
                    }
                  >
                    View full portfolio

                    <ArrowUpRight
                      size={15}
                    />

                  </button>

                </>

              )}

            </div>


            {/* =================================================
                TOP HOLDINGS
            ================================================= */}

            <div className="dashboard-panel">

              <div className="panel-header">

                <div>

                  <h2>
                    Top Holdings
                  </h2>

                  <p>
                    Your current positions
                  </p>

                </div>


                <button
                  className="view-link"
                  type="button"
                  onClick={() =>
                    navigate(
                      "/portfolio"
                    )
                  }
                >
                  View all
                </button>

              </div>


              <div className="holding-list">

                {topHoldings.length === 0 ? (

                  <EmptyDashboardState
                    icon={
                      <BriefcaseBusiness
                        size={28}
                      />
                    }
                    title="No holdings"
                    message="Your purchased stocks will appear here."
                    small
                  />

                ) : (

                  topHoldings.map(
                    (holding) => {

                      const pnl =
                        Number(
                          holding.profitLoss ??
                          holding.pnl ??
                          0
                        );

                      const currentValue =
                        Number(
                          holding.currentValue ??
                          0
                        );

                      const quantity =
                        Number(
                          holding.quantity ??
                          0
                        );

                      return (

                        <div
                          className="holding-item"
                          key={
                            holding.symbol
                          }
                        >

                          <StockAvatar
                            symbol={
                              holding.symbol
                            }
                          />


                          <div className="holding-info">

                            <strong>
                              {
                                holding.symbol
                              }
                            </strong>

                            <span>
                              {
                                holding.companyName ||
                                holding.name ||
                                "Stock"
                              }
                            </span>

                            <small>
                              {quantity}{" "}
                              {quantity === 1
                                ? "Share"
                                : "Shares"}
                            </small>

                          </div>


                          <div className="holding-value">

                            <strong>
                              {formatMoney(
                                currentValue
                              )}
                            </strong>

                            <span
                              className={
                                pnl >= 0
                                  ? "positive"
                                  : "negative"
                              }
                            >
                              {formatSignedMoney(
                                pnl
                              )}
                            </span>

                          </div>

                        </div>

                      );

                    }
                  )

                )}

              </div>

            </div>


            {/* =================================================
                MARKET MOVERS
            ================================================= */}

            <div className="dashboard-panel movers-panel">

              <div className="panel-header">

                <div>

                  <h2>
                    Market Movers
                  </h2>

                  <p>
                    Stocks you're watching
                  </p>

                </div>


                <button
                  className="view-link"
                  type="button"
                  onClick={() =>
                    navigate(
                      "/watchlist"
                    )
                  }
                >
                  View all
                </button>

              </div>


              <div className="movers-list">

                {marketMovers.length === 0 ? (

                  <EmptyDashboardState
                    icon={
                      <Star size={28} />
                    }
                    title="Watchlist is empty"
                    message="Add stocks to track their movement."
                    small
                  />

                ) : (

                  marketMovers.map(
                    (stock) => (

                      <button
                        type="button"
                        className="mover-item"
                        key={
                          stock.symbol
                        }
                        onClick={() =>
                          navigate(
                            `/stocks/${stock.symbol}`
                          )
                        }
                      >

                        <StockAvatar
                          symbol={
                            stock.symbol
                          }
                          small
                        />


                        <div className="mover-info">

                          <strong>
                            {
                              stock.symbol
                            }
                          </strong>

                          <span>
                            {
                              stock.companyName ||
                              stock.name ||
                              "Stock"
                            }
                          </span>

                        </div>


                        <div className="mover-price">

                          <strong>
                            {formatMoney(
                              stock.currentPrice
                            )}
                          </strong>

                          <span
                            className={
                              stock.change >= 0
                                ? "positive"
                                : "negative"
                            }
                          >
                            {formatPercent(
                              stock.change
                            )}
                          </span>

                        </div>


                        <MiniChart
                          positive={
                            stock.change >= 0
                          }
                        />

                      </button>

                    )

                  )

                )}

              </div>

            </div>

          </section>


          {/* =================================================
              INVITE
          ================================================= */}

          <section className="invite-banner">

            <div className="invite-icon">
              <Gift size={40} />
            </div>


            <div className="invite-copy">

              <h2>
                Invite & Earn Rewards!
              </h2>

              <p>
                Invite your friends to Stockly
                and earn exciting rewards
              </p>

              <span>
                when they start investing.
              </span>

            </div>


            <button
              className="invite-button"
              type="button"
              onClick={handleInvite}
            >
              Invite Now

              <UserPlus size={16} />
            </button>


            <div className="invite-graphic">

              <div className="invite-bars">

                <span />
                <span />
                <span />
                <span />
                <span />
                <span />

              </div>

              <TrendingUp
                size={105}
                strokeWidth={1.2}
              />

            </div>

          </section>


          {/* =================================================
              BOTTOM MARKET SECTION
          ================================================= */}

          <section className="dashboard-footer">

            <div className="dashboard-market-snapshot">

              <MarketSnapshot
                name={marketData.nifty.name}
                value={marketData.nifty.value}
                change={marketData.nifty.change}
                onClick={() => navigate("/markets")}
              />

              <MarketSnapshot
                name={marketData.sensex.name}
                value={marketData.sensex.value}
                change={marketData.sensex.change}
                onClick={() => navigate("/markets")}
              />

            </div>


            <button
              className="dashboard-refresh"
              type="button"
              onClick={() =>
                loadDashboard(true)
              }
              disabled={refreshing}
            >

              <RefreshCw
                size={14}
                className={
                  refreshing
                    ? "refresh-spin"
                    : ""
                }
              />

              <span>
                {refreshing
                  ? "Refreshing..."
                  : "Refresh"}
              </span>

            </button>

          </section>

          {/* =================================================
              INVITE FEEDBACK
          ================================================= */}

          {showInviteMessage && (

            <div className="invite-toast">

              <span className="invite-toast-icon">
                <Gift size={15} />
              </span>

              <span>
                {inviteMessage}
              </span>

            </div>

          )}


        </main>

      </div>

    </DashboardLayout>
  );
}


/* =====================================================
   STAT
===================================================== */

function DashboardStat({
  icon,
  label,
  value,
  subtitle,
  positive = false,
}) {

  return (

    <div className="dashboard-stat">

      <div className="stat-icon-box">
        {icon}
      </div>

      <div className="stat-content">

        <span>
          {label}
        </span>

        <strong>
          {value}
        </strong>

        <small
          className={
            positive
              ? "positive"
              : ""
          }
        >
          {subtitle}
        </small>

      </div>

    </div>

  );
}


/* =====================================================
   EMPTY STATE
===================================================== */

function EmptyDashboardState({
  icon,
  title,
  message,
  small = false,
}) {

  return (

    <div
      className={`dashboard-empty ${
        small ? "small" : ""
      }`}
    >

      {icon}

      <strong>
        {title}
      </strong>

      <span>
        {message}
      </span>

    </div>

  );
}


/* =====================================================
   STOCK AVATAR
===================================================== */

function StockAvatar({
  symbol,
  small = false,
}) {

  return (

    <div
      className={`stock-avatar ${
        small
          ? "small-avatar"
          : ""
      }`}
    >
      {symbol
        ?.trim()
        ?.charAt(0) || "?"}
    </div>

  );
}


function MarketSnapshot({
  name,
  value,
  change,
  onClick,
}) {
  const numericValue = Number(value || 0);
  const numericChange = Number(change || 0);

  return (
    <button
      type="button"
      className="market-snapshot"
      onClick={onClick}
    >
      <div className="market-snapshot-info">
        <span className="market-name">
          {name}
        </span>

        <strong className="market-value">
          {numericValue.toLocaleString("en-IN", {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })}
        </strong>

        <small
          className={
            numericChange >= 0
              ? "positive"
              : "negative"
          }
        >
          {numericChange >= 0 ? "+" : ""}
          {numericChange.toFixed(2)}%
        </small>
      </div>

      <MiniChart
        positive={numericChange >= 0}
      />
    </button>
  );
}


/* =====================================================
   MINI CHART
===================================================== */

function MiniChart({
  positive = true,
}) {

  return (

    <svg
      className={`mini-chart ${
        positive
          ? "mini-chart-positive"
          : "mini-chart-negative"
      }`}
      viewBox="0 0 100 40"
      preserveAspectRatio="none"
    >

      <path
        d={
          positive
            ? `
              M0 31
              L10 27
              L18 30
              L28 18
              L38 23
              L48 14
              L58 19
              L68 10
              L77 15
              L87 6
              L100 10
            `
            : `
              M0 8
              L10 12
              L18 9
              L28 17
              L38 14
              L48 23
              L58 19
              L68 27
              L77 23
              L87 31
              L100 28
            `
        }
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
      />

    </svg>

  );
}


/* =====================================================
   DONUT
===================================================== */

function createDonutGradient(
  allocation
) {

  if (!allocation.length) {
    return "#1d2a35";
  }

  const colors = [
    "#19d3c5",
    "#2765c7",
    "#7146ce",
    "#e99b39",
  ];

  let current = 0;

  const stops =
    allocation.map(
      (item, index) => {

        const start = current;

        current +=
          item.percentage;

        return `${
          colors[
            index %
              colors.length
          ]
        } ${start}% ${current}%`;

      }
    );

  return `conic-gradient(${stops.join(
    ", "
  )})`;

}


export default Dashboard;