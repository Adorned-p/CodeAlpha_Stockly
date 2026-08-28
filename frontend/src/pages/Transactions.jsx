import { useEffect, useMemo, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  History,
  RefreshCw,
  Search,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";

function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/trades");

      setTransactions(response.data || []);
    } catch (error) {
      console.error("Failed to fetch transactions:", error);

      setError(
        error.response?.data?.message ||
          "Unable to load your transactions."
      );
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, []);

  const formatMoney = (value) => {
    return Number(value || 0).toLocaleString("en-IN", {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    });
  };

  const formatDate = (date) => {
    if (!date) return "—";

    return new Date(date).toLocaleString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const filteredTransactions = useMemo(() => {
    return transactions.filter((transaction) => {
      const matchesType =
        filter === "ALL" ||
        transaction.type === filter;

      const searchValue = search.trim().toLowerCase();

      const matchesSearch =
        !searchValue ||
        transaction.symbol
          ?.toLowerCase()
          .includes(searchValue) ||
        transaction.companyName
          ?.toLowerCase()
          .includes(searchValue);

      return matchesType && matchesSearch;
    });
  }, [transactions, search, filter]);

  const totalBuys = transactions.filter(
    (transaction) => transaction.type === "BUY"
  ).length;

  const totalSells = transactions.filter(
    (transaction) => transaction.type === "SELL"
  ).length;

  return (
    <DashboardLayout>
      <div className="transactions-page">

        {/* HEADER */}

        <div className="page-header">
          <div>
            <p className="eyebrow">ACTIVITY</p>

            <h1>Transactions</h1>

            <p className="page-description">
              Review your completed stock trades.
            </p>
          </div>

          <button
            className="refresh-button"
            onClick={fetchTransactions}
            disabled={loading}
          >
            <RefreshCw
              size={16}
              className={
                loading ? "refresh-spinning" : ""
              }
            />

            Refresh
          </button>
        </div>


        {/* SUMMARY CARDS */}

        <div className="transaction-summary">

          <div className="transaction-summary-card">
            <div className="transaction-summary-icon">
              <History size={20} />
            </div>

            <div>
              <span>Total Trades</span>
              <strong>{transactions.length}</strong>
            </div>
          </div>


          <div className="transaction-summary-card">
            <div className="transaction-summary-icon buy-icon">
              <ArrowUpRight size={20} />
            </div>

            <div>
              <span>Purchases</span>
              <strong>{totalBuys}</strong>
            </div>
          </div>


          <div className="transaction-summary-card">
            <div className="transaction-summary-icon sell-icon">
              <ArrowDownRight size={20} />
            </div>

            <div>
              <span>Sales</span>
              <strong>{totalSells}</strong>
            </div>
          </div>

        </div>


        {/* TRANSACTION CARD */}

        <section className="transactions-card">

          <div className="transactions-card-header">

            <div>
              <h2>Transaction History</h2>

              <p>
                Your latest trading activity
              </p>
            </div>

          </div>


          {/* FILTER BAR */}

          <div className="transactions-toolbar">

            <div className="transaction-search">

              <Search size={17} />

              <input
                type="text"
                placeholder="Search stocks..."
                value={search}
                onChange={(event) =>
                  setSearch(event.target.value)
                }
              />

            </div>


            <div className="transaction-tabs">

              <button
                className={
                  filter === "ALL"
                    ? "transaction-tab active"
                    : "transaction-tab"
                }
                onClick={() => setFilter("ALL")}
              >
                All
              </button>

              <button
                className={
                  filter === "BUY"
                    ? "transaction-tab active"
                    : "transaction-tab"
                }
                onClick={() => setFilter("BUY")}
              >
                Buys
              </button>

              <button
                className={
                  filter === "SELL"
                    ? "transaction-tab active"
                    : "transaction-tab"
                }
                onClick={() => setFilter("SELL")}
              >
                Sells
              </button>

            </div>

          </div>


          {/* LOADING */}

          {loading && (
            <div className="transaction-empty-state">

              <RefreshCw
                size={28}
                className="refresh-spinning"
              />

              <p>Loading transactions...</p>

            </div>
          )}


          {/* ERROR */}

          {!loading && error && (
            <div className="transaction-empty-state">

              <History size={30} />

              <h3>Unable to load transactions</h3>

              <p>{error}</p>

              <button
                className="transaction-retry-button"
                onClick={fetchTransactions}
              >
                Try Again
              </button>

            </div>
          )}


          {/* EMPTY */}

          {!loading &&
            !error &&
            filteredTransactions.length === 0 && (
              <div className="transaction-empty-state">

                <History size={32} />

                <h3>No transactions found</h3>

                <p>
                  Your completed trades will appear here.
                </p>

              </div>
            )}


          {/* TRANSACTIONS */}

          {!loading &&
            !error &&
            filteredTransactions.length > 0 && (

              <div className="transactions-list">

                {filteredTransactions.map(
                  (transaction) => {

                    const isBuy =
                      transaction.type === "BUY";

                    return (
                      <div
                        className="transaction-item"
                        key={transaction.id}
                      >

                        {/* TYPE ICON */}

                        <div
                          className={
                            isBuy
                              ? "transaction-type-icon buy"
                              : "transaction-type-icon sell"
                          }
                        >
                          {isBuy ? (
                            <ArrowUpRight size={19} />
                          ) : (
                            <ArrowDownRight size={19} />
                          )}
                        </div>


                        {/* STOCK */}

                        <div className="transaction-stock-info">

                          <strong>
                            {transaction.symbol}
                          </strong>

                          <span>
                            {transaction.companyName}
                          </span>

                        </div>


                        {/* TYPE */}

                        <div className="transaction-action">

                          <span
                            className={
                              isBuy
                                ? "transaction-badge buy"
                                : "transaction-badge sell"
                            }
                          >
                            {transaction.type}
                          </span>

                        </div>


                        {/* QUANTITY */}

                        <div className="transaction-detail">

                          <span>Quantity</span>

                          <strong>
                            {transaction.quantity}
                          </strong>

                        </div>


                        {/* PRICE */}

                        <div className="transaction-detail">

                          <span>Price</span>

                          <strong>
                            ₹{formatMoney(
                              transaction.price
                            )}
                          </strong>

                        </div>


                        {/* TOTAL */}

                        <div className="transaction-total">

                          <span>Total</span>

                          <strong>
                            ₹{formatMoney(
                              transaction.totalAmount
                            )}
                          </strong>

                        </div>


                        {/* DATE */}

                        <div className="transaction-date">

                          <span>Executed</span>

                          <strong>
                            {formatDate(
                              transaction.executedAt
                            )}
                          </strong>

                        </div>

                      </div>
                    );
                  }
                )}

              </div>
            )}

        </section>

      </div>
    </DashboardLayout>
  );
}

export default Transactions;