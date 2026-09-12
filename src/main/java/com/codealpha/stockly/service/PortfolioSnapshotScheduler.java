package com.codealpha.stockly.service;

import com.codealpha.stockly.entity.Holding;
import com.codealpha.stockly.entity.PortfolioSnapshot;
import com.codealpha.stockly.entity.User;
import com.codealpha.stockly.repository.HoldingRepository;
import com.codealpha.stockly.repository.PortfolioSnapshotRepository;
import com.codealpha.stockly.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PortfolioSnapshotScheduler {

    private final UserRepository userRepository;
    private final HoldingRepository holdingRepository;
    private final PortfolioSnapshotRepository portfolioSnapshotRepository;

    public PortfolioSnapshotScheduler(
            UserRepository userRepository,
            HoldingRepository holdingRepository,
            PortfolioSnapshotRepository portfolioSnapshotRepository
    ) {
        this.userRepository = userRepository;
        this.holdingRepository = holdingRepository;
        this.portfolioSnapshotRepository =
                portfolioSnapshotRepository;
    }

    /*
     * Take a portfolio snapshot every 15 minutes.
     *
     * fixedRate = 900000 milliseconds = 15 minutes
     */
    @Scheduled(
            initialDelay = 900000,
            fixedRate = 900000
    )
    @Transactional
    public void createPortfolioSnapshots() {

        List<User> users =
                userRepository.findAll();

        for (User user : users) {

            List<Holding> holdings =
                    holdingRepository.findByUser(user);

            BigDecimal portfolioValue =
                    BigDecimal.ZERO;

            for (Holding holding : holdings) {

                BigDecimal currentPrice =
                        holding.getStock()
                                .getCurrentPrice();

                BigDecimal quantity =
                        BigDecimal.valueOf(
                                holding.getQuantity()
                        );

                BigDecimal currentValue =
                        currentPrice.multiply(quantity);

                portfolioValue =
                        portfolioValue.add(
                                currentValue
                        );
            }

            PortfolioSnapshot snapshot =
                    new PortfolioSnapshot();

            snapshot.setUser(user);

            snapshot.setPortfolioValue(
                    portfolioValue.setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
            );

            snapshot.setRecordedAt(
                    LocalDateTime.now()
            );

            portfolioSnapshotRepository.save(
                    snapshot
            );
        }
    }
}