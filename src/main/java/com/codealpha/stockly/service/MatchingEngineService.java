package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Execution;
import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Order;
import com.codealpha.stockly.entity.OrderSide;
import com.codealpha.stockly.entity.OrderStatus;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.ExecutionRepository;
import com.codealpha.stockly.repository.HoldingRepository;
import com.codealpha.stockly.repository.OrderRepository;
import com.codealpha.stockly.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
public class MatchingEngineService {

    private final OrderRepository orderRepository;
    private final ExecutionRepository executionRepository;
    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;

    public MatchingEngineService(
            OrderRepository orderRepository,
            ExecutionRepository executionRepository,
            UserRepository userRepository,
            HoldingRepository holdingRepository
    ) {
        this.orderRepository = orderRepository;
        this.executionRepository = executionRepository;
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
    }

    // =========================================================
    // MATCH ORDER
    // =========================================================

    @Transactional
    public void match(Order incomingOrder) {

        if (incomingOrder == null) {
            return;
        }

        if (incomingOrder.getStatus() != OrderStatus.OPEN &&
                incomingOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) {
            return;
        }

        if (incomingOrder.getInstrument() == null) {
            throw new IllegalArgumentException(
                    "Order instrument is required"
            );
        }

        if (incomingOrder.getLimitPrice() == null ||
                incomingOrder.getLimitPrice()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Limit price is required for matching"
            );
        }

        Instrument instrument =
                incomingOrder.getInstrument();

        /*
         * Determine opposite side.
         *
         * BUY  -> match SELL orders
         * SELL -> match BUY orders
         */

        OrderSide oppositeSide =
                incomingOrder.getSide() == OrderSide.BUY
                        ? OrderSide.SELL
                        : OrderSide.BUY;

        List<Order> oppositeOrders =
                orderRepository
                        .findByInstrumentAndSideAndStatusOrderByCreatedAtAsc(
                                instrument,
                                oppositeSide,
                                OrderStatus.OPEN
                        );

                List<Order> partiallyFilledOrders =
                        orderRepository
                                .findByInstrumentAndSideAndStatusOrderByCreatedAtAsc(
                                        instrument,
                                        oppositeSide,
                                        OrderStatus.PARTIALLY_FILLED
                                );

                oppositeOrders.addAll(partiallyFilledOrders);

        // =========================================================
        // REMOVE ORDERS THAT CANNOT PARTICIPATE IN LIMIT MATCHING
        // =========================================================

        oppositeOrders.removeIf(order ->
                order.getLimitPrice() == null
        );
        /*
         * Price-time priority.
         *
         * For BUY incoming:
         *     cheapest SELL first
         *
         * For SELL incoming:
         *     highest BUY first
         */

        Comparator<Order> comparator;

        if (oppositeSide == OrderSide.SELL) {

            comparator =
                    Comparator
                            .comparing(
                                    Order::getLimitPrice
                            )
                            .thenComparing(
                                    Order::getCreatedAt
                            );

        } else {

            comparator =
                    Comparator
                            .comparing(
                                    Order::getLimitPrice
                            )
                            .reversed()
                            .thenComparing(
                                    Order::getCreatedAt
                            );
        }

        oppositeOrders.sort(comparator);

        /*
         * Try to match the incoming order
         * against each compatible order.
         */

