package pt.zeromonos.garbagecollection.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Data // Anotação do Lombok: cria getters, setters, toString, equals, hashCode
@NoArgsConstructor // Anotação do Lombok: cria um construtor sem argumentos
public class BookingRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String bookingToken;

    @Column(nullable = false)
    private String itemDescription;

    @Column(nullable = false)
    private String municipality;

    private String fullAddress;

    @Column(nullable = false)
    private LocalDate bookingDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private TimeSlot timeSlot;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime lastUpdatedAt;

    // Construtor para facilitar a criação de novos bookings
    public BookingRequest(String itemDescription, String municipality, String fullAddress, LocalDate bookingDate, TimeSlot timeSlot) {
        this.itemDescription = itemDescription;
        this.municipality = municipality;
        this.fullAddress = fullAddress;
        this.bookingDate = bookingDate;
        this.timeSlot = timeSlot;
        
        // Valores padrão na criação
        this.bookingToken = UUID.randomUUID().toString();
        this.status = BookingStatus.RECEIVED;
        this.createdAt = LocalDateTime.now();
        this.lastUpdatedAt = this.createdAt;
    }
}