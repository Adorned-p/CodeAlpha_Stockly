package com.codealpha.stockly.service;

import com.codealpha.stockly.exception.ResourceNotFoundException;
import com.codealpha.stockly.dto.OrderRequest;
import com.codealpha.stockly.dto.OrderResponse;
import com.codealpha.stockly.entity.Execution;
import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Order;
import com.codealpha.stockly.entity.OrderSide;
import com.codealpha.stockly.entity.OrderStatus;
import com.codealpha.stockly.entity.OrderType;
import com.codealpha.stockly.entity.Stock;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.ExecutionRepository;
import com.codealpha.stockly.repository.HoldingRepository;
import com.codealpha.stockly.repository.InstrumentRepository;
import com.codealpha.stockly.repository.OrderRepository;
import com.codealpha.stockly.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final MarketQuoteService marketQuoteService;
    private final MatchingEngineService matchingEngineService;
    private final UserRepository userRepository;
    private final InstrumentRepository instrumentRepository;
    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final HoldingRepository holdingRepository;

    public OrderService(
            UserRepository userRepository,
            InstrumentRepository instrumentRepository,
            OrderRepository orderRepository,
            ExecutionRepository executionRepository,
            HoldingRepository holdingRepository,
            MatchingEngineService matchingEngineService,
            MarketQuoteService marketQuoteService
    ) {
        this.userRepository = userRepository;
        this.instrumentRepository = instrumentRepository;
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.holdingRepository = holdingRepository;
        this.matchingEngineService = matchingEngineService;
        this.marketQuoteService = marketQuoteService;
    }

    // =========================================================
    // PLACE ORDER
    // =========================================================

    @Transactional
    public OrderResponse placeOrder(
            String userEmail,
            OrderRequest request
    ) {

        validateOrder(request);

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        String symbol =
                request.getSymbol().toUpperCase();

        Instrument instrument =
                instrumentRepository
                        .findBySymbol(symbol)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Instrument not found: "
                                                + symbol
                                )
                        );

        if (!instrument.isActive()) {

            throw new IllegalArgumentException(
                    "Instrument is currently inactive"
            );
        }

        Order order = new Order();

        order.setUser(user);
        order.setInstrument(instrument);
        order.setSide(request.getSide());
        order.setType(request.getType());
        order.setQuantity(request.getQuantity());
        order.setFilledQuantity(0);
        order.setLimitPrice(request.getLimitPrice());
        order.setStopPrice(request.getStopPrice());

        LocalDateTime now =
                LocalDateTime.now();

        order.setCreatedAt(now);
        order.setUpdatedAt(now);

        order.setStatus(
                OrderStatus.PENDING
        );

        order = orderRepository.save(order);

        // =====================================================
        // MARKET ORDER
        // =====================================================

        if (request.getType() == OrderType.MARKET) {

            BigDecimal executionPrice =
                    instrument.getCurrentPrice();

            if (executionPrice == null ||
                    executionPrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                throw new IllegalArgumentException(
                        "Current market price is not available for "
                                + instrument.getSymbol()
                );
            }

            if (request.getSide() == OrderSide.BUY) {

                executeBuy(
                        user,
                        instrument,
                        request.getQuantity(),
                        executionPrice
                );

            } else {

                executeSell(
                        user,
                        instrument,
                        request.getQuantity(),
                        executionPrice
                );
            }

            BigDecimal totalAmount =
                    executionPrice.multiply(
                            BigDecimal.valueOf(
                                    request.getQuantity()
                            )
                    );

            Execution execution =
                    new Execution();

            execution.setOrder(order);
            execution.setQuantity(
                    request.getQuantity()
            );
            execution.setPrice(
                    executionPrice
            );
            execution.setTotalAmount(
                    totalAmount
            );
            execution.setExecutedAt(
                    LocalDateTime.now()
            );

            executionRepository.save(execution);

            order.setFilledQuantity(
                    request.getQuantity()
            );

            order.setAverageFillPrice(
                    executionPrice
            );

            order.setStatus(
                    OrderStatus.FILLED
            );

            order.setUpdatedAt(
                    LocalDateTime.now()
            );

            orderRepository.save(order);

            return toResponse(order);
        }

        // =====================================================
        // LIMIT ORDER
        // =====================================================

        if (request.getType() == OrderType.LIMIT) {

            reserveForLimitOrder(
                    user,
                    instrument,
                    request
            );

            order.setStatus(OrderStatus.OPEN);
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            matchingEngineService.match(order);

            return toResponse(
                    orderRepository
                            .findById(order.getId())
                            .orElse(order)
            );
        }

        // =====================================================
        // STOP / STOP LIMIT ORDER
        // =====================================================

        if (request.getType() == OrderType.STOP ||
                request.getType() == OrderType.STOP_LIMIT) {

            reserveForStopOrder(
                    user,
                    instrument,
                    request
            );

            order.setStatus(OrderStatus.OPEN);
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            /*
             * STOP orders are NOT sent to the matching engine yet.
             *
             * They remain OPEN until StopOrderService detects
             * that the market price has reached the stop price.
             */

            return toResponse(order);
        }

        throw new IllegalArgumentException(
                "Unsupported order type: "
                        + request.getType()
        );
    }

    // =========================================================
    // MARKET BUY
    // =========================================================

    private void executeBuy(
            User user,
            Instrument instrument,
            Integer quantity,
            BigDecimal price
    ) {

        BigDecimal totalAmount =
                price.multiply(
                        BigDecimal.valueOf(quantity)
                );

        BigDecimal balance =
                user.getVirtualBalance();

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        BigDecimal reservedBalance =
                user.getReservedBalance();

        if (reservedBalance == null) {
            reservedBalance = BigDecimal.ZERO;
        }

        BigDecimal availableBalance =
                balance.subtract(
                        reservedBalance
                );

        if (availableBalance.compareTo(
                totalAmount
        ) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient available virtual balance"
            );
        }

        user.setVirtualBalance(
                balance.subtract(totalAmount)
        );

        userRepository.save(user);

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            throw new IllegalArgumentException(
                    "Instrument is not linked to a stock"
            );
        }

        Holding holding =
                holdingRepository
                        .findByUserAndStock(
                                user,
                                stock
                        )
                        .orElse(null);

        if (holding == null) {

            holding = new Holding();

            holding.setUser(user);
            holding.setStock(stock);
            holding.setQuantity(quantity);
            holding.setAverageBuyPrice(price);
            holding.setReservedQuantity(0);

        } else {

            int oldQuantity =
                    holding.getQuantity();

            BigDecimal oldAverage =
                    holding.getAverageBuyPrice();

            if (oldAverage == null) {
                oldAverage = BigDecimal.ZERO;
            }

            BigDecimal oldValue =
                    oldAverage.multiply(
                            BigDecimal.valueOf(oldQuantity)
                    );

            BigDecimal newValue =
                    price.multiply(
                            BigDecimal.valueOf(quantity)
                    );

            int totalQuantity =
                    oldQuantity + quantity;

            BigDecimal newAverage =
                    oldValue
                            .add(newValue)
                            .divide(
                                    BigDecimal.valueOf(
                                            totalQuantity
                                    ),
                                    4,
                                    RoundingMode.HALF_UP
                            );

            holding.setQuantity(totalQuantity);
            holding.setAverageBuyPrice(newAverage);
        }

        holdingRepository.save(holding);
    }

    // =========================================================
    // MARKET SELL
    // =========================================================

    private void executeSell(
            User user,
            Instrument instrument,
            Integer quantity,
            BigDecimal price
    ) {

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            throw new IllegalArgumentException(
                    "Instrument is not linked to a stock"
            );
        }

        Holding holding =
                holdingRepository
                        .findByUserAndStock(
                                user,
                                stock
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "You do not own this stock"
                                )
                        );

        int reservedQuantity =
                holding.getReservedQuantity();

        int availableQuantity =
                holding.getQuantity()
                        - reservedQuantity;

        if (availableQuantity < quantity) {

            throw new IllegalArgumentException(
                    "Insufficient available shares to sell"
            );
        }

        BigDecimal totalAmount =
                price.multiply(
                        BigDecimal.valueOf(quantity)
                );

        BigDecimal balance =
                user.getVirtualBalance();

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        user.setVirtualBalance(
                balance.add(totalAmount)
        );

        userRepository.save(user);

        int remainingQuantity =
                holding.getQuantity()
                        - quantity;

        holding.setQuantity(
                remainingQuantity
        );

        if (remainingQuantity == 0) {

            holdingRepository.delete(holding);

        } else {

            holdingRepository.save(holding);
        }
    }

    // =========================================================
    // RESERVE LIMIT ORDER
    // =========================================================

    private void reserveForLimitOrder(
            User user,
            Instrument instrument,
            OrderRequest request
    ) {

        if (request.getLimitPrice() == null) {

            throw new IllegalArgumentException(
                    "Limit price is required"
            );
        }

        if (request.getSide() == OrderSide.BUY) {

            BigDecimal requiredAmount =
                    request.getLimitPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            request.getQuantity()
                                    )
                            );

            BigDecimal balance =
                    user.getVirtualBalance();

            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            BigDecimal reserved =
                    user.getReservedBalance();

            if (reserved == null) {
                reserved = BigDecimal.ZERO;
            }

            BigDecimal available =
                    balance.subtract(reserved);

            if (available.compareTo(
                    requiredAmount
            ) < 0) {

                throw new IllegalArgumentException(
                        "Insufficient available balance"
                );
            }

            user.setReservedBalance(
                    reserved.add(requiredAmount)
            );

            userRepository.save(user);

            return;
        }

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            throw new IllegalArgumentException(
                    "Instrument is not linked to a stock"
            );
        }

        Holding holding =
                holdingRepository
                        .findByUserAndStock(
                                user,
                                stock
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "You do not own this stock"
                                )
                        );

        int reserved =
                holding.getReservedQuantity();

        int available =
                holding.getQuantity()
                        - reserved;

        if (available < request.getQuantity()) {

            throw new IllegalArgumentException(
                    "Insufficient available shares"
            );
        }

        holding.setReservedQuantity(
                reserved + request.getQuantity()
        );

        holdingRepository.save(holding);
    }

    // =========================================================
