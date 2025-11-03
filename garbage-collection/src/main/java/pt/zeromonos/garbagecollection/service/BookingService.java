package pt.zeromonos.garbagecollection.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.zeromonos.garbagecollection.domain.BookingRequest;
import pt.zeromonos.garbagecollection.domain.BookingStatus;
import pt.zeromonos.garbagecollection.dto.BookingRequestDTO;
import pt.zeromonos.garbagecollection.repository.BookingRequestRepository;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.List;
import java.util.Optional;

@Service
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);

    @Autowired
    private BookingRequestRepository bookingRepository;

    @Autowired
    private GeoApiService geoApiService;

    public List<String> getAvailableMunicipalities() {
        return geoApiService.getMunicipalities();
    }

    public BookingRequest createBooking(BookingRequestDTO dto) {
        // 1. Validar os dados
        List<String> validMunicipalities = geoApiService.getMunicipalities();
        if (dto.getMunicipality() == null || !validMunicipalities.contains(dto.getMunicipality())) {
            logger.warn("Attempt to create booking with invalid municipality: {}", dto.getMunicipality());
            throw new IllegalArgumentException("Municipality not available for service or is null: " + dto.getMunicipality());
        }

        if (dto.getBookingDate() == null || dto.getBookingDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Booking date must be today or in the future.");
        }

        // 2. Criar a entidade a partir do DTO
        BookingRequest newBooking = new BookingRequest(
                dto.getItemDescription(),
                dto.getMunicipality(),
                dto.getFullAddress(),
                dto.getBookingDate(),
                dto.getTimeSlot()
        );

        // 3. Guardar na base de dados
        BookingRequest savedBooking = bookingRepository.save(newBooking);
        logger.info("New booking created with token: {}", savedBooking.getBookingToken());

        return savedBooking;
    }

    public Optional<BookingRequest> findBookingByToken(String token) {
        return bookingRepository.findByBookingToken(token);
    }

    public List<BookingRequest> findBookingsByMunicipality(String municipality) {
        return bookingRepository.findByMunicipality(municipality);
    }

    public BookingRequest updateBookingStatus(Long bookingId, BookingStatus newStatus) {
        Objects.requireNonNull(bookingId, "Booking id cannot be null");

        if (newStatus == null) {
            throw new IllegalArgumentException("Booking status cannot be null");
        }

        BookingRequest booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new EntityNotFoundException("Booking with id " + bookingId + " not found"));

        booking.setStatus(newStatus);
        booking.setLastUpdatedAt(LocalDateTime.now());

        BookingRequest saved = bookingRepository.save(booking);
        logger.info("Booking {} status updated to {}", saved.getBookingToken(), saved.getStatus());
        return saved;
    }
}