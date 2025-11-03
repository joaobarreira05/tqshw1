import http from 'k6/http';
import { check, sleep } from 'k6';

// Opções do teste: escalar de 1 até 10 utilizadores virtuais e voltar a 1, durante 1 minuto.
// Isto simula um pico de utilização.
export const options = {
  stages: [
    { duration: '20s', target: 10 }, // Sobe para 10 utilizadores em 20s
    { duration: '20s', target: 10 }, // Mantém 10 utilizadores por 20s
    { duration: '20s', target: 0 },  // Desce para 0 utilizadores em 20s
  ],
  thresholds: {
    // Definimos um "Quality Gate" de performance:
    // 95% dos pedidos têm de ser mais rápidos que 800ms.
    http_req_duration: ['p(95)<800'], 
  },
};

const API_BASE_URL = 'http://localhost:8080/api/bookings';

// O código que cada utilizador virtual vai executar repetidamente.
export default function () {
  // Cada utilizador vai criar um agendamento único.
  const payload = JSON.stringify({
    itemDescription: `Item de teste de carga - VU=${__VU} ITER=${__ITER}`, // Garante um item único
    municipality: 'Lisboa',
    fullAddress: 'Rua do Teste de Carga, 123',
    bookingDate: '2026-12-01',
    timeSlot: 'MORNING',
  });

  const params = {
    headers: { 'Content-Type': 'application/json' },
  };

  const res = http.post(API_BASE_URL, payload, params);

  // Verificação: O pedido de criação teve sucesso (status 201 Created)?
  check(res, {
    'Status da criação foi 201': (r) => r.status === 201,
  });

  // Pequena pausa de 1 segundo para simular o comportamento de um utilizador real.
  sleep(1);
}