        for (Order oppositeOrder : oppositeOrders) {

            if (oppositeOrder.getUser().getId()
                    .equals(incomingOrder.getUser().getId())) {

                continue;
            }

            if (incomingOrder.getStatus() != OrderStatus.OPEN &&
                    incomingOrder.getStatus() != OrderStatus.PARTIALLY_FILLED) {

                break;
            }

            /*
             * Do not match an order against itself.
             */

            if (oppositeOrder.getId() != null &&
                    oppositeOrder.getId()
                            .equals(incomingOrder.getId())) {

                continue;
            }

            /*
             * Do not allow users to trade against
             * their own orders.
             */

            if (oppositeOrder.getUser() != null &&
                    incomingOrder.getUser() != null &&
                    oppositeOrder.getUser().getId()
                            .equals(incomingOrder.getUser().getId())) {

                continue;
            }

            /*
             * Check whether prices cross.
             */

            if (!pricesCross(
                    incomingOrder,
                    oppositeOrder
            )) {

                /*
                 * Because orders are sorted by price,
                 * no later order can match either.
                 */

                break;
            }

            executeMatch(
                    incomingOrder,
                    oppositeOrder
            );
        }
    }

    // =========================================================
    // PRICE MATCHING
    // =========================================================

    private boolean pricesCross(
            Order incoming,
            Order opposite
    ) {

        BigDecimal incomingPrice =
                incoming.getLimitPrice();

        BigDecimal oppositePrice =
                opposite.getLimitPrice();

        if (incomingPrice == null ||
                oppositePrice == null) {

            return false;
        }

        /*
         * BUY LIMIT:
         *
         * BUY price must be >= SELL price.
         */

        if (incoming.getSide() == OrderSide.BUY) {

            return incomingPrice.compareTo(
                    oppositePrice
            ) >= 0;
        }

        /*
         * SELL LIMIT:
         *
         * SELL price must be <= BUY price.
         */

        return incomingPrice.compareTo(
                oppositePrice
        ) <= 0;
    }

    // =========================================================
    // EXECUTE MATCH
    // =========================================================

    private void executeMatch(
            Order incoming,
            Order opposite
    ) {

        int incomingRemaining =
                getRemainingQuantity(incoming);

        int oppositeRemaining =
                getRemainingQuantity(opposite);

        int executionQuantity =
                Math.min(
                        incomingRemaining,
                        oppositeRemaining
                );

        if (executionQuantity <= 0) {
            return;
        }

        /*
         * Trade executes at the price of the
         * RESTING order.
         *
         * This is standard price-time behavior
         * for our simulated exchange.
         */

        BigDecimal executionPrice =
                opposite.getLimitPrice();

        if (executionPrice == null ||
                executionPrice.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            throw new IllegalArgumentException(
                    "Invalid execution price"
            );
        }

        BigDecimal totalAmount =
                executionPrice.multiply(
                        BigDecimal.valueOf(
                                executionQuantity
                        )
                );

        /*
         * Determine buyer and seller.
         */

        Order buyOrder;
        Order sellOrder;

        if (incoming.getSide() == OrderSide.BUY) {

            buyOrder = incoming;
            sellOrder = opposite;

        } else {

            buyOrder = opposite;
            sellOrder = incoming;
        }

        /*
         * Make sure this really is
         * BUY vs SELL.
         */

        if (buyOrder.getSide() != OrderSide.BUY ||
                sellOrder.getSide() != OrderSide.SELL) {

            throw new IllegalArgumentException(
                    "Invalid order sides for matching"
            );
        }

        /*
         * Settle money and holdings.
         */

        settleTrade(
                buyOrder,
                sellOrder,
                executionQuantity,
                executionPrice,
                totalAmount
        );

        /*
         * Create execution records.
         */

        createExecution(
                buyOrder,
                executionQuantity,
                executionPrice,
                totalAmount
        );

        createExecution(
                sellOrder,
                executionQuantity,
                executionPrice,
                totalAmount
        );

        /*
         * Update both orders.
         */

        updateOrderAfterFill(
                buyOrder,
                executionQuantity,
                executionPrice
        );

        updateOrderAfterFill(
                sellOrder,
                executionQuantity,
                executionPrice
        );
    }

    // =========================================================
    // SETTLE TRADE
    // =========================================================

    private void settleTrade(
            Order buyOrder,
            Order sellOrder,
            int quantity,
            BigDecimal executionPrice,
            BigDecimal totalAmount
    ) {

        User buyer =
                buyOrder.getUser();

        User seller =
                sellOrder.getUser();

        if (buyer == null ||
                seller == null) {

            throw new IllegalArgumentException(
                    "Both orders must have users"
            );
        }

        // =====================================================
        // BUYER MONEY
        // =====================================================

        BigDecimal buyerBalance =
                buyer.getVirtualBalance();

        if (buyerBalance == null) {
            buyerBalance = BigDecimal.ZERO;
        }

        BigDecimal buyerReserved =
                buyer.getReservedBalance();

        if (buyerReserved == null) {
            buyerReserved = BigDecimal.ZERO;
        }

        /*
         * The BUY order reserved:
         *
         * quantity × limit price
         *
         * But the actual trade happens at:
         *
         * quantity × execution price
         *
         * Therefore we release the reserved
         * amount and deduct the actual amount.
         */

        BigDecimal reservedForExecution =
                buyOrder.getLimitPrice()
                        .multiply(
                                BigDecimal.valueOf(
                                        quantity
                                )
                        );

        if (buyerReserved.compareTo(
                reservedForExecution
        ) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient reserved balance for BUY order"
            );
        }

        /*
         * Actual cash payment.
         */

        if (buyerBalance.compareTo(
                totalAmount
        ) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient buyer balance"
            );
        }

        buyer.setVirtualBalance(
                buyerBalance.subtract(
                        totalAmount
                )
        );

        /*
         * Release reserved cash.
         */

        buyer.setReservedBalance(
                buyerReserved.subtract(
                        reservedForExecution
                )
        );

        userRepository.save(buyer);

        // =====================================================
        // SELLER MONEY
        // =====================================================

        BigDecimal sellerBalance =
                seller.getVirtualBalance();

        if (sellerBalance == null) {
            sellerBalance = BigDecimal.ZERO;
        }

        seller.setVirtualBalance(
                sellerBalance.add(
                        totalAmount
                )
        );

        userRepository.save(seller);

        // =====================================================
        // BUYER HOLDING
        // =====================================================

        Holding buyerHolding =
                findHolding(
                        buyer,
                        buyOrder.getInstrument()
                );

        if (buyerHolding == null) {

            buyerHolding =
                    new Holding();

            buyerHolding.setUser(
                    buyer
            );

            buyerHolding.setStock(
                    buyOrder
                            .getInstrument()
                            .getStock()
            );

            buyerHolding.setQuantity(
                    0
            );

            buyerHolding.setAverageBuyPrice(
                    executionPrice
            );

            buyerHolding.setReservedQuantity(
                    0
            );
        }

        int oldQuantity =
                buyerHolding.getQuantity();

        BigDecimal oldAverage =
                buyerHolding.getAverageBuyPrice();

        if (oldAverage == null) {
            oldAverage = BigDecimal.ZERO;
        }

        BigDecimal oldValue =
                oldAverage.multiply(
                        BigDecimal.valueOf(
                                oldQuantity
                        )
                );

        BigDecimal newValue =
                executionPrice.multiply(
                        BigDecimal.valueOf(
                                quantity
                        )
                );

        int newQuantity =
                oldQuantity + quantity;

        BigDecimal newAverage =
                oldValue
                        .add(newValue)
                        .divide(
                                BigDecimal.valueOf(
                                        newQuantity
                                ),
                                4,
                                RoundingMode.HALF_UP
                        );

        buyerHolding.setQuantity(
                newQuantity
        );

        buyerHolding.setAverageBuyPrice(
                newAverage
        );

        if (buyerHolding.getReservedQuantity() == null) {
            buyerHolding.setReservedQuantity(0);
        }

        holdingRepository.save(
                buyerHolding
        );

        // =====================================================
        // SELLER HOLDING
        // =====================================================

        Holding sellerHolding =
                findHolding(
                        seller,
                        sellOrder.getInstrument()
                );

        if (sellerHolding == null) {

            throw new IllegalArgumentException(
                    "Seller does not own this stock"
            );
        }

        int sellerQuantity =
                sellerHolding.getQuantity();

        if (sellerQuantity < quantity) {

            throw new IllegalArgumentException(
                    "Seller does not have enough shares"
            );
        }

        Integer sellerReservedValue =
                sellerHolding.getReservedQuantity();

        int sellerReserved =
                sellerReservedValue == null
                        ? 0
                        : sellerReservedValue;

        /*
         * The shares being sold must have been
         * reserved by the SELL order.
         */

        if (sellerReserved < quantity) {

            throw new IllegalArgumentException(
                    "Insufficient reserved shares for SELL order"
            );
        }

        /*
         * Remove sold shares.
         */

        int remainingQuantity =
                sellerQuantity - quantity;

        /*
         * Release reserved shares.
         */

        int remainingReserved =
                sellerReserved - quantity;

        sellerHolding.setReservedQuantity(
                remainingReserved
        );

        sellerHolding.setQuantity(
                remainingQuantity
        );

        /*
         * If no shares remain, delete the holding.
         */

        if (remainingQuantity == 0) {

            holdingRepository.delete(
                    sellerHolding
            );

        } else {

            holdingRepository.save(
                    sellerHolding
            );
        }
    }

    // =========================================================
    // FIND HOLDING
    // =========================================================

    private Holding findHolding(
            User user,
            Instrument instrument
    ) {

        if (user == null ||
                instrument == null ||
                instrument.getStock() == null) {

            return null;
        }

        return holdingRepository
                .findByUserAndStock(
                        user,
                        instrument.getStock()
                )
                .orElse(null);
    }

    // =========================================================
    // CREATE EXECUTION
    // =========================================================

    private void createExecution(
            Order order,
            int quantity,
            BigDecimal price,
            BigDecimal totalAmount
    ) {

        Execution execution =
                new Execution();

        execution.setOrder(
                order
        );

        execution.setQuantity(
                quantity
        );

        execution.setPrice(
                price
        );

        execution.setTotalAmount(
                totalAmount
        );

        execution.setExecutedAt(
                LocalDateTime.now()
        );

        executionRepository.save(
                execution
        );
    }

    // =========================================================
    // UPDATE ORDER AFTER FILL
    // =========================================================

    private void updateOrderAfterFill(
            Order order,
            int quantity,
            BigDecimal executionPrice
    ) {

        int oldFilledQuantity =
                order.getFilledQuantity();

        int newFilledQuantity =
                oldFilledQuantity + quantity;

        /*
         * Calculate weighted average execution price.
         *
         * Example:
         *
         * 2 shares @ 100
         * 3 shares @ 110
         *
         * average =
         * (200 + 330) / 5
         * = 106
         */

        BigDecimal oldAverage =
                order.getAverageFillPrice();

        if (oldAverage == null ||
                oldFilledQuantity == 0) {

            order.setAverageFillPrice(
                    executionPrice
            );

        } else {

            BigDecimal oldValue =
                    oldAverage.multiply(
                            BigDecimal.valueOf(
                                    oldFilledQuantity
                            )
                    );

            BigDecimal newValue =
                    executionPrice.multiply(
                            BigDecimal.valueOf(
                                    quantity
                            )
                    );

            BigDecimal weightedAverage =
                    oldValue
                            .add(newValue)
                            .divide(
                                    BigDecimal.valueOf(
                                            newFilledQuantity
                                    ),
                                    4,
                                    RoundingMode.HALF_UP
                            );

            order.setAverageFillPrice(
                    weightedAverage
            );
        }

        order.setFilledQuantity(
                newFilledQuantity
        );

        /*
         * Determine final status.
         */

        if (newFilledQuantity >=
                order.getQuantity()) {

            order.setStatus(
                    OrderStatus.FILLED
            );

        } else {

            order.setStatus(
                    OrderStatus.PARTIALLY_FILLED
            );
        }

        order.setUpdatedAt(
                LocalDateTime.now()
        );

        orderRepository.save(
                order
        );
    }

    // =========================================================
    // REMAINING QUANTITY
    // =========================================================

    private int getRemainingQuantity(
            Order order
    ) {

        int quantity =
                order.getQuantity();

        int filled =
                order.getFilledQuantity();

        return Math.max(
                0,
                quantity - filled
        );
    }
}