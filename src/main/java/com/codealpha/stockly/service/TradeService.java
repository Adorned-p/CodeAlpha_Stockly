package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.BuyRequest;
import com.codealpha.stockly.dto.SellRequest;
import com.codealpha.stockly.dto.TradeResponse;
import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.PortfolioSnapshot;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.StockStatus;
import com.codealpha.stockly.entity.Transaction;
import com.codealpha.stockly.entity.TransactionType;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.HoldingRepository;
import com.codealpha.stockly.repository.PortfolioSnapshotRepository;
import com.codealpha.stockly.repository.StockRepository;
import com.codealpha.stockly.repository.TransactionRepository;
import com.codealpha.stockly.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class TradeService {

    private final CurrencyConversionService currencyConversionService;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;
    private final UserRepository userRepository;
    private final StockRepository stockRepository;
    private final HoldingRepository holdingRepository;
    private final TransactionRepository transactionRepository;

    public TradeService(
            UserRepository userRepository,
            StockRepository stockRepository,
            HoldingRepository holdingRepository,
            TransactionRepository transactionRepository,
            PortfolioSnapshotRepository portfolioSnapshotRepository,
            CurrencyConversionService currencyConversionService
    ) {
        this.userRepository = userRepository;
        this.stockRepository = stockRepository;
        this.holdingRepository = holdingRepository;
        this.transactionRepository = transactionRepository;
        this.portfolioSnapshotRepository = portfolioSnapshotRepository;
        this.currencyConversionService = currencyConversionService;
    }

    // =========================================================
    // BUY STOCK
    // =========================================================

    @Transactional
    public TradeResponse buyStock(
            String userEmail,
            BuyRequest request
    ) {

        // 1. Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        // 2. Validate symbol and exchange
        String symbol = normalize(request.getSymbol());
        String exchange = normalize(request.getExchange());

        // 3. Find the exact stock using symbol + exchange
        Stock stock = stockRepository
                .findBySymbolAndExchange(symbol, exchange)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Stock not found: "
                                        + symbol
                                        + " on "
                                        + exchange
                        )
                );

        // 4. Check stock status
        if (stock.getStatus() == StockStatus.SUSPENDED) {

            throw new IllegalArgumentException(
                    "Stock is currently suspended"
            );
        }

        // 5. Get current price
        BigDecimal price = stock.getCurrentPrice();

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Stock price is not available"
            );
        }

        // 6. Calculate total cost
        BigDecimal totalAmount = price.multiply(
                BigDecimal.valueOf(request.getQuantity())
        );

        // 7. Check balance
        if (user.getVirtualBalance().compareTo(totalAmount) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient virtual balance"
            );
        }

        // 8. Deduct money
        BigDecimal remainingBalance =
                user.getVirtualBalance()
                        .subtract(totalAmount);

        user.setVirtualBalance(remainingBalance);

        userRepository.save(user);

        // 9. Find existing holding for this exact stock
        Holding holding = holdingRepository
                .findByUserAndStock(user, stock)
                .orElse(null);

        if (holding == null) {

            // First time buying this exact stock
            holding = new Holding();

            holding.setUser(user);
            holding.setStock(stock);
            holding.setQuantity(request.getQuantity());
            holding.setAverageBuyPrice(price);

        } else {

            // User already owns this exact stock

            int oldQuantity = holding.getQuantity();
            int newQuantity = request.getQuantity();

            BigDecimal oldAverage =
                    holding.getAverageBuyPrice();

            BigDecimal oldValue =
                    oldAverage.multiply(
                            BigDecimal.valueOf(oldQuantity)
                    );

            BigDecimal newValue =
                    price.multiply(
                            BigDecimal.valueOf(newQuantity)
                    );

            int totalQuantity =
                    oldQuantity + newQuantity;

            BigDecimal newAverage =
                    oldValue
                            .add(newValue)
                            .divide(
                                    BigDecimal.valueOf(totalQuantity),
                                    2,
                                    RoundingMode.HALF_UP
                            );

            holding.setQuantity(totalQuantity);
            holding.setAverageBuyPrice(newAverage);
        }

        holdingRepository.save(holding);

        // 10. Create transaction record
        Transaction transaction = new Transaction();

        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setType(TransactionType.BUY);
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(price);
        transaction.setTotalAmount(totalAmount);
        transaction.setExecutedAt(LocalDateTime.now());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        savePortfolioSnapshot(user);

        // 11. Return response
        return new TradeResponse(
                savedTransaction.getId(),
                stock.getSymbol(),
                stock.getExchange(),
                TransactionType.BUY,
                request.getQuantity(),
                price,
                totalAmount,
                remainingBalance,
                savedTransaction.getExecutedAt()
        );
    }

    // =========================================================
    // SELL STOCK
    // =========================================================

    @Transactional
    public TradeResponse sellStock(
            String userEmail,
            SellRequest request
    ) {

        // 1. Find the user
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "User not found"
                        )
                );

        // 2. Validate symbol and exchange
        String symbol = normalize(request.getSymbol());
        String exchange = normalize(request.getExchange());

        // 3. Find the exact stock using symbol + exchange
        Stock stock = stockRepository
                .findBySymbolAndExchange(symbol, exchange)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Stock not found: "
                                        + symbol
                                        + " on "
                                        + exchange
                        )
                );

        // 4. Check stock status
        if (stock.getStatus() == StockStatus.SUSPENDED) {

            throw new IllegalArgumentException(
                    "Stock is currently suspended"
            );
        }

        // 5. Find user's holding for this exact stock
        Holding holding = holdingRepository
                .findByUserAndStock(user, stock)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "You do not own this stock"
                        )
                );

        // 6. Check quantity
        if (holding.getQuantity() < request.getQuantity()) {

            throw new IllegalArgumentException(
                    "Insufficient shares to sell"
            );
        }

        // 7. Get current stock price
        BigDecimal price = stock.getCurrentPrice();

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Stock price is not available"
            );
        }

        // 8. Calculate sale amount
        BigDecimal totalAmount =
                price.multiply(
                        BigDecimal.valueOf(
                                request.getQuantity()
                        )
                );

        // 9. Add money to user's balance
        BigDecimal remainingBalance =
                user.getVirtualBalance()
                        .add(totalAmount);

        user.setVirtualBalance(remainingBalance);

        userRepository.save(user);

        // 10. Reduce holding
        int remainingQuantity =
                holding.getQuantity()
                        - request.getQuantity();

        if (remainingQuantity == 0) {

            holdingRepository.delete(holding);
            holdingRepository.flush();

        } else {

            holding.setQuantity(remainingQuantity);

            holdingRepository.save(holding);
        }

        // 11. Create transaction
        Transaction transaction = new Transaction();

        transaction.setUser(user);
        transaction.setStock(stock);
        transaction.setType(TransactionType.SELL);
        transaction.setQuantity(request.getQuantity());
        transaction.setPrice(price);
        transaction.setTotalAmount(totalAmount);
        transaction.setExecutedAt(LocalDateTime.now());

        Transaction savedTransaction =
                transactionRepository.save(transaction);

        savePortfolioSnapshot(user);

        // 12. Return response
        return new TradeResponse(
                savedTransaction.getId(),
                stock.getSymbol(),
                stock.getExchange(),
                TransactionType.SELL,
                request.getQuantity(),
                price,
                totalAmount,
                remainingBalance,
                savedTransaction.getExecutedAt()
        );
    }

    // =========================================================
    // PORTFOLIO SNAPSHOT
    // =========================================================

    private void savePortfolioSnapshot(User user) {

        BigDecimal portfolioValueInr =
                BigDecimal.ZERO;

        var holdings =
                holdingRepository.findByUser(user);

        for (Holding holding : holdings) {

            BigDecimal quantity =
                    BigDecimal.valueOf(
                            holding.getQuantity()
                    );

            BigDecimal currentPrice =
                    holding.getStock()
                            .getCurrentPrice();

            if (currentPrice == null) {
                continue;
            }

            BigDecimal currentValue =
                    currentPrice.multiply(quantity);

            String currency =
                    getStockCurrency(
                            holding.getStock()
                    );

            BigDecimal currentValueInr;

            if ("INR".equals(currency)) {

                currentValueInr = currentValue;

            } else {

                currentValueInr =
                        currencyConversionService.convertToInr(
                                currentValue,
                                currency
                        );
            }

            portfolioValueInr =
                    portfolioValueInr.add(
                            currentValueInr
                    );
        }

        PortfolioSnapshot snapshot =
                new PortfolioSnapshot();

        snapshot.setUser(user);

        snapshot.setPortfolioValue(
                portfolioValueInr
        );

        snapshot.setRecordedAt(
                LocalDateTime.now()
        );

        portfolioSnapshotRepository.save(snapshot);
    }

    // =========================================================
    // STOCK CURRENCY
    // =========================================================

    private String getStockCurrency(Stock stock) {

        String exchange =
                stock.getExchange();

        if (exchange == null) {
            return "USD";
        }

        String normalizedExchange =
                exchange.trim().toUpperCase();

        if ("NSE".equals(normalizedExchange)
                || "BSE".equals(normalizedExchange)) {

            return "INR";
        }

        return "USD";
    }

    // =========================================================
    // NORMALIZE
    // =========================================================

    private String normalize(String value) {

        if (value == null || value.isBlank()) {

            throw new IllegalArgumentException(
                    "Symbol and exchange are required"
            );
        }

        return value.trim().toUpperCase();
    }
}