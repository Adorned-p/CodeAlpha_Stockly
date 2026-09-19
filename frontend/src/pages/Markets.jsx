import {
  useEffect,
  useMemo,
  useState,
} from "react";

import { useNavigate } from "react-router-dom";

import {
  ArrowDown,
  ArrowUp,
  BarChart3,
  ChevronRight,
  RefreshCw,
  Search,
  TrendingDown,
  X,
} from "lucide-react";

import DashboardLayout from "../components/layout/DashboardLayout";
import api from "../services/api";


function Markets() {

  const navigate = useNavigate();

  const [stocks, setStocks] = useState([]);

  const [search, setSearch] =
    useState("");

  const [sector, setSector] =
    useState("ALL");

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  const [refreshing, setRefreshing] =
    useState(false);


  /*
   * =====================================================
   * FETCH STOCKS
   * =====================================================
   */

  const fetchStocks = async (
    isRefresh = false
  ) => {

    try {

      if (isRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError("");

      const response =
        await api.get("/stocks");

      console.log(
        "Stocks API response:",
        response.data
      );

      const data =
        Array.isArray(response.data)
          ? response.data
          : Array.isArray(response.data?.stocks)
            ? response.data.stocks
            : Array.isArray(response.data?.data)
              ? response.data.data
              : Array.isArray(response.data?.content)
                ? response.data.content
                : [];

      setStocks(data);

    } catch (err) {

      console.error(
        "Failed to fetch stocks:",
        err
      );

      setError(
        err?.response?.data?.message ||
        "Unable to load stocks."
      );

    } finally {

      setLoading(false);
      setRefreshing(false);

    }

  };


  useEffect(() => {
    fetchStocks();
  }, []);


  /*
   * =====================================================
   * PRICE CHANGE
   * =====================================================
   */

  const calculateChange = (stock) => {
    const current =
      Number(
        stock.currentPrice ?? 0
      );

    const previous =
      Number(
        stock.previousClose ?? 0
      );

    if (
      current <= 0 ||
      previous <= 0
    ) {
      return 0;
    }

    return (
      ((current - previous) /
        previous) *
      100
    );
  };


  /*
   * =====================================================
   * FILTER STOCKS
   * =====================================================
   */

  const filteredStocks =
    useMemo(() => {

      const query =
        search
          .trim()
          .toLowerCase();

      return stocks.filter(
        (stock) => {

          const symbol =
            String(
              stock.symbol || ""
            )
              .trim()
              .toLowerCase();

          const company =
            String(
              stock.companyName || ""
            )
              .trim()
              .toLowerCase();

          const stockSector =
            String(
              stock.sector || ""
            )
              .trim()
              .toUpperCase();

          const selectedSector =
            String(
              sector || ""
            )
              .trim()
              .toUpperCase();


          const matchesSearch =
            !query ||
            symbol.includes(query) ||
            company.includes(query);


          const matchesSector =
            selectedSector === "ALL" ||
            stockSector === selectedSector;


          return (
            matchesSearch &&
            matchesSector
          );

        }
      );

    }, [
      stocks,
      search,
      sector,
    ]);


  /*
   * =====================================================
   * SECTORS
   * =====================================================
   */

  const sectors =
    useMemo(() => {

      return [
        ...new Set(
          stocks
            .map(
              (stock) =>
                stock.sector
            )
            .filter(Boolean)
        ),
      ].sort();

    }, [stocks]);


  /*
   * =====================================================
   * FORMAT PRICE
   * =====================================================
   */

  const formatPrice = (
    price
  ) => {

    return Number(
      price ?? 0
    ).toLocaleString(
      "en-IN",
      {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }
    );

  };


  /*
   * =====================================================
   * FORMAT DATE
   * =====================================================
   */

  const formatDate = (
    date
  ) => {

    if (!date) {
      return "—";
    }

    const parsed =
      new Date(date);

    if (
      Number.isNaN(
        parsed.getTime()
      )
    ) {
      return "—";
    }

    return parsed.toLocaleString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        hour: "2-digit",
        minute: "2-digit",
      }
    );

  };


  /*
   * =====================================================
   * STOCK STATUS
   * =====================================================
   */

  const isStockActive = (
    stock
  ) => {

    return (
      String(
        stock.status || ""
      )
        .trim()
        .toUpperCase() ===
      "ACTIVE"
    );

  };


  /*
   * =====================================================
   * NAVIGATION
   * =====================================================
   */

  const openStockDetails = (
    symbol,
    exchange
  ) => {

    if (!symbol || !exchange) {
      return;
    }

    navigate(
      `/stocks/${encodeURIComponent(
        symbol
      )}?exchange=${encodeURIComponent(
        exchange
      )}`
    );

  };


  /*
   * =====================================================
   * CLEAR SEARCH
   * =====================================================
   */

  const clearSearch = () => {
    setSearch("");
  };


  /*
   * =====================================================
   * RENDER
   * =====================================================
   */

  return (

    <DashboardLayout>

      <div className="markets-page">


        {/* =================================================
            HEADER
        ================================================= */}

        <div className="markets-header">

          <div>

            <p className="eyebrow">
              MARKET
            </p>

            <h1>
              Markets
            </h1>

            <p className="page-description">
              Explore simulated market
              opportunities.
            </p>

          </div>


          <button
            type="button"
            className="refresh-button"
            onClick={() =>
              fetchStocks(true)
            }
            disabled={
              loading ||
              refreshing
            }
          >

            <RefreshCw
              size={16}
              className={
                refreshing
                  ? "refresh-spinning"
                  : ""
              }
            />

            {refreshing
              ? "Refreshing"
              : "Refresh"}

          </button>

        </div>


        {/* =================================================
            SIMULATION BANNER
        ================================================= */}

        <div className="simulation-banner">

          <BarChart3 size={18} />

          <div>

            <strong>
              Simulated market
            </strong>

            <span>
              Stock prices are simulated
              for educational purposes.
              No real money or financial
              transactions are involved.
            </span>

          </div>

        </div>


        {/* =================================================
            FILTER BAR
        ================================================= */}

        <div className="market-filters">


          {/* SEARCH */}

          <div className="market-search">

            <Search
              size={17}
            />

            <input
              type="text"
              value={search}
              placeholder="Search stocks or symbols..."
              onChange={(event) =>
                setSearch(
                  event.target.value
                )
              }
            />


            {search && (

              <button
                type="button"
                className="clear-search"
                onClick={
                  clearSearch
                }
                aria-label="Clear search"
              >
                <X size={14} />
              </button>

            )}

          </div>


          {/* SECTORS */}

          <div className="sector-filters">

            <button
              type="button"
              className={
                sector === "ALL"
                  ? "sector-button active"
                  : "sector-button"
              }
              onClick={() =>
                setSector("ALL")
              }
            >
              All
            </button>


            {sectors.map(
              (item) => (

                <button
                  type="button"
                  key={item}
                  className={
                    sector === item
                      ? "sector-button active"
                      : "sector-button"
                  }
                  onClick={() =>
                    setSector(item)
                  }
                >
                  {item}
                </button>

              )
            )}

          </div>

        </div>


        {/* =================================================
            ACTIVE FILTER INFO
        ================================================= */}

        {(search ||
          sector !== "ALL") && (

          <div className="active-filter-info">

            <span>

              Showing{" "}
              <strong>
                {filteredStocks.length}
              </strong>{" "}
              of{" "}
              <strong>
                {stocks.length}
              </strong>{" "}
              stocks

            </span>


            <button
              type="button"
              onClick={() => {

                setSearch("");
                setSector("ALL");

              }}
            >
              Clear filters
            </button>

          </div>

        )}


        {/* =================================================
            STOCK CARD
        ================================================= */}

        <section className="markets-card">


          <div className="markets-card-header">

            <div>

              <h2>
                Stocks
              </h2>

              <p>
                {filteredStocks.length}{" "}
                {filteredStocks.length === 1
                  ? "asset"
                  : "assets"}{" "}
                available
              </p>

            </div>

          </div>


          {/* =================================================
              LOADING
          ================================================= */}

          {loading && (

            <div className="market-state">

              <RefreshCw
                size={26}
                className="refresh-spinning"
              />

              <p>
                Loading market data...
              </p>

            </div>

          )}


          {/* =================================================
              ERROR
          ================================================= */}

          {!loading &&
            error && (

              <div className="market-state error-state">

                <TrendingDown
                  size={30}
                />

                <p>
                  {error}
                </p>

                <button
                  type="button"
                  className="retry-button"
                  onClick={() =>
                    fetchStocks()
                  }
                >
                  Try again
                </button>

              </div>

            )}


          {/* =================================================
              EMPTY
          ================================================= */}

          {!loading &&
            !error &&
            filteredStocks.length === 0 && (

              <div className="market-state">

                <Search
                  size={30}
                />

                <p>
                  No stocks found
                </p>

                <span>
                  Try changing your search
                  or sector filter.
                </span>

                {(search ||
                  sector !== "ALL") && (

                  <button
                    type="button"
                    className="retry-button"
                    onClick={() => {

                      setSearch("");
                      setSector("ALL");

                    }}
                  >
                    Clear filters
                  </button>

                )}

              </div>

            )}


          {/* =================================================
              TABLE
          ================================================= */}

          {!loading &&
            !error &&
            filteredStocks.length > 0 && (

              <div className="stock-table-wrapper">

                <table className="stock-table">

                  <thead>

                    <tr>

                      <th>
                        Asset
                      </th>

                      <th>
                        Price
                      </th>

                      <th>
                        Change
                      </th>

                      <th>
                        Sector
                      </th>

                      <th>
                        Status
                      </th>

                      <th>
                        Updated
                      </th>

                      <th>
                        Action
                      </th>

                    </tr>

                  </thead>


                  <tbody>

                    {filteredStocks.map(
                      (stock) => {

                        const change =
                          calculateChange(
                            stock
                          );

                        const positive =
                          change >= 0;

                        const active =
                          isStockActive(
                            stock
                          );


                        return (

                          <tr
                            key={
                              stock.id ??
                              `${stock.symbol}-${stock.exchange}`
                            }
                            className={
                              active
                                ? "stock-table-row"
                                : "stock-table-row suspended-row"
                            }
                            onClick={() =>
                              openStockDetails(
                                stock.symbol,
                                stock.exchange
                              )
                            }
                          >


                            {/* ASSET */}

                            <td>

                              <div className="asset-cell">

                                <div className="asset-icon">

                                  {stock.symbol
                                    ?.charAt(
                                      0
                                    )
                                    ?.toUpperCase() ||
                                    "?"}

                                </div>


                                <div className="asset-details">

                                  <strong>
                                    {
                                      stock.symbol
                                    }
                                  </strong>

                                  <span>
                                    {
                                      stock.companyName ||
                                      "Unknown company"
                                    }
                                  </span>

                                </div>

                              </div>

                            </td>


                            {/* PRICE */}

                            <td>

                              <strong className="stock-price">

                                ₹
                                {formatPrice(
                                  stock.currentPriceInr ??
                                  stock.currentPrice
                                )}

                              </strong>

                            </td>


                            {/* CHANGE */}

                            <td>

                              <div
                                className={
                                  positive
                                    ? "market-change positive"
                                    : "market-change negative"
                                }
                              >

                                {positive ? (
                                  <ArrowUp
                                    size={13}
                                  />
                                ) : (
                                  <ArrowDown
                                    size={13}
                                  />
                                )}

                                {positive
                                  ? "+"
                                  : ""}

                                {change.toFixed(
                                  2
                                )}
                                %

                              </div>

                            </td>


                            {/* SECTOR */}

                            <td>

                              <span className="sector-label">

                                {stock.sector ||
                                  "—"}

                              </span>

                            </td>


                            {/* STATUS */}

                            <td>

                              <span
                                className={
                                  active
                                    ? "status-badge active"
                                    : "status-badge suspended"
                                }
                              >

                                <span className="status-dot" />

                                {stock.status ||
                                  "UNKNOWN"}

                              </span>

                            </td>


                            {/* UPDATED */}

                            <td>

                              <span className="updated-time">

                                {formatDate(
                                  stock.lastUpdated
                                )}

                              </span>

                            </td>


                            {/* ACTION */}

                            <td>

                              <button
                                type="button"
                                className="trade-button"
                                disabled={!active}
                                onClick={(
                                  event
                                ) => {

                                  event.stopPropagation();

                                  if (
                                    active
                                  ) {

                                    openStockDetails(
                                      stock.symbol,
                                      stock.exchange
                                    );

                                  }

                                }}
                              >

                                {active
                                  ? "Trade"
                                  : "Suspended"}

                                {active && (
                                  <ChevronRight
                                    size={13}
                                  />
                                )}

                              </button>

                            </td>

                          </tr>

                        );

                      }
                    )}

                  </tbody>

                </table>

              </div>

            )}

        </section>

      </div>

    </DashboardLayout>

  );
}


export default Markets;