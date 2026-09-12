package com.codealpha.stockly.service;

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

    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public StopOrderService(
            OrderRepository orderRepository,
            OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @Transactional
    @Scheduled(fixedDelay = 2000)
    public void checkStopOrders() {

        /*
         * IMPORTANT:
         * Load the Instrument together with the Order.
         *
         * Order.instrument is LAZY, and this scheduled method
         * needs the instrument's current price.
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

            BigDecimal currentPrice =
                    instrument.getCurrentPrice();

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
                            + " | Current: "
                            + currentPrice
                            + " | Stop: "
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

                orderService.triggerStopOrder(order);

                System.out.println(
                        "[STOP FILLED] Order "
                                + order.getId()
                );

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