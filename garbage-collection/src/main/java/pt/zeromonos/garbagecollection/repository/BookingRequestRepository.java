package pt.zeromonos.garbagecollection.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.zeromonos.garbagecollection.domain.BookingRequest;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRequestRepository extends JpaRepository<BookingRequest, Long> {

    // O Spring Data JPA cria a query automaticamente a partir do nome do método!
    // "Encontra-me um BookingRequest através do seu campo 'bookingToken'"
    Optional<BookingRequest> findByBookingToken(String bookingToken);

    // "Encontra-me uma lista de BookingRequests através do seu campo 'municipality'"
    List<BookingRequest> findByMunicipality(String municipality);
}