import { useEffect, useMemo, useState } from "react";
import {
  ArrowDownRight,
  ArrowUpRight,
  History,
  RefreshCw,
  Search,
  Clock3,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";

function Transactions() {
  const [transactions, setTransactions] = useState([]);
  const [orders, setOrders] = useState([]);

  const [loading, setLoading] = useState(true);
  const [ordersLoading, setOrdersLoading] = useState(true);

  const [error, setError] = useState("");
  const [ordersError, setOrdersError] = useState("");

  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");

  const [cancellingOrderId, setCancellingOrderId] =
    useState(null);

  // =========================================================
  // FETCH COMPLETED TRADES
  // =========================================================

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      setError("");

      const response = await api.get("/trades");

      setTransactions(response.data || []);

    } catch (error) {
      console.error(
        "Failed to fetch transactions:",
        error
      );

      setError(
        error.response?.data?.message ||
          "Unable to load your transactions."
      );

    } finally {
      setLoading(false);
    }
  };

  // =========================================================
  // FETCH ORDERS
  // =========================================================

  const fetchOrders = async () => {
    try {
      setOrdersLoading(true);
      setOrdersError("");

      const response = await api.get("/orders");

      setOrders(response.data || []);

    } catch (error) {
      console.error(
        "Failed to fetch orders:",
        error
      );

      setOrdersError(
        error.response?.data?.message ||
          "Unable to load your orders."
      );

    } finally {
      setOrdersLoading(false);
    }
  };

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    fetchTransactions();
    fetchOrders();
  }, []);

  // =========================================================
  // CANCEL ORDER
  // =========================================================

  const handleCancelOrder = async (orderId) => {
    try {
      setCancellingOrderId(orderId);
      setOrdersError("");

      await api.delete(
        `/orders/${orderId}`
      );

      await fetchOrders();

    } catch (error) {
      console.error(
        "Failed to cancel order:",
        error
      );

      setOrdersError(
        error.response?.data?.message ||
          "Unable to cancel the order."
      );

    } finally {
      setCancellingOrderId(null);
    }
  };

  // =========================================================
  // FORMAT MONEY
  // =========================================================

  const formatMoney = (value) => {
    return Number(value || 0).toLocaleString(
      "en-IN",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    );
  };

  // =========================================================
  // FORMAT DATE
  // =========================================================

  const formatDate = (date) => {
    if (!date) return "—";

    return new Date(date).toLocaleString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit",
      }
    );
  };

  // =========================================================
  // OPEN ORDERS
  // =========================================================

  const openOrders = useMemo(() => {
    return orders.filter(
      (order) =>
        order.status === "OPEN" ||
        order.status === "PARTIALLY_FILLED"
    );
  }, [orders]);

  // =========================================================
  // ORDER HISTORY
  // =========================================================

  const orderHistory = useMemo(() => {
    return orders.filter(
      (order) =>
        order.status !== "OPEN" &&
        order.status !== "PARTIALLY_FILLED"
    );
  }, [orders]);

  // =========================================================
  // FILTER TRANSACTIONS
  // =========================================================

  const filteredTransactions = useMemo(() => {
    return transactions.filter(
      (transaction) => {

        const matchesType =
          filter === "ALL" ||
          transaction.type === filter;

        const searchValue =
          search.trim().toLowerCase();

        const matchesSearch =
          !searchValue ||
          transaction.symbol
            ?.toLowerCase()
            .includes(searchValue) ||
          transaction.companyName
            ?.toLowerCase()
            .includes(searchValue) ||
          transaction.exchange
            ?.toLowerCase()
            .includes(searchValue);

        return (
          matchesType &&
          matchesSearch
        );
      }
    );
  }, [
    transactions,
    search,
    filter,
  ]);

  // =========================================================
  // SUMMARY
  // =========================================================

  const totalBuys =
    transactions.filter(
      (transaction) =>
        transaction.type === "BUY"
    ).length;

  const totalSells =
    transactions.filter(
      (transaction) =>
        transaction.type === "SELL"
    ).length;

  // =========================================================
  // ORDER PRICE
  // =========================================================

  const getOrderPrice = (order) => {

    if (
      order.averageFillPrice != null
    ) {
      return order.averageFillPrice;
    }

    if (
      order.type === "STOP" &&
      order.stopPrice != null
    ) {
      return order.stopPrice;
    }

    return order.limitPrice;
  };

  // =========================================================
  // ORDER TYPE LABEL
  // =========================================================

  const formatOrderType = (type) => {
    if (!type) return "—";

    return type
      .replaceAll("_", " ")
      .toUpperCase();
  };

  return (
    <DashboardLayout>

      <div className="transactions-page">

        {/* =====================================================
            HEADER
        ===================================================== */}

        <div className="page-header">

          <div>

            <p className="eyebrow">
              ACTIVITY
            </p>

            <h1>
              Transactions
            </h1>

            <p className="page-description">
              Review your orders and completed trades.
            </p>

          </div>

          <button
            className="refresh-button"
            onClick={() => {
              fetchTransactions();
              fetchOrders();
            }}
            disabled={
              loading ||
              ordersLoading
            }
          >

            <RefreshCw
              size={16}
              className={
                loading ||
                ordersLoading
                  ? "refresh-spinning"
                  : ""
              }
            />

            Refresh

          </button>

        </div>


        {/* =====================================================
            SUMMARY CARDS
        ===================================================== */}

        <div className="transaction-summary">

          <div className="transaction-summary-card">

            <div className="transaction-summary-icon">
              <History size={20} />
            </div>

            <div>

              <span>
                Total Trades
              </span>

              <strong>
                {transactions.length}
              </strong>

            </div>

          </div>


          <div className="transaction-summary-card">

            <div className="transaction-summary-icon buy-icon">
              <ArrowUpRight size={20} />
            </div>

            <div>

              <span>
                Purchases
              </span>

              <strong>
                {totalBuys}
              </strong>

            </div>

          </div>


          <div className="transaction-summary-card">

            <div className="transaction-summary-icon sell-icon">
              <ArrowDownRight size={20} />
            </div>

            <div>

              <span>
                Sales
              </span>

              <strong>
                {totalSells}
              </strong>

            </div>

          </div>

        </div>


        {/* =====================================================
            OPEN ORDERS
        ===================================================== */}

        <section className="transactions-card">

          <div className="transactions-card-header">

            <div>

              <h2>
                Open Orders
              </h2>

              <p>
                Manage your pending limit and stop orders.
              </p>

            </div>

          </div>


          {ordersLoading && (

            <div className="transaction-empty-state">

              <RefreshCw
                size={28}
                className="refresh-spinning"
              />

              <p>
                Loading orders...
              </p>

            </div>

          )}


          {!ordersLoading &&
            ordersError && (

              <div className="transaction-empty-state">

                <History size={30} />

                <h3>
                  Unable to load orders
                </h3>

                <p>
                  {ordersError}
                </p>

                <button
                  className="transaction-retry-button"
                  onClick={fetchOrders}
                >
                  Try Again
                </button>

              </div>

          )}


          {!ordersLoading &&
            !ordersError &&
            openOrders.length === 0 && (

              <div className="transaction-empty-state">

                <Clock3 size={32} />

                <h3>
                  No open orders
                </h3>

                <p>
                  Your pending orders will appear here.
                </p>

              </div>

          )}


          {!ordersLoading &&
            !ordersError &&
            openOrders.length > 0 && (

              <div className="transactions-list">

                {openOrders.map(
                  (order) => {

                    const isBuy =
                      order.side === "BUY";

                    return (

                      <div
                        className="transaction-item"
                        key={order.id}
                      >

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


                        <div className="transaction-stock-info">

                          <strong>
                            {order.symbol}
                          </strong>

                          <span>
                            {order.instrumentName}
                          </span>

                        </div>


                        <div className="transaction-action">

                          <span
                            className={
                              isBuy
                                ? "transaction-badge buy"
                                : "transaction-badge sell"
                            }
                          >
                            {order.side}
                          </span>

                        </div>


                        <div className="transaction-detail">

                          <span>
                            Order Type
                          </span>

                          <strong>
                            {formatOrderType(
                              order.type
                            )}
                          </strong>

                        </div>


                        <div className="transaction-detail">

                          <span>
                            Quantity
                          </span>

                          <strong>
                            {order.filledQuantity || 0}
                            {" / "}
                            {order.quantity}
                          </strong>

                        </div>


                        <div className="transaction-detail">

                          <span>
                            Price
                          </span>

                          <strong>
                            ₹{formatMoney(
                              getOrderPrice(order)
                            )}
                          </strong>

                        </div>


                        <div className="transaction-detail">

                          <span>
                            Status
                          </span>

                          <strong>
                            {formatOrderType(
                              order.status
                            )}
                          </strong>

                        </div>


                        <div className="transaction-action">

                          <button
                            type="button"
                            className="secondary-button"
                            disabled={
                              cancellingOrderId ===
                              order.id
                            }
                            onClick={() =>
                              handleCancelOrder(
                                order.id
                              )
                            }
                          >
                            {cancellingOrderId ===
                            order.id
                              ? "Cancelling..."
                              : "Cancel"}
                          </button>

                        </div>

                      </div>

                    );
                  }
                )}

              </div>

          )}

        </section>


        {/* =====================================================
            ORDER HISTORY
        ===================================================== */}

        <section className="transactions-card">

          <div className="transactions-card-header">

            <div>

              <h2>
                Order History
              </h2>

              <p>
                Your completed and cancelled orders.
              </p>

            </div>

          </div>


          {orderHistory.length === 0 ? (

            <div className="transaction-empty-state">

              <History size={32} />

              <h3>
                No order history
              </h3>

              <p>
                Completed orders will appear here.
              </p>

            </div>

          ) : (

            <div className="transactions-list">

              {orderHistory.map(
                (order) => {

                  const isBuy =
                    order.side === "BUY";

                  return (

                    <div
                      className="transaction-item"
                      key={order.id}
                    >

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


                      <div className="transaction-stock-info">

                        <strong>
                          {order.symbol}
                        </strong>

                        <span>
                          {order.instrumentName}
                        </span>

                      </div>


                      <div className="transaction-action">

                        <span
                          className={
                            isBuy
                              ? "transaction-badge buy"
                              : "transaction-badge sell"
                          }
                        >
                          {order.side}
                        </span>

                      </div>


                      <div className="transaction-detail">

                        <span>
                          Order Type
                        </span>

                        <strong>
                          {formatOrderType(
                            order.type
                          )}
                        </strong>

                      </div>


                      <div className="transaction-detail">

                        <span>
                          Quantity
                        </span>

                        <strong>
                          {order.filledQuantity || 0}
                          {" / "}
                          {order.quantity}
                        </strong>

                      </div>


                      <div className="transaction-detail">

                        <span>
                          Fill Price
                        </span>

                        <strong>
                          {order.averageFillPrice != null
                            ? `₹${formatMoney(
                                order.averageFillPrice
                              )}`
                            : "—"}
                        </strong>

                      </div>


                      <div className="transaction-detail">

                        <span>
                          Status
                        </span>

                        <strong>
                          {formatOrderType(
                            order.status
                          )}
                        </strong>

                      </div>


                      <div className="transaction-date">

                        <span>
                          Updated
                        </span>

                        <strong>
                          {formatDate(
                            order.updatedAt
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


        {/* =====================================================
            EXISTING TRANSACTION HISTORY
        ===================================================== */}

        <section className="transactions-card">

          <div className="transactions-card-header">

            <div>

              <h2>
                Transaction History
              </h2>

              <p>
                Your completed trading activity.
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
                  setSearch(
                    event.target.value
                  )
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
                onClick={() =>
                  setFilter("ALL")
                }
              >
                All
              </button>

              <button
                className={
                  filter === "BUY"
                    ? "transaction-tab active"
                    : "transaction-tab"
                }
                onClick={() =>
                  setFilter("BUY")
                }
              >
                Buys
              </button>

              <button
                className={
                  filter === "SELL"
                    ? "transaction-tab active"
                    : "transaction-tab"
                }
                onClick={() =>
                  setFilter("SELL")
                }
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

              <p>
                Loading transactions...
              </p>

            </div>

          )}


          {/* ERROR */}

          {!loading &&
            error && (

              <div className="transaction-empty-state">

                <History size={30} />

                <h3>
                  Unable to load transactions
                </h3>

                <p>
                  {error}
                </p>

                <button
                  className="transaction-retry-button"
                  onClick={
                    fetchTransactions
                  }
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

                <h3>
                  No transactions found
                </h3>

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


                        <div className="transaction-stock-info">

                          <strong>
                            {transaction.symbol}
                          </strong>

                          <span>
                            {transaction.companyName}
                          </span>

                          <small>
                            {transaction.exchange}
                          </small>

                        </div>


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


                        <div className="transaction-detail">

                          <span>
                            Quantity
                          </span>

                          <strong>
                            {transaction.quantity}
                          </strong>

                        </div>


                        <div className="transaction-detail">

                          <span>
                            Price
                          </span>

                          <strong>
                            ₹{formatMoney(
                              transaction.price
                            )}
                          </strong>

                        </div>


                        <div className="transaction-total">

                          <span>
                            Total
                          </span>

                          <strong>
                            ₹{formatMoney(
                              transaction.totalAmount
                            )}
                          </strong>

                        </div>


                        <div className="transaction-date">

                          <span>
                            Executed
                          </span>

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