// RESERVE STOP ORDER
// =========================================================

    private void reserveForStopOrder(
            User user,
            Instrument instrument,
            OrderRequest request
    ) {

        /*
         * STOP_LIMIT:
         *
         * Reserve using the LIMIT price because
         * that is the maximum BUY amount / limit
         * used by the matching engine.
         *
         * STOP:
         *
         * Reserve using the STOP price because the
         * eventual order becomes a market order and
         * we need a reasonable maximum reservation.
         */

        BigDecimal reservationPrice;

        if (request.getType() == OrderType.STOP_LIMIT) {

            reservationPrice =
                    request.getLimitPrice();

        } else {

            reservationPrice =
                    request.getStopPrice();
        }

        if (request.getSide() == OrderSide.BUY) {

            BigDecimal requiredAmount =
                    reservationPrice.multiply(
                            BigDecimal.valueOf(
                                    request.getQuantity()
                            )
                    );

            BigDecimal balance =
                    user.getVirtualBalance();

            if (balance == null) {
                balance = BigDecimal.ZERO;
            }

            BigDecimal reserved =
                    user.getReservedBalance();

            if (reserved == null) {
                reserved = BigDecimal.ZERO;
            }

            BigDecimal available =
                    balance.subtract(reserved);

            if (available.compareTo(
                    requiredAmount
            ) < 0) {

                throw new IllegalArgumentException(
                        "Insufficient available balance"
                );
            }

            user.setReservedBalance(
                    reserved.add(requiredAmount)
            );

            userRepository.save(user);

            return;
        }

        /*
         * SELL STOP / STOP_LIMIT
         *
         * Reserve shares until the stop is triggered.
         */

        Stock stock =
                instrument.getStock();

        if (stock == null) {

            throw new IllegalArgumentException(
                    "Instrument is not linked to a stock"
            );
        }

        Holding holding =
                holdingRepository
                        .findByUserAndStock(
                                user,
                                stock
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "You do not own this stock"
                                )
                        );

        int reserved =
                holding.getReservedQuantity();

        int available =
                holding.getQuantity()
                        - reserved;

        if (available < request.getQuantity()) {

            throw new IllegalArgumentException(
                    "Insufficient available shares"
            );
        }

        holding.setReservedQuantity(
                reserved + request.getQuantity()
        );

        holdingRepository.save(holding);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateOrder(
            OrderRequest request
    ) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Order request is required"
            );
        }

        if (request.getSymbol() == null ||
                request.getSymbol().isBlank()) {

            throw new IllegalArgumentException(
                    "Stock symbol is required"
            );
        }

        if (request.getQuantity() == null ||
                request.getQuantity() < 1) {

            throw new IllegalArgumentException(
                    "Quantity must be at least 1"
            );
        }

        if (request.getSide() == null) {

            throw new IllegalArgumentException(
                    "Order side is required"
            );
        }

        if (request.getType() == null) {

            throw new IllegalArgumentException(
                    "Order type is required"
            );
        }

        // -----------------------------------------
        // LIMIT
        // -----------------------------------------

        if (request.getType() == OrderType.LIMIT) {

            if (request.getLimitPrice() == null ||
                    request.getLimitPrice()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Valid limit price is required"
                );
            }
        }

        // -----------------------------------------
