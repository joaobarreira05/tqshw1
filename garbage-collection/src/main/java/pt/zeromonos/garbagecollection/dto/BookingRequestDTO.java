package pt.zeromonos.garbagecollection.dto;

import lombok.Data;
import pt.zeromonos.garbagecollection.domain.TimeSlot;
import java.time.LocalDate;

@Data
public class BookingRequestDTO {
    private String itemDescription;
    private String municipality;
    private String fullAddress;
    private LocalDate bookingDate;
    private TimeSlot timeSlot;
}