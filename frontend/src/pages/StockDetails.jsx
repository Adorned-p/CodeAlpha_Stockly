import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

import {
  ArrowLeft,
  ArrowDown,
  ArrowUp,
  CheckCircle2,
  Minus,
  Plus,
  RefreshCw,
  Star,
  TrendingDown,
  X,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";

/* =========================================================
   STOCK CHART TOOLTIP
========================================================= */

const StockChartTooltip = ({
  active,
  payload,
}) => {
  if (!active || !payload || !payload.length) {
    return null;
  }

  const data = payload[0]?.payload;

  if (!data) {
    return null;
  }

  const price = Number(data.price);

  if (!Number.isFinite(price)) {
    return null;
  }

  const date = data.time
    ? new Date(data.time)
    : null;

  const formattedDate =
    date && !Number.isNaN(date.getTime())
      ? date.toLocaleString("en-IN", {
          day: "2-digit",
          month: "short",
          year: "numeric",
          hour: "2-digit",
          minute: "2-digit",
        })
      : "";

  return (
    <div className="stock-chart-tooltip">
      <div className="stock-chart-tooltip-price">
        ₹
        {price.toLocaleString("en-IN", {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        })}
      </div>

      {formattedDate && (
        <div className="stock-chart-tooltip-time">
          {formattedDate}
        </div>
      )}
    </div>
  );
};

function StockDetails() {
  const { symbol } = useParams();
  const navigate = useNavigate();

  // =========================================================
  // STOCK / USER / HOLDING
  // =========================================================

  const [stock, setStock] = useState(null);
  const [user, setUser] = useState(null);
  const [holding, setHolding] = useState(null);

  // =========================================================
  // CHART
  // =========================================================

  const [selectedRange, setSelectedRange] =
    useState("ONE_DAY");

  const [priceHistory, setPriceHistory] =
    useState([]);

  const [historyLoading, setHistoryLoading] =
    useState(false);

  const [historyError, setHistoryError] =
    useState("");

  // =========================================================
  // TRADE
  // =========================================================

  const [tradeType, setTradeType] =
    useState("BUY");

  const [quantity, setQuantity] =
    useState(1);

  const [loading, setLoading] =
    useState(true);

  const [tradeLoading, setTradeLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const [tradeError, setTradeError] =
    useState("");

  const [showConfirmation, setShowConfirmation] =
    useState(false);

  const [success, setSuccess] =
    useState(null);

  // =========================================================
  // WATCHLIST
  // =========================================================

  const [isWatchlisted, setIsWatchlisted] =
    useState(false);

  const [watchlistLoading, setWatchlistLoading] =
    useState(false);

  // =========================================================
  // AI INSIGHT
  // =========================================================

  const [aiInsight, setAiInsight] =
    useState("");

  const [aiLoading, setAiLoading] =
    useState(false);

  const [aiError, setAiError] =
    useState("");

  // =========================================================
  // FETCH STOCK
  // =========================================================

  const fetchStock = async () => {
    try {
      setLoading(true);
      setError("");

      const response =
        await api.get(`/stocks/${symbol}`);

      setStock(response.data);
    } catch (error) {
      console.error(
        "Failed to fetch stock:",
        error
      );

      if (error.response?.status === 404) {
        setError(
          "The stock you're looking for doesn't exist."
        );
      } else {
        setError(
          "Unable to load stock information."
        );
      }
    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // FETCH HISTORY
  // =========================================================

  const fetchHistory = async (
    range = selectedRange
  ) => {
    try {
      setHistoryLoading(true);
      setHistoryError("");

      const rangeMap = {
        ONE_DAY: {
          interval: "1d",
          outputSize: 100,
        },

        ONE_WEEK: {
          interval: "1w",
          outputSize: 100,
        },

        ONE_MONTH: {
          interval: "1m",
          outputSize: 100,
        },

        THREE_MONTHS: {
          interval: "3m",
          outputSize: 120,
        },

        SIX_MONTHS: {
          interval: "6m",
          outputSize: 140,
        },

        ONE_YEAR: {
          interval: "1y",
          outputSize: 150,
        },

        ALL: {
          interval: "all",
          outputSize: 200,
        },
      };

      const selected =
        rangeMap[range] || rangeMap.ONE_MONTH;

      const response =
        await api.get(
          `/market-data/history/${symbol}`,
          {
            params: {
              interval: selected.interval,
              outputSize: selected.outputSize,
            },
          }
        );

      const data =
        Array.isArray(response.data)
          ? response.data
          : [];

      const normalizedData = data
        .map((item) => ({
          recordedAt:
            item.datetime ??
            item.timestamp,

          price:
            Number(
              item.close ??
              item.price ??
              0
            ) *
            Number(
              stock?.exchangeRateToInr ?? 1
            ),
        }))
        .filter(
          (item) =>
            item.recordedAt &&
            Number.isFinite(item.price)
        );

      setPriceHistory(normalizedData);
    } catch (error) {
      console.error(
        "Failed to fetch price history:",
        error
      );

      setPriceHistory([]);

      setHistoryError(
        error.response?.data?.message ||
          "Historical price data is not available for this period."
      );
    } finally {
      setHistoryLoading(false);
    }
  };

  // =========================================================
  // FETCH USER
  // =========================================================

  const fetchUser = async () => {
    try {
      const response =
        await api.get("/users/me");

      setUser(response.data);
    } catch (error) {
      console.error(
        "Failed to fetch user:",
        error
      );
    }
  };

  // =========================================================
  // FETCH HOLDING
  // =========================================================

  const fetchHolding = async () => {
    try {
      const response =
        await api.get(
          "/portfolio/holdings"
        );

      const holdings =
        response.data?.holdings || [];

      const currentHolding =
        holdings.find(
          (item) =>
            item.symbol
              ?.trim()
              .toUpperCase() ===
            symbol
              ?.trim()
              .toUpperCase()
        );

      if (currentHolding) {
        setHolding(currentHolding);
      } else {
        setHolding(null);
      }
    } catch (error) {
      console.error(
        "Failed to fetch holdings:",
        error
      );

      setHolding(null);
    }
  };

  // =========================================================
  // FETCH WATCHLIST
  // =========================================================

  const fetchWatchlistStatus = async () => {
    try {
      const response =
        await api.get("/watchlist");

      const watchlist =
        response.data || [];

      const exists =
        watchlist.some(
          (item) =>
            item.symbol
              ?.trim()
              .toUpperCase() ===
            symbol
              ?.trim()
              .toUpperCase()
        );

      setIsWatchlisted(exists);
    } catch (error) {
      console.error(
        "Failed to fetch watchlist:",
        error
      );

      setIsWatchlisted(false);
    }
  };

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    fetchStock();
    fetchUser();
    fetchHolding();
    fetchWatchlistStatus();
  }, [symbol]);

  // =========================================================
  // FETCH AI STOCK INSIGHT
  // =========================================================

  const fetchAiInsight = async () => {
    if (!symbol || aiLoading) {
      return;
    }

    try {
      setAiLoading(true);
      setAiError("");

      const response =
        await api.get(
          `/ai/stock/${symbol}`
        );

      setAiInsight(
        response.data || ""
      );
    } catch (error) {
      console.error(
        "Failed to fetch AI insight:",
        error
      );

      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Unable to generate AI insight.";

      setAiError(message);
    } finally {
      setAiLoading(false);
    }
  };

  // =========================================================
  // LOAD HISTORY WHEN RANGE CHANGES
  // =========================================================

  useEffect(() => {
    if (symbol && stock) {
      fetchHistory(selectedRange);
    }
  }, [
    symbol,
    selectedRange,
    stock,
  ]);

  // =========================================================
  // CHANGE CHART RANGE
  // =========================================================

  const handleRangeChange = (range) => {
    if (range === selectedRange) {
      return;
    }

    setSelectedRange(range);
  };

  // =========================================================
  // WATCHLIST TOGGLE
  // =========================================================

  const handleWatchlistToggle = async () => {
    if (!stock || watchlistLoading) {
      return;
    }

    try {
      setWatchlistLoading(true);
      setTradeError("");

      const stockSymbol =
        stock.symbol;

      if (isWatchlisted) {
        await api.delete(
          `/watchlist/${stockSymbol}`
        );

        setIsWatchlisted(false);
      } else {
        await api.post(
          `/watchlist/${stockSymbol}`
        );

        setIsWatchlisted(true);
      }
    } catch (error) {
      console.error(
        "Watchlist update failed:",
        error
      );

      const message =
        error.response?.data?.message ||
        error.response?.data?.error ||
        "Unable to update your watchlist.";

      setTradeError(message);
    } finally {
      setWatchlistLoading(false);
    }
  };

  // =========================================================
  // PRICE CHANGE
  // =========================================================

  const priceChange = useMemo(() => {
    if (!stock) {
      return 0;
    }

    const current =
      Number(stock.currentPrice || 0);

    const previous =
      Number(stock.previousClose || 0);

    if (!previous) {
      return 0;
    }

    return (
      ((current - previous) / previous) *
      100
    );
  }, [stock]);

  // =========================================================
  // CHART DATA
  // =========================================================

  const chartPoints = useMemo(() => {
    if (!priceHistory.length) {
      return [];
    }

    return [...priceHistory]
      .map((item) => ({
        price: Number(item.price),
        time: item.recordedAt,
      }))
      .filter(
        (item) =>
          Number.isFinite(item.price) &&
          item.time
      )
      .sort(
        (a, b) =>
          new Date(a.time).getTime() -
          new Date(b.time).getTime()
      );
  }, [priceHistory]);

  // =========================================================
  // CHART METRICS
  // =========================================================

  const chartMetrics = useMemo(() => {
    if (chartPoints.length === 0) {
      return {
        min: 0,
        max: 0,
        first: 0,
        last: 0,
        change: 0,
      };
    }

    const prices =
      chartPoints.map(
        (point) => point.price
      );

    const first = prices[0];

    const last =
      prices[prices.length - 1];

    const minPrice =
      Math.min(...prices);

    const maxPrice =
      Math.max(...prices);

    const change =
      first > 0
        ? ((last - first) / first) *
          100
        : 0;

    return {
      min: minPrice,
      max: maxPrice,
      first,
      last,
      change,
    };
  }, [chartPoints]);

  // =========================================================
  // CHART POSITIVE / NEGATIVE
  // =========================================================

  const chartIsPositive =
    chartMetrics.change >= 0;

  // =========================================================
  // ESTIMATED TOTAL
  // =========================================================

  const estimatedTotal =
    Number(stock?.currentPrice || 0) *
    Number(quantity || 0);

  // =========================================================
  // FORMATTING
  // =========================================================

  const formatPrice = (value) => {
    return Number(value || 0)
      .toLocaleString(
        "en-IN",
        {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }
      );
  };

  const formatBalance = (value) => {
    return Number(value || 0)
      .toLocaleString(
        "en-IN",
        {
          minimumFractionDigits: 2,
          maximumFractionDigits: 2,
        }
      );
  };

  // =========================================================
  // FORMAT CHART DATE
  // =========================================================

  const formatChartDate = (value) => {
    if (!value) {
      return "";
    }

    const date =
      new Date(value);

    if (
      Number.isNaN(
        date.getTime()
      )
    ) {
      return "";
    }

    if (
      selectedRange === "ONE_DAY"
    ) {
      return date.toLocaleTimeString(
        "en-IN",
        {
          hour: "2-digit",
          minute: "2-digit",
        }
      );
    }

    return date.toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
      }
    );
  };

  // =========================================================
  // QUANTITY CONTROLS
  // =========================================================

  const decreaseQuantity = () => {
    setQuantity((current) =>
      Math.max(1, current - 1)
    );
  };

  const increaseQuantity = () => {
    setQuantity(
      (current) => current + 1
    );
  };

  const handleQuantityChange = (
    event
  ) => {
    const value =
      parseInt(
        event.target.value,
        10
      );

    if (Number.isNaN(value)) {
      setQuantity(1);
      return;
    }

    setQuantity(
      Math.max(1, value)
    );
  };

  // =========================================================
  // TRADE TYPE
  // =========================================================

  const handleTradeTypeChange =
    async (type) => {
      setTradeType(type);
      setTradeError("");
      setSuccess(null);
      setQuantity(1);

      if (type === "SELL") {
        await fetchHolding();
      }
    };

  // =========================================================
  // OPEN CONFIRMATION
  // =========================================================

  const handleTradeSubmit = () => {
    setTradeError("");
    setSuccess(null);

    if (!stock) {
      return;
    }

    const stockStatus =
      String(
        stock.status || ""
      )
        .trim()
        .toUpperCase();

    if (stockStatus !== "ACTIVE") {
      setTradeError(
        "This stock is currently suspended."
      );

      return;
    }

    if (
      !quantity ||
      quantity < 1 ||
      !Number.isInteger(quantity)
    ) {
      setTradeError(
        "Quantity must be a positive whole number."
      );

      return;
    }

    if (
      tradeType === "SELL" &&
      quantity >
        Number(
          holding?.quantity || 0
        )
    ) {
      setTradeError(
        "You don't own enough shares to sell."
      );

      return;
    }

    setShowConfirmation(true);
  };

  // =========================================================
  // EXECUTE TRADE
  // =========================================================

  const executeTrade = async () => {
    try {
      setTradeLoading(true);
      setTradeError("");

      const endpoint =
        tradeType === "BUY"
          ? "/trades/buy"
          : "/trades/sell";

      const response =
        await api.post(
          endpoint,
          {
            symbol: stock.symbol,
            quantity: quantity,
          }
        );

      setShowConfirmation(false);

      setSuccess({
        type: tradeType,
        data: response.data,
      });

      await Promise.all([
        fetchUser(),
        fetchHolding(),
        fetchStock(),
      ]);

      setQuantity(1);
    } catch (error) {
      console.error(
        "Trade failed:",
        error
      );

      let message =
        "Unable to complete the trade.";

      if (
        error.response?.data?.message
      ) {
        message =
          error.response.data.message;
      } else if (
        error.response?.data?.error
      ) {
        message =
          error.response.data.error;
      } else if (
        error.response?.status === 400
      ) {
        message =
          "The trade request is invalid.";
      } else if (
        error.response?.status === 403
      ) {
        message =
          "You are not authorized to trade.";
      }

      setShowConfirmation(false);
      setTradeError(message);
    } finally {
      setTradeLoading(false);
    }
  };

  // =========================================================
  // LOADING
  // =========================================================

  if (loading) {
    return (
      <DashboardLayout>
        <div className="stock-details-state">
          <RefreshCw
            size={28}
            className="refresh-spinning"
          />

          <p>
            Loading stock...
          </p>
        </div>
      </DashboardLayout>
    );
  }

  // =========================================================
  // ERROR
  // =========================================================

  if (error || !stock) {
    return (
      <DashboardLayout>
        <div className="stock-details-state">
          <TrendingDown size={30} />

          <h2>
            Stock not found
          </h2>

          <p>
            {error ||
              "The stock you're looking for doesn't exist."}
          </p>

          <button
            className="primary-button"
            onClick={() =>
              navigate("/markets")
            }
          >
            Back to Market
          </button>
        </div>
      </DashboardLayout>
    );
  }

  // =========================================================
  // STOCK STATUS
  // =========================================================

  const stockStatus =
    String(
      stock.status || ""
    )
      .trim()
      .toUpperCase();

  const isStockActive =
    stockStatus === "ACTIVE";

  const currentBalance =
    Number(
      user?.virtualBalance || 0
    );

  const sharesOwned =
    Number(
      holding?.quantity || 0
    );

  // =========================================================
  // CHART RANGE LABELS
  // =========================================================

  const ranges = [
    {
      label: "1D",
      value: "ONE_DAY",
    },
    {
      label: "1W",
      value: "ONE_WEEK",
    },
    {
      label: "1M",
      value: "ONE_MONTH",
    },
    {
      label: "3M",
      value: "THREE_MONTHS",
    },
    {
      label: "1Y",
      value: "ONE_YEAR",
    },
  ];

  // =========================================================
  // UI
  // =========================================================

  return (
    <DashboardLayout>
      <div className="stock-details-page">

        {/* BACK */}

        <button
          className="back-button"
          onClick={() =>
            navigate("/markets")
          }
        >
          <ArrowLeft size={17} />
          Back to Market
        </button>

        {/* HEADER */}

        <div className="stock-detail-header">
          <div>
            <div className="stock-title-row">

              <div className="large-stock-icon">
                {stock.symbol?.charAt(0)}
              </div>

              <div>
                <p className="eyebrow">
                  STOCK
                </p>

                <h1>
                  {stock.symbol}
                </h1>

                <p className="stock-company">
                  {stock.companyName}
                </p>
              </div>

            </div>
          </div>

          <div className="stock-price-header">

            <strong>
              ₹{formatPrice(
                stock.currentPriceInr ??
                stock.currentPrice
              )}
            </strong>

            <div
              className={
                priceChange >= 0
                  ? "market-change positive"
                  : "market-change negative"
              }
            >

              {priceChange >= 0 ? (
                <ArrowUp size={15} />
              ) : (
                <ArrowDown size={15} />
              )}

              {priceChange >= 0
                ? "+"
                : ""}

              {priceChange.toFixed(2)}%

            </div>
          </div>
        </div>

        {/* MAIN GRID */}

        <div className="stock-detail-grid">

          {/* LEFT */}

          <div className="stock-detail-main">

            {/* =================================================
                CHART
            ================================================= */}

            <section className="dashboard-card stock-chart-card">

              <div className="card-header">

                <div>
                  <h2>
                    Price Overview
                  </h2>

                  <p>
                    Real market price history
                  </p>
                </div>

                <div className="chart-periods">

                  {ranges.map(
                    (range) => (
                      <button
                        key={range.value}
                        type="button"
                        className={
                          selectedRange ===
                          range.value
                            ? "active"
                            : ""
                        }
                        onClick={() =>
                          handleRangeChange(
                            range.value
                          )
                        }
                        disabled={
                          historyLoading
                        }
                      >
                        {range.label}
                      </button>
                    )
                  )}

                </div>
              </div>

              <div className="stock-chart">

                {historyLoading && (
                  <div className="chart-state">

                    <RefreshCw
                      size={24}
                      className="refresh-spinning"
                    />

                    <span>
                      Loading price history...
                    </span>

                  </div>
                )}

                {!historyLoading &&
                  historyError && (
                    <div className="chart-state">

                      <TrendingDown
                        size={24}
                      />

                      <span>
                        {historyError}
                      </span>

                    </div>
                  )}

                {!historyLoading &&
                  !historyError &&
                  chartPoints.length === 0 && (
                    <div className="chart-state">

                      <span>
                        No historical data
                        available for this period.
                      </span>

                    </div>
                  )}

                {!historyLoading &&
                  !historyError &&
                  chartPoints.length > 0 && (
                    <>

                      <div className="stock-recharts-wrapper">

                        <ResponsiveContainer
                          width="100%"
                          height={360}
                        >

                          <LineChart
                            data={chartPoints}
                            margin={{
                              top: 20,
                              right: 20,
                              left: 5,
                              bottom: 5,
                            }}
                          >

                            <CartesianGrid
                              horizontal={true}
                              vertical={false}
                              stroke="#e5eaee"
                            />

                            <XAxis
                              dataKey="time"
                              tickFormatter={
                                formatChartDate
                              }
                              axisLine={false}
                              tickLine={false}
                              tick={{
                                fontSize: 12,
                                fill: "#718096",
                              }}
                              minTickGap={40}
                            />

                            <YAxis
                              domain={[
                                "auto",
                                "auto",
                              ]}
                              axisLine={false}
                              tickLine={false}
                              tick={{
                                fontSize: 12,
                                fill: "#718096",
                              }}
                              tickFormatter={(
                                value
                              ) =>
                                `₹${Number(
                                  value
                                ).toFixed(0)}`
                              }
                              width={65}
                            />

                            <Tooltip
                              cursor={{
                                stroke:
                                  "#94a3b8",
                                strokeWidth: 1,
                                strokeDasharray:
                                  "5 5",
                              }}
                              content={
                                <StockChartTooltip />
                              }
                              isAnimationActive={
                                false
                              }
                              wrapperStyle={{
                                outline:
                                  "none",
                                zIndex: 100,
                              }}
                            />

                            <Line
                              type="monotone"
                              dataKey="price"
                              stroke={
                                chartIsPositive
                                  ? "#008f7a"
                                  : "#dc3545"
                              }
                              strokeWidth={3}
                              dot={{
                                r: 3,
                                strokeWidth: 2,
                                fill: "#ffffff",
                              }}
                              activeDot={{
                                r: 7,
                                strokeWidth: 3,
                                fill: "#ffffff",
                              }}
                              isAnimationActive={
                                false
                              }
                              connectNulls
                            />

                          </LineChart>

                        </ResponsiveContainer>

                      </div>

                      <div className="chart-axis">

                        <span>
                          {formatChartDate(
                            chartPoints[0]
                              ?.time
                          )}
                        </span>

                        <span>
                          {formatChartDate(
                            chartPoints[
                              chartPoints.length -
                                1
                            ]?.time
                          )}
                        </span>

                      </div>

                    </>
                  )}

              </div>

              {!historyLoading &&
                !historyError &&
                chartPoints.length > 0 && (

                  <div className="chart-note">

                    <span>
                      Period change
                    </span>

                    <strong
                      className={
                        chartIsPositive
                          ? "positive"
                          : "negative"
                      }
                    >

                      {chartIsPositive
                        ? "+"
                        : ""}

                      {chartMetrics.change.toFixed(
                        2
                      )}
                      %

                    </strong>

                  </div>

                )}

            </section>

            {/* =================================================
                STOCK INFORMATION
            ================================================= */}

            <section className="dashboard-card">

              <div className="card-header">

                <div>
                  <h2>
                    Stock Information
                  </h2>

                  <p>
                    Market snapshot
                  </p>
                </div>

              </div>

              <div className="stock-info-grid">

                <div>
                  <span>
                    Market Status
                  </span>

                  <strong
                    className={
                      isStockActive
                        ? "status-text active"
                        : "status-text suspended"
                    }
                  >
                    {stock.status}
                  </strong>
                </div>

                <div>
                  <span>
                    Sector
                  </span>

                  <strong>
                    {stock.sector || "—"}
                  </strong>
                </div>

                <div>
                  <span>
                    Current Price
                  </span>

                  <strong>
                    ₹{formatPrice(
                      stock.currentPriceInr ??
                      stock.currentPrice
                    )}
                  </strong>
                </div>

                <div>
                  <span>
                    Previous Close
                  </span>

                  <strong>
                    ₹{formatPrice(
                      Number(stock.previousClose || 0) *
                        Number(stock.exchangeRateToInr || 1)
                    )}
                  </strong>
                </div>

                <div>
                  <span>
                    Price Change
                  </span>

                  <strong
                    className={
                      priceChange >= 0
                        ? "positive"
                        : "negative"
                    }
                  >

                    {priceChange >= 0
                      ? "+"
                      : ""}

                    {priceChange.toFixed(
                      2
                    )}
                    %

                  </strong>
                </div>

                <div>
                  <span>
                    Exchange
                  </span>

                  <strong>
                    {stock.exchange || "—"}
                  </strong>
                </div>

              </div>

            </section>

            {/* =================================================
                AI STOCK INSIGHT
            ================================================= */}

            <section className="dashboard-card stock-ai-section">

              <div className="card-header">

                <div>
                  <h2>
                    ✨ Stockly AI Insight
                  </h2>

                  <p>
                    AI-powered analysis based on
                    Stockly market data.
                  </p>
                </div>

                <button
                  type="button"
                  className="ai-insight-button"
                  onClick={fetchAiInsight}
                  disabled={aiLoading}
                >

                  {aiLoading ? (
                    <>
                      <RefreshCw
                        size={16}
                        className="refresh-spinning"
                      />

                      Analyzing...
                    </>
                  ) : (
                    <>
                      <RefreshCw size={16} />

                      {aiInsight
                        ? "Refresh Insight"
                        : "Generate AI Insight"}

                    </>
                  )}

                </button>

              </div>

              {aiError && (
                <div className="ai-error">
                  {aiError}
                </div>
              )}

              {aiInsight &&
                !aiError && (
                  <div className="ai-insight-content">

                    <div className="ai-disclaimer">

                      <span>
                        AI
                      </span>

                      <p>
                        Educational analysis based
                        on available Stockly data.
                        This is not financial advice
                        or a guaranteed prediction.
                      </p>

                    </div>

                    <div className="ai-response">

                      <ReactMarkdown
                        remarkPlugins={[
                          remarkGfm,
                        ]}
                      >
                        {aiInsight}
                      </ReactMarkdown>

                    </div>

                  </div>
                )}

            </section>

          </div>

          {/* =================================================
              RIGHT TRADE PANEL
          ================================================= */}

          <aside className="trade-panel">

            <div className="trade-panel-header">

              <div>

                <p className="eyebrow">
                  TRADE
                </p>

                <h2>
                  {stock.symbol}
                </h2>

              </div>

              <div className="trade-panel-actions">

                <button
                  type="button"
                  className={
                    isWatchlisted
                      ? "watchlist-button active"
                      : "watchlist-button"
                  }
                  onClick={
                    handleWatchlistToggle
                  }
                  disabled={
                    watchlistLoading
                  }
                  title={
                    isWatchlisted
                      ? "Remove from watchlist"
                      : "Add to watchlist"
                  }
                >

                  <Star
                    size={17}
                    fill={
                      isWatchlisted
                        ? "currentColor"
                        : "none"
                    }
                  />

                  {watchlistLoading
                    ? "Saving..."
                    : isWatchlisted
                    ? "Watching"
                    : "Watchlist"}

                </button>

                <span
                  className={
                    isStockActive
                      ? "trade-status active"
                      : "trade-status suspended"
                  }
                >
                  {stock.status}
                </span>

              </div>

            </div>

            {/* BUY / SELL */}

            <div className="trade-tabs">

              <button
                type="button"
                className={
                  tradeType === "BUY"
                    ? "trade-tab buy active"
                    : "trade-tab buy"
                }
                onClick={() =>
                  handleTradeTypeChange(
                    "BUY"
                  )
                }
              >
                Buy
              </button>

              <button
                type="button"
                className={
                  tradeType === "SELL"
                    ? "trade-tab sell active"
                    : "trade-tab sell"
                }
                onClick={() =>
                  handleTradeTypeChange(
                    "SELL"
                  )
                }
              >
                Sell
              </button>

            </div>

            {/* BALANCE / SHARES */}

            <div className="trade-summary">

              <div>

                <span>
                  {tradeType === "BUY"
                    ? "Available Balance"
                    : "Shares Owned"}
                </span>

                <strong>
                  {tradeType === "BUY"
                    ? `₹${formatBalance(
                        currentBalance
                      )}`
                    : sharesOwned}
                </strong>

              </div>

              <div>

                <span>
                  Current Price
                </span>

                <strong>
                  ₹{formatPrice(
                    stock.currentPrice
                  )}
                </strong>

              </div>

            </div>

            {/* QUANTITY */}

            <div className="trade-field">

              <label htmlFor="trade-quantity">
                Quantity
              </label>

              <div className="quantity-control">

                <button
                  type="button"
                  onClick={
                    decreaseQuantity
                  }
                  disabled={
                    quantity <= 1
                  }
                >
                  <Minus size={16} />
                </button>

                <input
                  id="trade-quantity"
                  name="quantity"
                  type="number"
                  min="1"
                  step="1"
                  value={quantity}
                  onChange={
                    handleQuantityChange
                  }
                />

                <button
                  type="button"
                  onClick={
                    increaseQuantity
                  }
                >
                  <Plus size={16} />
                </button>

              </div>

            </div>

            {/* TOTAL */}

            <div className="trade-total">

              <span>
                {tradeType === "BUY"
                  ? "Estimated Total"
                  : "Estimated Value"}
              </span>

              <strong>
                ₹{formatPrice(
                  estimatedTotal
                )}
              </strong>

            </div>

            {/* ERROR */}

            {tradeError && (
              <div className="trade-error">

                <TrendingDown size={17} />

                <span>
                  {tradeError}
                </span>

              </div>
            )}

            {/* CTA */}

            <button
              type="button"
              className={
                tradeType === "BUY"
                  ? "trade-submit buy"
                  : "trade-submit sell"
              }
              disabled={tradeLoading}
              onClick={
                handleTradeSubmit
              }
            >

              {tradeType === "BUY"
                ? `Buy ${stock.symbol}`
                : `Sell ${stock.symbol}`}

            </button>

            <p className="trade-disclaimer">
              Final price and transaction
              amount are calculated by the
              server.
            </p>

          </aside>

        </div>

        {/* =================================================
            SUCCESS
        ================================================= */}

        {success && (
          <div className="trade-success">

            <div className="success-icon">
              <CheckCircle2 size={22} />
            </div>

            <div>

              <strong>
                Trade completed
              </strong>

              <p>
                You{" "}
                {success.type === "BUY"
                  ? "bought"
                  : "sold"}{" "}
                {quantity} shares of{" "}
                {stock.symbol}.
              </p>

              {success.data
                ?.transactionReference && (
                <span>
                  Transaction ID:{" "}
                  {
                    success.data
                      .transactionReference
                  }
                </span>
              )}

            </div>

            <button
              type="button"
              onClick={() =>
                setSuccess(null)
              }
            >
              <X size={17} />
            </button>

          </div>
        )}

      </div>

      {/* =====================================================
          CONFIRMATION MODAL
      ===================================================== */}

      {showConfirmation && (
        <div
          className="trade-modal-overlay"
          onClick={() =>
            !tradeLoading &&
            setShowConfirmation(false)
          }
        >

          <div
            className="trade-modal"
            onClick={(event) =>
              event.stopPropagation()
            }
          >

            <div className="trade-modal-header">

              <div>

                <p className="eyebrow">
                  CONFIRM TRADE
                </p>

                <h2>
                  Confirm{" "}
                  {tradeType === "BUY"
                    ? "Purchase"
                    : "Sale"}
                </h2>

              </div>

              {!tradeLoading && (
                <button
                  type="button"
                  onClick={() =>
                    setShowConfirmation(
                      false
                    )
                  }
                >
                  <X size={19} />
                </button>
              )}

            </div>

            <div className="confirmation-stock">

              <div className="confirmation-icon">
                {stock.symbol?.charAt(0)}
              </div>

              <div>

                <strong>
                  {stock.symbol}
                </strong>

                <span>
                  {stock.companyName}
                </span>

              </div>

            </div>

            <div className="confirmation-details">

              <div>

                <span>
                  Quantity
                </span>

                <strong>
                  {quantity}
                </strong>

              </div>

              <div>

                <span>
                  Price per share
                </span>

                <strong>
                  ₹{formatPrice(
                    stock.currentPrice
                  )}
                </strong>

              </div>

              <div>

                <span>
                  Estimated total
                </span>

                <strong>
                  ₹{formatPrice(
                    estimatedTotal
                  )}
                </strong>

              </div>

              <div>

                <span>
                  {tradeType === "BUY"
                    ? "Available balance"
                    : "Shares owned"}
                </span>

                <strong>
                  {tradeType === "BUY"
                    ? `₹${formatBalance(
                        currentBalance
                      )}`
                    : sharesOwned}
                </strong>

              </div>

            </div>

            <div className="confirmation-warning">

              <span>
                Final execution price and
                amount are determined by the
                server.
              </span>

            </div>

            <div className="modal-actions">

              <button
                type="button"
                className="secondary-button"
                disabled={tradeLoading}
                onClick={() =>
                  setShowConfirmation(
                    false
                  )
                }
              >
                Cancel
              </button>

              <button
                type="button"
                className={
                  tradeType === "BUY"
                    ? "confirm-button buy"
                    : "confirm-button sell"
                }
                disabled={tradeLoading}
                onClick={
                  executeTrade
                }
              >

                {tradeLoading ? (
                  <>
                    <RefreshCw
                      size={16}
                      className="refresh-spinning"
                    />

                    Processing...
                  </>
                ) : (
                  `Confirm ${
                    tradeType === "BUY"
                      ? "Buy"
                      : "Sell"
                  }`
                )}

              </button>

            </div>

          </div>

        </div>
      )}

    </DashboardLayout>
  );
}

export default StockDetails;