// STOP
// -----------------------------------------

        if (request.getType() == OrderType.STOP ||
                request.getType() == OrderType.STOP_LIMIT) {

            if (request.getStopPrice() == null ||
                    request.getStopPrice()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Valid stop price is required"
                );
            }
        }

// -----------------------------------------
// STOP LIMIT
// -----------------------------------------

        if (request.getType() == OrderType.STOP_LIMIT) {

            if (request.getLimitPrice() == null ||
                    request.getLimitPrice()
                            .compareTo(BigDecimal.ZERO) <= 0) {

                throw new IllegalArgumentException(
                        "Valid limit price is required for STOP_LIMIT"
                );
            }
        }

        // -----------------------------------------
        // MARKET
        // -----------------------------------------

        if (request.getType() == OrderType.MARKET) {

            if (request.getLimitPrice() != null) {

                throw new IllegalArgumentException(
                        "Market orders cannot have a limit price"
                );
            }

            if (request.getStopPrice() != null) {

                throw new IllegalArgumentException(
                        "Market orders cannot have a stop price"
                );
            }
        }
    }

    // =========================================================
// TRIGGER STOP ORDER
// =========================================================

    @Transactional
    public void triggerStopOrder(Order order) {

        if (order == null) {
            return;
        }

        if (order.getStatus() != OrderStatus.OPEN) {
            return;
        }

        if (order.getType() != OrderType.STOP &&
                order.getType() != OrderType.STOP_LIMIT) {
            return;
        }

        Instrument instrument =
                order.getInstrument();

        if (instrument == null) {
            return;
        }

        BigDecimal marketPrice =
                instrument.getCurrentPrice();

        if (marketPrice == null ||
                marketPrice.compareTo(BigDecimal.ZERO) <= 0) {

            return;
        }

        BigDecimal stopPrice =
                order.getStopPrice();

        if (stopPrice == null ||
                stopPrice.compareTo(BigDecimal.ZERO) <= 0) {

            return;
        }

        /*
         * BUY STOP:
         *
         * Trigger when market price reaches
         * or goes above the stop price.
         */

        boolean triggered;

        if (order.getSide() == OrderSide.BUY) {

            triggered =
                    marketPrice.compareTo(stopPrice) >= 0;

        } else {

            /*
             * SELL STOP:
             *
             * Trigger when market price reaches
             * or falls below the stop price.
             */

            triggered =
                    marketPrice.compareTo(stopPrice) <= 0;
        }

        if (!triggered) {
            return;
        }

        // =========================================================
        // STOP LIMIT
        // =========================================================

        if (order.getType() == OrderType.STOP_LIMIT) {

            /*
             * The stop has triggered.
             *
             * Keep the STOP_LIMIT type for order history,
             * but now give the matching engine the limit price.
             */

            order.setStatus(OrderStatus.OPEN);
            order.setUpdatedAt(LocalDateTime.now());

            orderRepository.save(order);

            matchingEngineService.match(order);

            return;
        }

        // =========================================================
        // STOP -> MARKET
        // =========================================================

        User user =
                order.getUser();

        int quantity =
                order.getQuantity()
                        - order.getFilledQuantity();

        if (quantity <= 0) {
            return;
        }

        /*
         * Release the reservation before executing
         * the market order.
         */

        releaseStopReservation(
                order,
                quantity
        );

        /*
         * Execute using the current market price.
         */

        if (order.getSide() == OrderSide.BUY) {

            executeBuy(
                    user,
                    instrument,
                    quantity,
                    marketPrice
            );

        } else {

            executeSell(
                    user,
                    instrument,
                    quantity,
                    marketPrice
            );
        }

        BigDecimal totalAmount =
                marketPrice.multiply(
                        BigDecimal.valueOf(quantity)
                );

        Execution execution =
                new Execution();

        execution.setOrder(order);
        execution.setQuantity(quantity);
        execution.setPrice(marketPrice);
        execution.setTotalAmount(totalAmount);
        execution.setExecutedAt(LocalDateTime.now());

        executionRepository.save(execution);

        order.setFilledQuantity(
                order.getFilledQuantity() + quantity
        );

        order.setAverageFillPrice(
                marketPrice
        );

        order.setStatus(
                OrderStatus.FILLED
        );

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        orderRepository.save(order);
    }

    // =========================================================
