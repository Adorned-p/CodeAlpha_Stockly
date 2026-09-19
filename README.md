# Stockly 📈

Stockly is a full-stack virtual stock trading platform designed to simulate stock-market investing and trading in a safe and educational environment.

The application allows users to explore market data, search for stocks, maintain a watchlist, view stock details, manage a virtual portfolio, and place simulated orders using different order types.

Stockly uses a React frontend, Spring Boot backend, MySQL database, and multiple external market-data providers.

> **Note:** Stockly is a virtual trading application. It does not place real stock-market orders or use real money.

---

# 📌 Table of Contents

- [Overview](#-overview)
- [Features](#-features)
- [Trading Features](#-trading-features)
- [Market Data](#-market-data)
- [Indian Market Support](#-indian-market-support)
- [AI Features](#-ai-features)
- [Technology Stack](#-technology-stack)
- [System Architecture](#-system-architecture)
- [Project Structure](#-project-structure)
- [Database](#-database)
- [Environment Variables](#-environment-variables)
- [Backend Setup](#-backend-setup)
- [Frontend Setup](#-frontend-setup)
- [Running the Application](#-running-the-application)
- [Application Workflow](#-application-workflow)
- [Order Types](#-order-types)
- [Security](#-security)
- [Market Data Providers](#-market-data-providers)
- [Admin Functionality](#-admin-functionality)
- [Current Status](#-current-status)
- [Known Improvements and Fixes](#-known-improvements-and-fixes)
- [Future Updates](#-future-updates)
- [Limitations](#-limitations)
- [Disclaimer](#-disclaimer)
- [Author](#-author)

---

# 🚀 Overview

Stockly provides a simulated stock-trading experience where users can:

- Create an account
- Log in securely
- Browse market information
- Search for instruments
- View individual stock details
- Monitor stock prices
- Add stocks to a watchlist
- Buy and sell stocks using virtual funds
- Place different types of orders
- Manage open orders
- Track executed transactions
- View portfolio holdings
- Monitor profit/loss
- Use AI-powered stock and portfolio assistance

The platform is designed with an exchange-aware architecture so that the same stock symbol can be handled correctly across different exchanges.

---

# ✨ Features

## 🔐 Authentication

Stockly provides secure user authentication using Spring Security and JWT.

Features include:

- User registration
- User login
- JWT-based authentication
- Protected routes
- Authenticated API requests
- User profile
- Role-based authorization

## 📊 Dashboard

The dashboard provides an overview of the user's trading account and market information.

It can display:

- Account summary
- Virtual balance
- Portfolio information
- Market information
- Trading activity
- Relevant market statistics

## 📈 Markets

The Markets section allows users to explore available stocks and market instruments.

Features include:

- Stock listings
- Stock search
- Market information
- Exchange information
- Sector information
- Current prices
- Stock selection
- Navigation to detailed stock pages

## 🔎 Stock Search

Stockly supports instrument searching through external market-data providers.

Search results can contain:

- Stock symbol
- Company name
- Exchange
- Country
- Currency
- Asset type
- Market-data provider
- Provider-specific symbol

Provider-specific symbols are maintained so that the application does not incorrectly assume that one symbol format works across every market-data provider.

---

# 🇮🇳 Indian Market Support

Stockly includes support for Indian market instruments.

The application supports exchange-aware handling for:

- NSE
- BSE

Indian stocks can therefore be represented using both:

```text
Symbol
Exchange
```

instead of relying only on the stock symbol.

For example:

```text
HDFC
NSE
```

and another listing with the same symbol on another exchange can be treated as a separate instrument.

This exchange-aware approach is used across:

- Stock details
- Watchlist
- Portfolio
- Holdings
- Buy orders
- Sell orders
- Transactions
- Market data
- Historical data

---

# ⭐ Watchlist

Users can maintain a personal watchlist.

Features include:

- Add stock to watchlist
- Remove stock from watchlist
- View watchlisted stocks
- View exchange information
- Open stock details directly from the watchlist

Stock identification considers both the symbol and exchange.

---

# 💰 Virtual Trading

Stockly provides a simulated trading system using virtual funds.

Users can:

- Buy stocks
- Sell stocks
- Track holdings
- Track available balance
- Track invested value
- Monitor profit/loss
- View completed transactions

No real money is involved.

---

# 📋 Order Management

Stockly supports multiple order types.

### Market Order

A market order attempts to execute using the current available market price.

### Limit Order

A limit order executes when the market reaches the specified limit price.

### Stop Order

A stop order becomes triggered when the market reaches the specified stop price.

### Stop-Limit Order

A stop-limit order combines:

- Stop price
- Limit price

Once the stop condition is reached, the order becomes eligible for limit-order execution.

## 🔄 Order Features

The trading system supports:

- Placing orders
- Open orders
- Order history
- Order cancellation
- Order execution
- Stop-order triggering
- Stop-limit handling
- Virtual balance management
- Order matching
- Trade execution
- Exchange-aware order processing

---

# 💼 Portfolio

The Portfolio section provides an overview of the user's current holdings.

Portfolio information includes:

- Stock symbol
- Exchange
- Company name
- Quantity
- Average purchase price
- Current market price
- Investment value
- Current value
- Profit/loss

The portfolio is calculated using the user's actual simulated holdings.

---

# 🧾 Transactions

Stockly maintains a record of completed trades.

Users can view:

- Buy transactions
- Sell transactions
- Quantity
- Execution price
- Total amount
- Execution time
- Stock symbol
- Exchange

Transactions can also be searched and filtered through the frontend.

---

# 🤖 AI Features

Stockly includes AI-powered functionality to provide additional assistance to users.

The application contains:

- AI stock assistance
- AI portfolio assistance
- AI chat functionality
- Stock-related insights

The AI functionality is integrated through the backend and can use an external AI service.

AI-generated information should be treated as informational rather than financial advice.

---

# 🌐 Market Data

Stockly uses external market-data providers to obtain market information and prices.

The application has a provider-based market-data architecture.

Supported providers include:

- Twelve Data
- Alpha Vantage
- EODHD
- Indian Market Data API

The backend uses provider-specific clients and services to communicate with these providers.

## 🔌 Market Data Architecture

```text
                    Stockly
                       |
                       v
            MarketDataProviderService
                       |
          +------------+------------+
          |            |            |
          v            v            v
     Twelve Data     EODHD     Alpha Vantage
          |
          |
          v
 Indian Market Data Provider
```

Provider-specific symbols are stored with instruments where required.

This allows Stockly to handle differences between provider symbol formats.

---

# 🗃️ Database

Stockly uses MySQL for persistent application data.

The database contains information related to:

- Users
- Stocks
- Instruments
- Holdings
- Orders
- Executions
- Transactions
- Watchlists
- Market quotes
- Historical stock prices
- Portfolio snapshots
- Wallet transactions

Hibernate/JPA is used for database interaction.

The current development configuration uses:

```properties
spring.jpa.hibernate.ddl-auto=update
```

which allows Hibernate to update the database schema based on the application's entity definitions.

---

# 🛠️ Technology Stack

## Backend

- Java 17
- Spring Boot
- Spring MVC
- Spring Data JPA
- Spring Security
- JWT
- MySQL
- Maven
- Caffeine Cache
- Jackson

## Frontend

- React
- Vite
- React Router
- Axios
- Recharts
- Lucide React
- React Markdown
- Remark GFM

## External APIs

- Twelve Data
- Alpha Vantage
- EODHD
- Indian Market Data API
- Groq

---

# 🏗️ System Architecture

Stockly follows a layered backend architecture.

```text
                        React Frontend
                              |
                              |
                         REST APIs
                              |
                              v
                    Spring Boot Backend
                              |
             +----------------+----------------+
             |                |                |
             v                v                v
        Controllers       Services         Security
             |                |
             |                |
             +--------+-------+
                      |
                      v
                 Repositories
                      |
                      v
                    MySQL
```

The application follows a separation of responsibilities between:

- Controllers
- Services
- Repositories
- Entities
- DTOs
- Security
- External market-data providers

---

# 📁 Project Structure

```text
Stockly/
│
├── frontend/
│   ├── public/
│   ├── src/
│   │   ├── assets/
│   │   ├── components/
│   │   │   └── layout/
│   │   ├── context/
│   │   ├── pages/
│   │   └── services/
│   ├── package.json
│   ├── package-lock.json
│   └── README.md
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── codealpha/
│   │   │           └── stockly/
│   │   │               ├── config/
│   │   │               ├── controller/
│   │   │               ├── dto/
│   │   │               ├── entity/
│   │   │               ├── exception/
│   │   │               ├── repository/
│   │   │               ├── security/
│   │   │               └── service/
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── .gitignore
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md
```

---

# ⚙️ Requirements

Before running Stockly, install:

- Java 17 or later
- MySQL
- Node.js
- npm
- Maven
- IntelliJ IDEA or another Java IDE

---

# 🗄️ Database Setup

Create the MySQL database:

```sql
CREATE DATABASE stockly;
```

The application expects the database to be available at:

```text
localhost:3306/stockly
```

Database credentials should be provided through environment variables.

---

# 🔐 Environment Variables

Sensitive credentials should **never be committed to GitHub**.

Configure the following environment variables:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password

TWELVE_DATA_API_KEY=your_twelve_data_api_key
ALPHA_VANTAGE_API_KEY=your_alpha_vantage_api_key
INDIAN_API_KEY=your_indian_api_key
EODHD_API_KEY=your_eodhd_api_key

GROQ_API_KEY=your_groq_api_key
```

Example Spring Boot configuration:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/stockly
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

twelvedata.api-key=${TWELVE_DATA_API_KEY}
alphavantage.api-key=${ALPHA_VANTAGE_API_KEY}
indianapi.api-key=${INDIAN_API_KEY}
eodhd.api-key=${EODHD_API_KEY}
```

### Important

Do not commit:

```text
.env
API keys
Database passwords
JWT secrets
Private credentials
```

---

# ▶️ Running the Backend

Open a terminal in the Stockly root directory.

### Windows

Using the Maven wrapper:

```bash
mvnw.cmd spring-boot:run
```

Or if Maven is installed:

```bash
mvn spring-boot:run
```

The backend runs on:

```text
http://localhost:8081
```

---

# ▶️ Running the Frontend

Open another terminal:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Start the development server:

```bash
npm run dev
```

Vite will display the frontend URL in the terminal.

Usually:

```text
http://localhost:5173
```

---

# 🔄 Application Workflow

```text
Register
   |
   v
Login
   |
   v
Dashboard
   |
   v
Markets
   |
   v
Search Stock
   |
   v
Stock Details
   |
   +---------> Add to Watchlist
   |
   v
Place Order
   |
   +----> Market
   |
   +----> Limit
   |
   +----> Stop
   |
   +----> Stop-Limit
   |
   v
Order Execution
   |
   v
Portfolio
   |
   v
Transactions
```

---

# 🔒 Security

Stockly uses Spring Security and JWT authentication.

Security responsibilities include:

- Authentication
- Authorization
- JWT validation
- Protected endpoints
- Role-based access
- Administrative access control

Sensitive configuration values are loaded through environment variables.

---

# 👨‍💼 Admin Functionality

Stockly contains administrative functionality for application management.

Administrative functionality includes:

- Market-data synchronization
- Historical market-data backfill
- Market statistics
- Stock management
- Administrative account operations

Administrative endpoints are protected by the application's authorization system.

---

# ⚡ Caching

Stockly uses Caffeine for application-level caching.

Caching can reduce unnecessary repeated requests for data that does not need to be retrieved repeatedly.

---

# 📡 Scheduled Market Data

Stockly includes scheduled market-data functionality.

The application can periodically refresh market information and maintain historical price information.

Historical market data can also be synchronized/backfilled through administrative functionality.

---

# 🧪 Testing Status

The major application flows have been manually tested during development.

Tested areas include:

- User registration
- User login
- Dashboard
- Markets
- Stock details
- Watchlist
- Buy orders
- Sell orders
- Portfolio
- Transactions
- Open orders
- Market orders
- Limit orders
- Stop orders
- Stop-limit orders
- NSE stock handling
- BSE stock handling
- Exchange-aware stock navigation

The application has also been tested with multiple market-data providers and provider-specific symbols.

---

# 🐛 Known Improvements and Fixes

The following areas have been addressed during development or remain areas for further refinement.

## Exchange-aware stock handling

Stock identification was improved to consider:

```text
Symbol + Exchange
```

instead of relying only on:

```text
Symbol
```

This prevents conflicts when the same symbol exists on different exchanges.

## Provider-specific symbols

Market-data provider symbol formats can differ.

Stockly therefore stores provider-specific symbols with instruments rather than reconstructing provider symbols incorrectly in unrelated parts of the application.

This improves compatibility with different market-data providers.

## Order execution

The order system has been refined to correctly handle:

- Market orders
- Limit orders
- Stop orders
- Stop-limit orders
- Order triggering
- Order matching
- Virtual balance reservation

Stop-limit orders are handled so that a triggered order does not repeatedly trigger unnecessarily.

## Indian market compatibility

Indian market instruments required additional exchange-aware handling.

The application now supports separate handling for NSE and BSE listings.

## Historical market data

Historical market-data handling has been separated from normal quote retrieval.

This prevents conflicts between historical-data endpoints and current market quote endpoints.

## API provider fallback

Stockly uses provider-specific market-data services and fallback handling where appropriate.

This improves resilience when a particular provider cannot return data for a specific instrument.

---

# 🔮 Future Updates

The following improvements are planned for future versions of Stockly.

## 📊 Advanced Portfolio Analytics

Future versions may include:

- Portfolio performance charts
- Daily/weekly/monthly returns
- Asset allocation charts
- Sector allocation
- Profit/loss history
- Risk metrics
- Investment performance comparison

## 📈 Advanced Stock Analysis

Potential additions include:

- Moving averages
- RSI
- MACD
- Bollinger Bands
- Additional technical indicators
- Candlestick pattern analysis
- Advanced charting
- Comparative stock analysis

## 🔔 Price Alerts

Future versions may support:

- Price alerts
- Target-price notifications
- Stop-loss notifications
- Watchlist alerts
- Order execution notifications

## 🧠 Improved AI Assistant

Future AI improvements may include:

- More detailed stock analysis
- Portfolio-level informational insights
- Natural-language portfolio queries
- Market summaries
- Historical trend analysis
- Personalized informational insights

AI features will continue to be presented as informational tools rather than financial advice.

## 🌍 More Markets and Exchanges

Future versions may expand support for:

- Additional Indian exchanges
- US exchanges
- European markets
- Additional international exchanges

## 📱 Responsive UI Improvements

Future frontend improvements may include:

- Improved mobile layouts
- Better tablet support
- Responsive trading interfaces
- Improved charts on small screens
- Enhanced accessibility

## 🎨 UI/UX Improvements

Possible improvements include:

- More detailed dashboard widgets
- Customizable dashboard
- Improved loading states
- Improved error messages
- Better empty states
- Improved chart interactions
- Dark/light theme customization

## ⚡ Performance Improvements

Future versions may introduce:

- More extensive caching
- Optimized database queries
- Reduced API requests
- Pagination for large datasets
- Background processing
- Improved market-data synchronization

## 🛡️ Security Improvements

Future security improvements may include:

- Refresh-token support
- Improved password policies
- Account verification
- Password reset functionality
- Login attempt protection
- More granular authorization
- Improved secret management for production deployment

## ☁️ Deployment

Future versions can be deployed using cloud infrastructure.

Possible deployment targets include:

- Docker
- AWS
- Azure
- Google Cloud
- Railway
- Render
- Other cloud platforms

A production deployment would also require production database configuration, secure secret management, HTTPS, and additional security hardening.

---

# 📝 Future Fixes and Maintenance

The project will continue to receive fixes and maintenance in areas such as:

- Market-data provider API changes
- Exchange symbol changes
- API rate-limit handling
- UI edge cases
- Order execution edge cases
- Database query optimization
- Error handling
- Validation
- Responsive design
- Dependency updates
- Security updates

External APIs can change independently of Stockly, so provider integrations may require maintenance over time.

---

# ⚠️ Current Limitations

Stockly is currently intended primarily as an educational and demonstration project.

Current limitations may include:

- Market-data providers have API rate limits.
- External provider availability can affect market-data retrieval.
- Market prices depend on external APIs.
- The application is not connected to a real brokerage account.
- Trades are simulated.
- The current database configuration is intended for local development.
- Production deployment would require additional security and infrastructure configuration.

---

# 💡 Design Philosophy

Stockly is built around several core principles:

### Separation of concerns

Controllers, services, repositories, entities, DTOs, and external providers have separate responsibilities.

### Exchange awareness

A stock is not identified only by its symbol. The exchange is also considered where required.

### Provider abstraction

Market-data providers are separated behind provider interfaces and services.

### Virtual trading

Trading operations are simulated so users can experiment without real financial risk.

### Security

Sensitive credentials are kept outside source code through environment variables.

---

# 📚 Educational Purpose

Stockly was developed as a full-stack software project to demonstrate practical implementation of:

- REST APIs
- Spring Boot
- Spring Security
- JWT authentication
- Database design
- JPA/Hibernate
- React
- API integration
- Market-data processing
- Trading logic
- Order matching
- Caching
- External service integration
- Full-stack application architecture

---

# ⚠️ Disclaimer

Stockly is a virtual stock-trading and educational application.

It does not execute real stock-market trades and does not connect users to a real brokerage account.

Market data is obtained from external services and may be delayed, unavailable, rate-limited, or inaccurate.

Nothing provided by Stockly should be considered financial, investment, or trading advice.

Users should not make real financial decisions solely based on information displayed by this application or generated by its AI features.

---

# 👨‍💻 Author

**Stockly**

A full-stack virtual stock trading platform built using:

```text
Java
Spring Boot
Spring Security
JWT
MySQL
React
Vite
REST APIs
External Market Data APIs
AI Integration
```

---

# 📌 Project Status

**Current status:** Active development / academic project

The core application functionality has been implemented and tested.

Future development will focus on:

- Advanced analytics
- Improved AI capabilities
- Additional market support
- UI/UX improvements
- Performance optimization
- Security hardening
- Cloud deployment
- Additional testing
