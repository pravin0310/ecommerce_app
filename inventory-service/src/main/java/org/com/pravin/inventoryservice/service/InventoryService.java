package org.com.pravin.inventoryservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.pravin.inventoryservice.entity.Inventory;
import org.com.pravin.inventoryservice.entity.StockReservation;
import org.com.pravin.inventoryservice.kafka.event.PaymentProcessedEvent;
import org.com.pravin.inventoryservice.repository.InventoryRepository;
import org.com.pravin.inventoryservice.repository.StockReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {
    private final InventoryRepository inventoryRepository;
    private final StockReservationRepository reservationRepository;

    /**
     * Reserve stock for all items atomically.
     * @Transactional ensures if ANY item fails,
     * ALL stock changes roll back automatically.
     */
    @Transactional
    public void reserveStock(Long orderId,
                             List<PaymentProcessedEvent.OrderItemDto> items) {

        for (PaymentProcessedEvent.OrderItemDto item : items) {

            // Pessimistic lock — SELECT FOR UPDATE
            // Prevents two orders reserving same stock at same time
            Inventory inventory = inventoryRepository
                    .findByProductIdWithLock(item.getProductId())
                    .orElseThrow(() -> new RuntimeException(
                            "Product not found in inventory: "
                                    + item.getProductId()));

            if (inventory.getAvailableQuantity() < item.getQuantity()) {
                throw new RuntimeException(
                        "Insufficient stock for productId="
                                + item.getProductId()
                                + ". Available: " + inventory.getAvailableQuantity()
                                + ", Requested: " + item.getQuantity());
            }

            // Deduct available, add to reserved
            inventory.setAvailableQuantity(
                    inventory.getAvailableQuantity() - item.getQuantity());
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() + item.getQuantity());
            inventoryRepository.save(inventory);

            // Record reservation for potential rollback later
            StockReservation reservation = StockReservation.builder()
                    .orderId(orderId)
                    .productId(item.getProductId())
                    .quantity(item.getQuantity())
                    .build();
            reservationRepository.save(reservation);

            log.info("Reserved {} units of productId={} for orderId={}",
                    item.getQuantity(), item.getProductId(), orderId);
        }
    }

    /**
     * Compensating transaction — called if order is cancelled after reservation.
     * Releases stock back to available.
     */
    @Transactional
    public void releaseStock(Long orderId) {
        List<StockReservation> reservations =
                reservationRepository.findByOrderId(orderId);

        for (StockReservation reservation : reservations) {
            Inventory inventory = inventoryRepository
                    .findByProductId(reservation.getProductId())
                    .orElseThrow();

            inventory.setAvailableQuantity(
                    inventory.getAvailableQuantity() + reservation.getQuantity());
            inventory.setReservedQuantity(
                    inventory.getReservedQuantity() - reservation.getQuantity());
            inventoryRepository.save(inventory);

            log.info("Released {} units of productId={} for orderId={}",
                    reservation.getQuantity(),
                    reservation.getProductId(), orderId);
        }

        reservationRepository.deleteAll(reservations);
    }

    public boolean isAlreadyReserved(Long orderId) {
        return reservationRepository.existsByOrderId(orderId);
    }
}
