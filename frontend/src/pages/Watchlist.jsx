import { useEffect, useState } from "react";
import {
  ArrowDown,
  ArrowUp,
  RefreshCw,
  Star,
  Trash2,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";
import { useNavigate } from "react-router-dom";

function Watchlist() {
  const navigate = useNavigate();

  const [stocks, setStocks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const fetchWatchlist = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/watchlist");

      setStocks(response.data || []);
    } catch (error) {
      console.error(
        "Failed to fetch watchlist:",
        error
      );

      setError(
        error.response?.data?.message ||
          "Unable to load your watchlist."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWatchlist();
  }, []);

  const removeFromWatchlist = async (
    symbol
  ) => {
    try {
      await api.delete(
        `/watchlist/${symbol}`
      );

      setStocks((current) =>
        current.filter(
          (stock) =>
            stock.symbol !== symbol
        )
      );
    } catch (error) {
      console.error(
        "Failed to remove stock:",
        error
      );

      setError(
        error.response?.data?.message ||
          "Unable to remove stock from watchlist."
      );
    }
  };

  const formatPrice = (price) => {
    return Number(price || 0).toLocaleString(
      "en-IN",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    );
  };

  return (
    <DashboardLayout>

      <div className="watchlist-page">

        {/* HEADER */}

        <div className="page-header">

          <div>

            <p className="eyebrow">
              WATCHLIST
            </p>

            <h1>
              Your Watchlist
            </h1>

            <p className="page-description">
              Keep an eye on stocks you're
              interested in.
            </p>

          </div>

          <button
            className="refresh-button"
            onClick={fetchWatchlist}
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


        {/* WATCHLIST CARD */}

        <section className="watchlist-card">

          <div className="watchlist-card-header">

            <div>

              <div className="watchlist-title">

                <Star
                  size={20}
                />

                <h2>
                  Followed Stocks
                </h2>

              </div>

              <p>
                {stocks.length}{" "}
                {stocks.length === 1
                  ? "stock"
                  : "stocks"}{" "}
                in your watchlist
              </p>

            </div>

          </div>


          {/* LOADING */}

          {loading && (

            <div className="watchlist-state">

              <RefreshCw
                size={28}
                className="refresh-spinning"
              />

              <p>
                Loading watchlist...
              </p>

            </div>

          )}


          {/* ERROR */}

          {!loading && error && (

            <div className="watchlist-state">

              <Star size={30} />

              <h3>
                Unable to load watchlist
              </h3>

              <p>
                {error}
              </p>

              <button
                className="watchlist-retry"
                onClick={fetchWatchlist}
              >
                Try Again
              </button>

            </div>

          )}


          {/* EMPTY */}

          {!loading &&
            !error &&
            stocks.length === 0 && (

              <div className="watchlist-state">

                <Star size={36} />

                <h3>
                  Your watchlist is empty
                </h3>

                <p>
                  Add stocks from their
                  details page to follow
                  them here.
                </p>

                <button
                  className="watchlist-retry"
                  onClick={() =>
                    navigate("/markets")
                  }
                >
                  Explore Markets
                </button>

              </div>

            )}


          {/* STOCK LIST */}

          {!loading &&
            !error &&
            stocks.length > 0 && (

              <div className="watchlist-list">

                {stocks.map((stock) => (

                  <div
                    className="watchlist-item"
                    key={stock.symbol}
                    onClick={() =>
                      navigate(
                        `/stocks/${stock.symbol}`
                      )
                    }
                  >

                    {/* ICON */}

                    <div className="watchlist-stock-icon">
                      {stock.symbol
                        ?.charAt(0)}
                    </div>


                    {/* STOCK INFO */}

                    <div className="watchlist-stock-info">

                      <strong>
                        {stock.symbol}
                      </strong>

                      <span>
                        {stock.companyName}
                      </span>

                    </div>


                    {/* SECTOR */}

                    <div className="watchlist-sector">

                      <span>
                        Sector
                      </span>

                      <strong>
                        {stock.sector ||
                          "—"}
                      </strong>

                    </div>


                    {/* EXCHANGE */}

                    <div className="watchlist-exchange">

                      <span>
                        Exchange
                      </span>

                      <strong>
                        {stock.exchange ||
                          "—"}
                      </strong>

                    </div>


                    {/* PRICE */}

                    <div className="watchlist-price">

                      <span>
                        Current Price
                      </span>

                      <strong>
                        ₹
                        {formatPrice(
                          stock.currentPriceInr ??
                          stock.currentPrice
                        )}
                      </strong>

                    </div>


                    {/* REMOVE */}

                    <button
                      type="button"
                      className="watchlist-remove"
                      title="Remove from watchlist"
                      onClick={(event) => {

                        event.stopPropagation();

                        removeFromWatchlist(
                          stock.symbol
                        );

                      }}
                    >

                      <Trash2
                        size={17}
                      />

                    </button>

                  </div>

                ))}

              </div>

            )}

        </section>

      </div>

    </DashboardLayout>
  );
}

export default Watchlist;