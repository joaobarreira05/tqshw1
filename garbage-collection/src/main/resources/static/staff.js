document.addEventListener('DOMContentLoaded', () => {
    const municipalitySelect = document.getElementById('municipalitySelect');
    const bookingsTbody = document.getElementById('bookingsTbody');

    const STATUS_OPTIONS = [
        { value: 'RECEIVED', label: 'Recebido' },
        { value: 'SCHEDULED', label: 'Agendado' },
        { value: 'IN_PROGRESS', label: 'Em execução' },
        { value: 'COMPLETED', label: 'Concluído' },
        { value: 'CANCELLED', label: 'Cancelado' }
    ];

    const STATUS_LABELS = STATUS_OPTIONS.reduce((map, option) => {
        map[option.value] = option.label;
        return map;
    }, {});

    loadMunicipalities();

    municipalitySelect.addEventListener('change', () => {
        const selectedMunicipality = municipalitySelect.value;
        bookingsTbody.innerHTML = '';

        if (!selectedMunicipality) {
            return;
        }

        loadBookingsForMunicipality(selectedMunicipality);
    });

    function formatStatus(status) {
        return STATUS_LABELS[status] || status || 'Desconhecido';
    }

    async function loadMunicipalities() {
        try {
            const response = await fetch('/api/bookings/municipalities');
            const municipalities = await response.json();
            municipalitySelect.innerHTML = '<option value="">Selecione um município para ver os agendamentos</option>';
            municipalities.forEach(mun => {
                const option = document.createElement('option');
                option.value = mun;
                option.textContent = mun;
                municipalitySelect.appendChild(option);
            });
        } catch (error) {
            console.error('Erro ao carregar municípios:', error);
            municipalitySelect.innerHTML = '<option value="">Erro ao carregar municípios</option>';
        }
    }

    async function loadBookingsForMunicipality(municipality) {
        try {
            const response = await fetch(`/api/bookings/staff/${municipality}`);
            if (!response.ok) {
                throw new Error('Não foi possível carregar os agendamentos.');
            }
            const bookings = await response.json();

            if (!Array.isArray(bookings) || bookings.length === 0) {
                bookingsTbody.innerHTML = '<tr><td colspan="6">Não existem agendamentos para este município.</td></tr>';
                return;
            }

            bookingsTbody.innerHTML = '';
            bookings.forEach(booking => {
                const row = createBookingRow(booking);
                bookingsTbody.appendChild(row);
            });
        } catch (error) {
            console.error('Erro ao carregar agendamentos:', error);
            bookingsTbody.innerHTML = '<tr><td colspan="6" style="color: red;">Erro ao carregar dados.</td></tr>';
        }
    }

    function createBookingRow(booking) {
        const row = document.createElement('tr');
        row.dataset.bookingId = booking.id;

        const idCell = document.createElement('td');
        idCell.textContent = booking.id ?? '—';

        const descriptionCell = document.createElement('td');
        descriptionCell.textContent = booking.itemDescription || '—';

        const addressCell = document.createElement('td');
        addressCell.textContent = booking.fullAddress || '—';

        const dateCell = document.createElement('td');
        dateCell.textContent = booking.bookingDate || '—';

        const statusCell = document.createElement('td');
        statusCell.classList.add('status-cell');
        statusCell.textContent = formatStatus(booking.status);

        const actionsCell = document.createElement('td');
        actionsCell.classList.add('actions-cell');

        const statusSelect = document.createElement('select');
        STATUS_OPTIONS.forEach(option => {
            const opt = document.createElement('option');
            opt.value = option.value;
            opt.textContent = option.label;
            if (booking.status === option.value) {
                opt.selected = true;
            }
            statusSelect.appendChild(opt);
        });

        const updateButton = document.createElement('button');
        updateButton.type = 'button';
        updateButton.textContent = 'Atualizar';

        updateButton.addEventListener('click', async () => {
            const newStatus = statusSelect.value;
            await handleStatusUpdate(row, booking.id, newStatus, statusCell, statusSelect, updateButton);
        });

        actionsCell.appendChild(statusSelect);
        actionsCell.appendChild(updateButton);

        row.appendChild(idCell);
        row.appendChild(descriptionCell);
        row.appendChild(addressCell);
        row.appendChild(dateCell);
        row.appendChild(statusCell);
        row.appendChild(actionsCell);

        return row;
    }

    async function handleStatusUpdate(row, bookingId, newStatus, statusCell, statusSelect, updateButton) {
        if (!bookingId) {
            alert('Identificador do agendamento inválido.');
            return;
        }

        try {
            statusSelect.disabled = true;
            updateButton.disabled = true;
            updateButton.textContent = 'A atualizar...';

            const updatedBooking = await updateBookingStatus(bookingId, newStatus);
            statusCell.textContent = formatStatus(updatedBooking.status);
            statusSelect.value = updatedBooking.status;

            row.classList.remove('status-updated');
            // Força reflow para reiniciar a animação
            void row.offsetWidth;
            row.classList.add('status-updated');
        } catch (error) {
            alert(error.message || 'Não foi possível atualizar o estado.');
        } finally {
            statusSelect.disabled = false;
            updateButton.disabled = false;
            updateButton.textContent = 'Atualizar';
        }
    }

    async function updateBookingStatus(bookingId, status) {
        const response = await fetch(`/api/bookings/staff/${bookingId}/status`, {
            method: 'PATCH',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ status })
        });

        if (!response.ok) {
            let errorMessage = 'Não foi possível atualizar o estado.';
            try {
                const data = await response.json();
                if (data && typeof data === 'object' && data.message) {
                    errorMessage = data.message;
                }
            } catch (jsonError) {
                try {
                    const textData = await response.text();
                    if (textData) {
                        errorMessage = textData;
                    }
                } catch (_) {
                    // ignore parsing errors
                }
            }
            throw new Error(errorMessage);
        }

        return response.json();
    }
});