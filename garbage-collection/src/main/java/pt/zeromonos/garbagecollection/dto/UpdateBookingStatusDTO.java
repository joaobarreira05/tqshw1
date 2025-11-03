package pt.zeromonos.garbagecollection.dto;

import pt.zeromonos.garbagecollection.domain.BookingStatus;

public class UpdateBookingStatusDTO {

    private BookingStatus status;

    public BookingStatus getStatus() {
        return status;
    }

    public void setStatus(BookingStatus status) {
        this.status = status;
    }
}
