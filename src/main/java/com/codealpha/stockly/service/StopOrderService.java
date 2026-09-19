package com.codealpha.stockly.service;

import com.codealpha.stockly.dto.MarketQuoteResponse;
import com.codealpha.stockly.entity.Instrument;
import com.codealpha.stockly.entity.Order;
import com.codealpha.stockly.entity.OrderSide;
import com.codealpha.stockly.entity.OrderStatus;
import com.codealpha.stockly.entity.OrderType;
import com.codealpha.stockly.repository.OrderRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StopOrderService {

    private final MarketQuoteService marketQuoteService;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public StopOrderService(
            OrderRepository orderRepository,
            OrderService orderService,
            MarketQuoteService marketQuoteService
    ) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.marketQuoteService = marketQuoteService;
    }

    @Transactional
    @Scheduled(fixedDelay = 2000)
    public void checkStopOrders() {

        /*
         * Load the Instrument together with the Order.
         *
         * Order.instrument is LAZY, and this scheduled method
         * needs the instrument's current market data.
         */
        List<Order> orders =
                orderRepository.findAllWithInstrument();

        System.out.println(
                "[STOP CHECK] Checking "
                        + orders.size()
                        + " orders"
        );

        for (Order order : orders) {

            if (order == null) {
                continue;
            }

            /*
             * Only OPEN orders can be triggered.
             */
            if (order.getStatus() != OrderStatus.OPEN) {
                continue;
            }

            /*
             * Only STOP and STOP_LIMIT orders
             * belong to this service.
             */
            if (order.getType() != OrderType.STOP &&
                    order.getType() != OrderType.STOP_LIMIT) {
                continue;
            }

            /*
             * IMPORTANT:
             *
             * A STOP_LIMIT becomes an active limit order
             * after its stop condition is triggered.
             *
             * It can remain OPEN while waiting for a matching
             * seller/buyer.
             *
             * Therefore, do NOT process it again as a stop order.
             */
            if (order.isStopTriggered()) {

                System.out.println(
                        "[STOP CHECK] Order "
                                + order.getId()
                                + " already triggered. "
                                + "Skipping stop check."
                );

                continue;
            }

            Instrument instrument =
                    order.getInstrument();

            if (instrument == null) {

                System.out.println(
                        "[STOP CHECK] Order "
                                + order.getId()
                                + " has no instrument"
                );

                continue;
            }

            MarketQuoteResponse quote;

            try {

                /*
                 * Use the exact Instrument rather than looking
                 * up only by symbol.
                 *
                 * This is important because different exchanges
                 * can contain the same symbol.
                 */
                quote =
                        marketQuoteService.getQuote(
                                instrument
                        );

            } catch (Exception e) {

                System.err.println(
                        "[STOP CHECK] Unable to get market quote for "
                                + instrument.getSymbol()
                                + ": "
                                + e.getMessage()
                );

                continue;
            }

            if (quote == null) {
                continue;
            }

            BigDecimal currentPrice =
                    quote.getCurrentPriceInr();

            BigDecimal stopPrice =
                    order.getStopPrice();

            System.out.println(
                    "[STOP CHECK] Order "
                            + order.getId()
                            + " | Symbol: "
                            + instrument.getSymbol()
                            + " | Side: "
                            + order.getSide()
                            + " | Type: "
                            + order.getType()
                            + " | Current INR: "
                            + currentPrice
                            + " | Stop INR: "
                            + stopPrice
            );

            /*
             * Ignore invalid current prices.
             */
            if (currentPrice == null ||
                    currentPrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                continue;
            }

            /*
             * Ignore invalid stop prices.
             */
            if (stopPrice == null ||
                    stopPrice.compareTo(
                            BigDecimal.ZERO
                    ) <= 0) {

                continue;
            }

            boolean triggered;

            /*
             * BUY STOP:
             *
             * Trigger when market price reaches
             * or goes above the stop price.
             */
            if (order.getSide() == OrderSide.BUY) {

                triggered =
                        currentPrice.compareTo(
                                stopPrice
                        ) >= 0;

            } else {

                /*
                 * SELL STOP:
                 *
                 * Trigger when market price reaches
                 * or falls below the stop price.
                 */
                triggered =
                        currentPrice.compareTo(
                                stopPrice
                        ) <= 0;
            }

            /*
             * Stop condition has not been reached yet.
             */
            if (!triggered) {
                continue;
            }

            System.out.println(
                    "[STOP TRIGGERED] Order "
                            + order.getId()
                            + " | Current price: "
                            + currentPrice
                            + " | Stop price: "
                            + stopPrice
            );

            try {

                /*
                 * Delegate actual trigger/execution logic
                 * to OrderService.
                 */
                orderService.triggerStopOrder(order);

                /*
                 * Reload the order so that we see the latest
                 * status after trigger processing.
                 */
                Order updatedOrder =
                        orderRepository
                                .findById(order.getId())
                                .orElse(order);

                /*
                 * A normal STOP order should execute immediately
                 * at the current market price.
                 */
                if (updatedOrder.getStatus() == OrderStatus.FILLED) {

                    System.out.println(
                            "[STOP FILLED] Order "
                                    + updatedOrder.getId()
                    );

                }

                /*
                 * A STOP_LIMIT order has been activated but
                 * does not necessarily execute immediately.
                 *
                 * It is now an active limit order waiting for
                 * the matching engine.
                 */
                else if (updatedOrder.isStopTriggered()) {

                    System.out.println(
                            "[STOP LIMIT ACTIVE] Order "
                                    + updatedOrder.getId()
                                    + " | Limit price: "
                                    + updatedOrder.getLimitPrice()
                    );
                }

            } catch (Exception e) {

                System.err.println(
                        "[STOP ERROR] Order "
                                + order.getId()
                                + " failed: "
                                + e.getMessage()
                );

                e.printStackTrace();
            }
        }
    }
}