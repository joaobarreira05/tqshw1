package pt.zeromonos.garbagecollection.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.zeromonos.garbagecollection.domain.BookingRequest;
import pt.zeromonos.garbagecollection.dto.BookingRequestDTO;
import pt.zeromonos.garbagecollection.dto.UpdateBookingStatusDTO;
import pt.zeromonos.garbagecollection.service.BookingService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bookings") // Prefixo para todos os endpoints nesta classe
public class BookingController {

    @Autowired
    private BookingService bookingService;

    // Endpoint para obter a lista de municípios
    // GET http://localhost:8080/api/bookings/municipalities
    @GetMapping("/municipalities")
    public ResponseEntity<List<String>> getMunicipalities() {
        List<String> municipalities = bookingService.getAvailableMunicipalities();
        return new ResponseEntity<>(municipalities, HttpStatus.OK);
    }

    // Endpoint para criar um novo agendamento
    // POST http://localhost:8080/api/bookings
    @PostMapping
    public ResponseEntity<BookingRequest> createBooking(@RequestBody BookingRequestDTO bookingDto) {
        try {
            BookingRequest createdBooking = bookingService.createBooking(bookingDto);
            return new ResponseEntity<>(createdBooking, HttpStatus.CREATED); // 201 Created
        } catch (IllegalArgumentException e) {
            // Se o serviço lançar uma excepção (e.g., município inválido), retornamos um erro
            return ResponseEntity.badRequest().build(); // 400 Bad Request
        }
    }

    // Endpoint para consultar um agendamento pelo token
    // GET http://localhost:8080/api/bookings/token/some-uuid-token
    @GetMapping("/token/{token}")
    public ResponseEntity<BookingRequest> getBookingByToken(@PathVariable String token) {
        Optional<BookingRequest> booking = bookingService.findBookingByToken(token);

        // 'map' é uma forma elegante de lidar com o Optional.
        // Se o booking existir, executa a primeira parte. Se não, a segunda.
        return booking.map(b -> new ResponseEntity<>(b, HttpStatus.OK))
                      .orElse(new ResponseEntity<>(HttpStatus.NOT_FOUND)); // 404 Not Found
    }

    // Endpoint para a equipa (staff) ver os agendamentos por município
    // GET http://localhost:8080/api/bookings/staff/Lisboa
    @GetMapping("/staff/{municipality}")
    public ResponseEntity<List<BookingRequest>> getBookingsForStaff(@PathVariable String municipality) {
        List<BookingRequest> bookings = bookingService.findBookingsByMunicipality(municipality);
        return new ResponseEntity<>(bookings, HttpStatus.OK);
    }

    @PatchMapping("/staff/{bookingId}/status")
    public ResponseEntity<BookingRequest> updateBookingStatus(@PathVariable Long bookingId,
                                                              @RequestBody UpdateBookingStatusDTO updateBookingStatusDTO) {
        try {
            BookingRequest updatedBooking = bookingService.updateBookingStatus(bookingId, updateBookingStatusDTO.getStatus());
            return ResponseEntity.ok(updatedBooking);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }
}