// RELEASE STOP RESERVATION
// =========================================================

    private void releaseStopReservation(
            Order order,
            int quantity
    ) {

        User user =
                order.getUser();

        if (order.getSide() == OrderSide.BUY) {

            BigDecimal reservedBalance =
                    user.getReservedBalance();

            if (reservedBalance == null) {
                reservedBalance = BigDecimal.ZERO;
            }

            BigDecimal reservationPrice =
                    order.getStopPrice();

            BigDecimal amount =
                    reservationPrice.multiply(
                            BigDecimal.valueOf(quantity)
                    );

            BigDecimal newReserved =
                    reservedBalance.subtract(amount);

            if (newReserved.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                newReserved = BigDecimal.ZERO;
            }

            user.setReservedBalance(
                    newReserved
            );

            userRepository.save(user);

            return;
        }

        Instrument instrument =
                order.getInstrument();

        Stock stock =
                instrument.getStock();

        if (stock == null) {
            return;
        }

        Holding holding =
                holdingRepository
                        .findByUserAndStock(
                                user,
                                stock
                        )
                        .orElse(null);

        if (holding == null) {
            return;
        }

        Integer reserved =
                holding.getReservedQuantity();

        if (reserved == null) {
            reserved = 0;
        }

        int newReserved =
                reserved - quantity;

        if (newReserved < 0) {
            newReserved = 0;
        }

        holding.setReservedQuantity(
                newReserved
        );

        holdingRepository.save(holding);
    }

    // =========================================================
    // CANCEL ORDER
    // =========================================================

    @Transactional
    public OrderResponse cancelOrder(
            String userEmail,
            Long orderId
    ) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found: " + orderId
                                )
                        );

        // Make sure the order belongs to the logged-in user
        if (order.getUser() == null ||
                !order.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to cancel this order"
            );
        }

        // Only OPEN or PARTIALLY_FILLED orders can be cancelled
        if (order.getStatus() != OrderStatus.OPEN &&
                order.getStatus() != OrderStatus.PARTIALLY_FILLED) {

            throw new IllegalArgumentException(
                    "Order cannot be cancelled. Current status: "
                            + order.getStatus()
            );
        }

        int remainingQuantity =
                order.getQuantity()
                        - order.getFilledQuantity();

        // =========================================================
        // RELEASE BUY RESERVATION
        // =========================================================

        if (order.getSide() == OrderSide.BUY &&
                remainingQuantity > 0) {

            /*
             * LIMIT and STOP_LIMIT BUY orders reserve using
             * limitPrice.
             *
             * STOP BUY orders reserve using stopPrice.
             */
            BigDecimal reservationPrice = null;

            if (order.getType() == OrderType.LIMIT ||
                    order.getType() == OrderType.STOP_LIMIT) {

                reservationPrice =
                        order.getLimitPrice();

            } else if (order.getType() == OrderType.STOP) {

                reservationPrice =
                        order.getStopPrice();
            }

            if (reservationPrice != null) {

                BigDecimal amountToRelease =
                        reservationPrice.multiply(
                                BigDecimal.valueOf(
                                        remainingQuantity
                                )
                        );

                BigDecimal reservedBalance =
                        user.getReservedBalance();

                if (reservedBalance == null) {
                    reservedBalance =
                            BigDecimal.ZERO;
                }

                BigDecimal newReservedBalance =
                        reservedBalance.subtract(
                                amountToRelease
                        );

                if (newReservedBalance.compareTo(
                        BigDecimal.ZERO
                ) < 0) {

                    newReservedBalance =
                            BigDecimal.ZERO;
                }

                user.setReservedBalance(
                        newReservedBalance
                );

                userRepository.save(user);
            }
        }

        // =========================================================
        // RELEASE SELL RESERVATION
        // =========================================================

        if (order.getSide() == OrderSide.SELL &&
                remainingQuantity > 0) {

            Instrument instrument =
                    order.getInstrument();

            if (instrument == null) {

                throw new IllegalArgumentException(
                        "Order instrument not found"
                );
            }

            Stock stock =
                    instrument.getStock();

            if (stock == null) {

                throw new IllegalArgumentException(
                        "Instrument is not linked to a stock"
                );
            }

            Holding holding =
                    holdingRepository
                            .findByUserAndStock(
                                    user,
                                    stock
                            )
                            .orElseThrow(() ->
                                    new IllegalArgumentException(
                                            "Holding not found for "
                                                    + stock.getSymbol()
                                    )
                            );

            Integer currentReserved =
                    holding.getReservedQuantity();

            if (currentReserved == null) {
                currentReserved = 0;
            }

            int newReservedQuantity =
                    currentReserved
                            - remainingQuantity;

            if (newReservedQuantity < 0) {
                newReservedQuantity = 0;
            }

            holding.setReservedQuantity(
                    newReservedQuantity
            );

            holdingRepository.saveAndFlush(
                    holding
            );
        }

        // =========================================================
        // CANCEL ORDER
        // =========================================================

        order.setStatus(
                OrderStatus.CANCELLED
        );

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        orderRepository.saveAndFlush(
                order
        );

        return toResponse(order);
    }

    public OrderResponse getOrder(
            String userEmail,
            Long orderId
    ) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        Order order =
                orderRepository
                        .findById(orderId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Order not found: " + orderId
                                )
                        );

        if (order.getUser() == null ||
                !order.getUser()
                        .getId()
                        .equals(user.getId())) {

            throw new IllegalArgumentException(
                    "You are not allowed to view this order"
            );
        }

        return toResponse(order);
    }

    // =========================================================
    // GET USER ORDERS
    // =========================================================

    public List<OrderResponse> getUserOrders(
            String userEmail
    ) {

        User user =
                userRepository
                        .findByEmail(userEmail)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        return orderRepository
                .findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private OrderResponse toResponse(
            Order order
    ) {

        return new OrderResponse(
                order.getId(),
                order.getInstrument().getSymbol(),
                order.getInstrument().getName(),
                order.getSide(),
                order.getType(),
                order.getStatus(),
                order.getQuantity(),
                order.getFilledQuantity(),
                order.getLimitPrice(),
                order.getStopPrice(),
                order.getAverageFillPrice(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}