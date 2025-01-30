import http from 'k6/http';
import { check, sleep } from 'k6';

// Get port from environment variable, default to 80 if not provided
const PORT = __ENV.PORT || '80';
const BASE_URL = `http://localhost:${PORT}`;

export const options = {
  // Test scenarios
  scenarios: {
    // Load test
    load_test: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '2m', target: 100 },  // Ramp up to 100 users
        { duration: '5m', target: 100 },  // Stay at 100 users
        { duration: '2m', target: 0 },    // Ramp down to 0 users
      ],
      gracefulRampDown: '30s',
    },
  },
  // Thresholds
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests should be below 2s
    http_req_failed: ['rate<0.01'],    // Less than 1% of requests should fail
  },
};

// Test endpoints
const ENDPOINTS = {
  getTransactions: '/transactions',
  createTransaction: '/transactions',
  getTransaction: (id) => `/transactions/${id}`,
  updateTransaction: (id) => `/transactions/${id}`,
};

// Sample transaction data
const sampleTransaction = {
  amount: 100.00,
  currency: 'USD',
  type: 'PAYMENT',
  status: 'PENDING',
  description: 'Test transaction'
};

export default function () {
  // Log the URL being used (only in verbose mode)
  console.log(`Testing against ${BASE_URL}`);

  // 1. Create a new transaction
  const createRes = http.post(
    `${BASE_URL}${ENDPOINTS.createTransaction}`,
    JSON.stringify(sampleTransaction),
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  check(createRes, {
    'Create transaction status is 201': (r) => r.status === 201,
    'Create response has transaction ID': (r) => r.json('id') !== undefined,
  });

  if (createRes.status === 201) {
    const transactionId = createRes.json('id');

    // 2. Get the created transaction
    const getRes = http.get(
      `${BASE_URL}${ENDPOINTS.getTransaction(transactionId)}`
    );
    
    check(getRes, {
      'Get transaction status is 200': (r) => r.status === 200,
      'Get response matches created transaction': (r) => r.json('id') === transactionId,
    });

    // 3. Update the transaction
    const updateData = {
      ...sampleTransaction,
      status: 'COMPLETED',
      description: 'Updated test transaction'
    };

    const updateRes = http.put(
      `${BASE_URL}${ENDPOINTS.updateTransaction(transactionId)}`,
      JSON.stringify(updateData),
      { headers: { 'Content-Type': 'application/json' } }
    );

    check(updateRes, {
      'Update transaction status is 200': (r) => r.status === 200,
    });
  }

  // 4. List transactions
  const listRes = http.get(`${BASE_URL}${ENDPOINTS.getTransactions}?page=0&size=10`);
  
  check(listRes, {
    'List transactions status is 200': (r) => r.status === 200,
    'List response has content': (r) => r.json('content') !== undefined,
  });

  // Wait between iterations
  sleep(1);
} 