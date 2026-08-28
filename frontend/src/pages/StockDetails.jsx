import { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";

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

function StockDetails() {
  const { symbol } = useParams();
  const navigate = useNavigate();

  const [stock, setStock] = useState(null);
  const [user, setUser] = useState(null);
  const [holding, setHolding] = useState(null);

  const [tradeType, setTradeType] = useState("BUY");
  const [quantity, setQuantity] = useState(1);

  const [loading, setLoading] = useState(true);
  const [tradeLoading, setTradeLoading] = useState(false);

  const [error, setError] = useState("");
  const [tradeError, setTradeError] = useState("");

  const [showConfirmation, setShowConfirmation] = useState(false);
  const [success, setSuccess] = useState(null);

  // WATCHLIST
  const [isWatchlisted, setIsWatchlisted] = useState(false);
  const [watchlistLoading, setWatchlistLoading] = useState(false);

  // =========================
  // FETCH STOCK
  // =========================

  const fetchStock = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get(`/stocks/${symbol}`);

      setStock(response.data);
    } catch (error) {
      console.error("Failed to fetch stock:", error);

      if (error.response?.status === 404) {
        setError("The stock you're looking for doesn't exist.");
      } else {
        setError("Unable to load stock information.");
      }
    } finally {
      setLoading(false);
    }
  };

  // =========================
  // FETCH USER
  // =========================

  const fetchUser = async () => {
    try {
      const response = await api.get("/users/me");

      setUser(response.data);
    } catch (error) {
      console.error("Failed to fetch user:", error);
    }
  };

  // =========================
  // FETCH HOLDING
  // =========================

  const fetchHolding = async () => {
    try {
      const response = await api.get("/portfolio/holdings");

      const holdings = response.data?.holdings || [];

      const currentHolding = holdings.find(
        (item) =>
          item.symbol?.trim().toUpperCase() ===
          symbol?.trim().toUpperCase()
      );

      if (currentHolding) {
        setHolding(currentHolding);
      } else {
        setHolding(null);
      }
    } catch (error) {
      console.error("Failed to fetch holdings:", error);
      setHolding(null);
    }
  };

  // =========================
  // FETCH WATCHLIST
  // =========================

  const fetchWatchlistStatus = async () => {
    try {
      const response = await api.get("/watchlist");

      const watchlist = response.data || [];

      const exists = watchlist.some(
        (item) =>
          item.symbol?.trim().toUpperCase() ===
          symbol?.trim().toUpperCase()
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

  // =========================
  // INITIAL LOAD
  // =========================

  useEffect(() => {
    fetchStock();
    fetchUser();
    fetchHolding();
    fetchWatchlistStatus();
  }, [symbol]);

  // =========================
  // WATCHLIST TOGGLE
  // =========================

  const handleWatchlistToggle = async () => {
    if (!stock || watchlistLoading) {
      return;
    }

    try {
      setWatchlistLoading(true);

      const stockSymbol = stock.symbol;

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

  // =========================
  // PRICE CHANGE
  // =========================

  const priceChange = useMemo(() => {
    if (!stock) {
      return 0;
    }

    const current = Number(
      stock.currentPrice || 0
    );

    const previous = Number(
      stock.previousPrice || 0
    );

    if (!previous) {
      return 0;
    }

    return (
      ((current - previous) / previous) *
      100
    );
  }, [stock]);

  // =========================
  // ESTIMATED TOTAL
  // =========================

  const estimatedTotal =
    Number(stock?.currentPrice || 0) *
    Number(quantity || 0);

  // =========================
  // FORMATTING
  // =========================

  const formatPrice = (value) => {
    return Number(value || 0).toLocaleString(
      "en-IN",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    );
  };

  const formatBalance = (value) => {
    return Number(value || 0).toLocaleString(
      "en-IN",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    );
  };

  // =========================
  // QUANTITY CONTROLS
  // =========================

  const decreaseQuantity = () => {
    setQuantity((current) =>
      Math.max(1, current - 1)
    );
  };

  const increaseQuantity = () => {
    setQuantity((current) => current + 1);
  };

  const handleQuantityChange = (event) => {
    const value = parseInt(
      event.target.value,
      10
    );

    if (Number.isNaN(value)) {
      setQuantity(1);
      return;
    }

    setQuantity(Math.max(1, value));
  };

  // =========================
  // TRADE TYPE
  // =========================

  const handleTradeTypeChange = async (
    type
  ) => {
    setTradeType(type);
    setTradeError("");
    setSuccess(null);
    setQuantity(1);

    if (type === "SELL") {
      await fetchHolding();
    }
  };

  // =========================
  // OPEN CONFIRMATION
  // =========================

  const handleTradeSubmit = () => {
    setTradeError("");
    setSuccess(null);

    if (!stock) {
      return;
    }

    const stockStatus = String(
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
        Number(holding?.quantity || 0)
    ) {
      setTradeError(
        "You don't own enough shares to sell."
      );
      return;
    }

    setShowConfirmation(true);
  };

  // =========================
  // EXECUTE TRADE
  // =========================

  const executeTrade = async () => {
    try {
      setTradeLoading(true);
      setTradeError("");

      const endpoint =
        tradeType === "BUY"
          ? "/trades/buy"
          : "/trades/sell";

      const response = await api.post(
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

      if (error.response?.data?.message) {
        message =
          error.response.data.message;
      } else if (error.response?.data?.error) {
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

  // =========================
  // LOADING
  // =========================

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

  // =========================
  // ERROR
  // =========================

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

  // =========================
  // STOCK STATUS
  // =========================

  const isPositive =
    priceChange >= 0;

  const stockStatus = String(
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

  // =========================
  // UI
  // =========================

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
                stock.currentPrice
              )}
            </strong>

            <div
              className={
                isPositive
                  ? "market-change positive"
                  : "market-change negative"
              }
            >

              {isPositive ? (
                <ArrowUp size={15} />
              ) : (
                <ArrowDown size={15} />
              )}

              {isPositive ? "+" : ""}
              {priceChange.toFixed(2)}%

            </div>

          </div>

        </div>


        {/* MAIN GRID */}

        <div className="stock-detail-grid">

          {/* LEFT */}

          <div className="stock-detail-main">

            {/* CHART */}

            <section className="dashboard-card stock-chart-card">

              <div className="card-header">

                <div>

                  <h2>
                    Price Overview
                  </h2>

                  <p>
                    Current simulated market price
                  </p>

                </div>

                <div className="chart-periods">

                  <button className="active">
                    1D
                  </button>

                  <button>
                    1W
                  </button>

                  <button>
                    1M
                  </button>

                  <button>
                    3M
                  </button>

                  <button>
                    1Y
                  </button>

                </div>

              </div>


              <div className="stock-chart">

                <div className="chart-grid-lines">

                  <span></span>
                  <span></span>
                  <span></span>
                  <span></span>

                </div>

                <div
                  className={
                    isPositive
                      ? "stock-chart-line positive-line"
                      : "stock-chart-line negative-line"
                  }
                >
                  <span></span>
                </div>

                <div className="chart-current-price">
                  ₹{formatPrice(
                    stock.currentPrice
                  )}
                </div>

                <div className="chart-axis">

                  <span>
                    Previous
                  </span>

                  <span>
                    Current
                  </span>

                </div>

              </div>


              <div className="chart-note">

                <span>
                  Previous price
                </span>

                <strong>
                  ₹{formatPrice(
                    stock.previousPrice
                  )}
                </strong>

              </div>

            </section>


            {/* STOCK INFORMATION */}

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
                      stock.currentPrice
                    )}
                  </strong>

                </div>


                <div>

                  <span>
                    Previous Price
                  </span>

                  <strong>
                    ₹{formatPrice(
                      stock.previousPrice
                    )}
                  </strong>

                </div>


                <div>

                  <span>
                    Price Change
                  </span>

                  <strong
                    className={
                      isPositive
                        ? "positive"
                        : "negative"
                    }
                  >
                    {isPositive ? "+" : ""}
                    {priceChange.toFixed(2)}%
                  </strong>

                </div>


                <div>

                  <span>
                    Last Updated
                  </span>

                  <strong>
                    {stock.lastUpdated
                      ? new Date(
                          stock.lastUpdated
                        ).toLocaleString(
                          "en-IN",
                          {
                            day: "2-digit",
                            month: "short",
                            hour: "2-digit",
                            minute: "2-digit",
                          }
                        )
                      : "—"}
                  </strong>

                </div>

              </div>

            </section>

          </div>


          {/* RIGHT TRADE PANEL */}

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


        {/* SUCCESS */}

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


      {/* CONFIRMATION MODAL */}

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