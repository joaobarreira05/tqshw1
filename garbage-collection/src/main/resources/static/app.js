// Executa o nosso código quando o HTML da página estiver completamente carregado.
document.addEventListener('DOMContentLoaded', () => {

    // --- Elementos do DOM que vamos manipular ---
    const municipalitySelect = document.getElementById('municipality');
    const bookingForm = document.getElementById('bookingForm');
    const resultDiv = document.getElementById('result');
    const statusForm = document.getElementById('statusForm');
    const statusResultDiv = document.getElementById('statusResult');

    // --- Lógica Principal ---

    // 1. Carregar os municípios assim que a página abre.
    fetch('/api/bookings/municipalities')
        .then(response => response.json()) // Converte a resposta para JSON.
        .then(municipalities => {
            municipalitySelect.innerHTML = '<option value="">Selecione um município</option>'; // Limpa a opção "A carregar...".
            municipalities.forEach(mun => {
                const option = document.createElement('option');
                option.value = mun;
                option.textContent = mun;
                municipalitySelect.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Erro ao carregar municípios:', error);
            municipalitySelect.innerHTML = '<option value="">Não foi possível carregar</option>';
        });

    // 2. Lidar com a submissão do formulário de agendamento.
    bookingForm.addEventListener('submit', event => {
        event.preventDefault(); // Impede que a página recarregue ao submeter.

        // Recolhe os dados do formulário.
        const formData = new FormData(bookingForm);
        const data = Object.fromEntries(formData.entries());

        // Faz o pedido POST para a nossa API.
        fetch('/api/bookings', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(data), // Converte o nosso objeto de dados para uma string JSON.
        })
        .then(response => {
            if (!response.ok) {
                // Se a resposta não for 2xx (e.g., 400 Bad Request), lança um erro.
                throw new Error('Falha no agendamento. Verifique os dados (e.g., a data é no futuro?).');
            }
            return response.json();
        })
        .then(booking => {
            // Mostra a mensagem de sucesso com o token.
            resultDiv.style.display = 'block';
            resultDiv.innerHTML = `
                <h3>Agendamento realizado com sucesso!</h3>
                <p>Guarde o seu código de consulta: <strong>${booking.bookingToken}</strong></p>
            `;
            bookingForm.reset(); // Limpa o formulário.
        })
        .catch(error => {
            resultDiv.style.display = 'block';
            resultDiv.innerHTML = `<p style="color: red;">${error.message}</p>`;
        });
    });

    // 3. Lidar com a consulta de estado do agendamento.
    statusForm.addEventListener('submit', event => {
        event.preventDefault();
        const token = document.getElementById('bookingToken').value;

        if (!token) return;

        fetch(`/api/bookings/token/${token}`)
            .then(response => {
                if (response.status === 404) {
                    throw new Error('Código de agendamento não encontrado.');
                }
                if (!response.ok) {
                    throw new Error('Ocorreu um erro ao consultar.');
                }
                return response.json();
            })
            .then(booking => {
                statusResultDiv.style.display = 'block';
                statusResultDiv.innerHTML = `
                    <h3>Detalhes do Agendamento</h3>
                    <p><strong>Município:</strong> ${booking.municipality}</p>
                    <p><strong>Descrição:</strong> ${booking.itemDescription}</p>
                    <p><strong>Data:</strong> ${booking.bookingDate}</p>
                    <p><strong>Estado:</strong> ${booking.status}</p>
                `;
            })
            .catch(error => {
                statusResultDiv.style.display = 'block';
                statusResultDiv.innerHTML = `<p style="color: red;">${error.message}</p>`;
            });
    });
});