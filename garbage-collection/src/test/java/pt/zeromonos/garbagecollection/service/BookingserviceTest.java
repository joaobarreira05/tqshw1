package pt.zeromonos.garbagecollection.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import jakarta.persistence.EntityNotFoundException;
import pt.zeromonos.garbagecollection.domain.BookingRequest;
import pt.zeromonos.garbagecollection.domain.BookingStatus;
import pt.zeromonos.garbagecollection.domain.TimeSlot;
import pt.zeromonos.garbagecollection.dto.BookingRequestDTO;
import pt.zeromonos.garbagecollection.repository.BookingRequestRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

// Ativa a integração do Mockito com o JUnit 5
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookingServiceTest {

    // Cria um mock (uma versão falsa) do repositório.
    @Mock
    private BookingRequestRepository bookingRepository;

    // Cria um mock do nosso serviço de API externa.
    @Mock
    private GeoApiService geoApiService;

    // Cria uma instância real do BookingService e injecta os mocks acima nele.
    @InjectMocks
    private BookingService bookingService;

    // -- Teste 1: Caminho Feliz (Happy Path) --
    // Testa a criação de um agendamento com dados válidos.
    @Test
    void whenCreateBooking_withValidData_thenBookingIsSaved() {
        // 1. Arrange (Preparar)
        // Criamos os dados de entrada para o teste.
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setMunicipality("Lisboa");
        dto.setItemDescription("Uma secretária");
        dto.setBookingDate(LocalDate.now().plusDays(5)); // Uma data futura
        dto.setTimeSlot(TimeSlot.MORNING);

        // Preparamos o que os nossos mocks devem fazer.
        // Dizemos ao mock do GeoApiService para retornar uma lista válida de municípios.
    when(geoApiService.getMunicipalities()).thenReturn(List.of("Lisboa", "Porto"));

    // Dizemos ao mock do repositório para retornar o próprio objeto que recebeu ao ser guardado.
    // isA garante que o argumento não é nulo.
    when(bookingRepository.save(any(BookingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // 2. Act (Agir)
        // Executamos o método que queremos testar.
        BookingRequest result = bookingService.createBooking(dto);

        // 3. Assert (Verificar)
        // Verificamos se o resultado é o esperado.
        assertNotNull(result); // O resultado não deve ser nulo.
        assertEquals("Lisboa", result.getMunicipality()); // O município deve ser o que enviámos.
        assertNotNull(result.getBookingToken()); // O token deve ter sido gerado.

        // Verificamos se os nossos mocks foram chamados como esperado.
        // Garante que o método getMunicipalities() foi chamado exatamente 1 vez.
        verify(geoApiService, times(1)).getMunicipalities();
        // Garante que o método save() foi chamado exatamente 1 vez.
        verify(bookingRepository, times(1)).save(any(BookingRequest.class));
    }

    // -- Teste 2: Caminho Triste (Sad Path) --
    // Testa a criação de um agendamento com um município inválido.
    @Test
    void whenCreateBooking_withInvalidMunicipality_thenThrowException() {
        // 1. Arrange
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setMunicipality("Terra do Nunca"); // Município que não existe na lista.
        dto.setBookingDate(LocalDate.now().plusDays(5));
        
        // Configuramos o mock para retornar a lista de municípios válidos.
        when(geoApiService.getMunicipalities()).thenReturn(List.of("Lisboa", "Porto"));

        // 2. Act & 3. Assert
        // Verificamos se uma excepção do tipo IllegalArgumentException é lançada.
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(dto);
        });

        // Verificamos que o método save NUNCA foi chamado, porque a validação falhou antes.
        verify(bookingRepository, never()).save(any(BookingRequest.class));
    }

    // -- Teste 3: Outro Caminho Triste --
    // Testa a criação de um agendamento com uma data no passado.
    @Test
    void whenCreateBooking_withPastDate_thenThrowException() {
        // 1. Arrange
        BookingRequestDTO dto = new BookingRequestDTO();
        dto.setMunicipality("Lisboa");
        dto.setBookingDate(LocalDate.now().minusDays(1)); // Data no passado.

        // Configuramos o mock para o município ser válido, para passarmos essa validação.
        when(geoApiService.getMunicipalities()).thenReturn(List.of("Lisboa"));

        // 2. Act & 3. Assert
        assertThrows(IllegalArgumentException.class, () -> {
            bookingService.createBooking(dto);
        });

    // Mais uma vez, o save não deve ser chamado.
        verify(bookingRepository, never()).save(any(BookingRequest.class));
    }

    @Test
    void whenUpdateBookingStatus_withValidData_thenPersistChanges() {
        BookingRequest existingBooking = new BookingRequest(
                "Frigorífico",
                "Lisboa",
                "Rua das Flores, 123",
                LocalDate.now().plusDays(3),
                TimeSlot.MORNING
        );
        existingBooking.setId(1L);
        LocalDateTime originalLastUpdatedAt = existingBooking.getLastUpdatedAt();

        when(bookingRepository.findById(1L)).thenReturn(Optional.of(existingBooking));
        when(bookingRepository.save(existingBooking)).thenReturn(existingBooking);

        BookingRequest updatedBooking = bookingService.updateBookingStatus(1L, BookingStatus.COMPLETED);

        assertEquals(BookingStatus.COMPLETED, updatedBooking.getStatus());
        assertNotNull(updatedBooking.getLastUpdatedAt());
        assertTrue(updatedBooking.getLastUpdatedAt().isAfter(originalLastUpdatedAt));

        verify(bookingRepository).findById(1L);
        verify(bookingRepository).save(existingBooking);
    }

    @Test
    void whenUpdateBookingStatus_withNullStatus_thenThrowException() {
        assertThrows(IllegalArgumentException.class, () ->
                bookingService.updateBookingStatus(1L, null)
        );

        verify(bookingRepository, never()).findById(anyLong());
        verify(bookingRepository, never()).save(any(BookingRequest.class));
    }

    @Test
    void whenUpdateBookingStatus_withUnknownBooking_thenThrowEntityNotFound() {
        when(bookingRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () ->
                bookingService.updateBookingStatus(99L, BookingStatus.COMPLETED)
        );

        verify(bookingRepository).findById(99L);
        verify(bookingRepository, never()).save(any(BookingRequest.class));
    }
}