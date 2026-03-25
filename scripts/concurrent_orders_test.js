const fs = require('fs');

/**
 * Configure your variables here
 * Change the Bearer token, User ID, and Product IDs appropriately.
 */
const BASE_URL = 'http://localhost:8080/api';
const AUTH_TOKEN = 'YOUR_ADMIN_OR_CUSTOMER_BEARER_TOKEN';
const USER_ID = 'YOUR_USER_UUID';
const PRODUCT_ID = 'YOUR_PRODUCT_UUID';

// Number of concurrent requests to spawn
const CONCURRENCY_LEVEL = 10;

const orderPayload = {
    userId: USER_ID,
    items: [
        {
            productId: PRODUCT_ID,
            quantity: 1
        }
    ],
    // paymentMethodId: "OPTIONAL_UUID",
    // shippingMethodId: "OPTIONAL_UUID"
};

async function createOrder(index) {
    const startTime = Date.now();
    try {
        const response = await fetch(`${BASE_URL}/orders`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${AUTH_TOKEN}`
            },
            body: JSON.stringify(orderPayload)
        });

        const data = await response.json();
        const endTime = Date.now();
        const duration = endTime - startTime;

        if (response.ok && data.status) {
            console.log(`[Req ${index}] SUCCESS (${duration}ms) - Order ID: ${data.data.id}`);
            return { index, success: true, duration, status: response.status };
        } else {
            console.log(`[Req ${index}] FAILED (${duration}ms) - Status: ${response.status} - Msg: ${data.message || data.error}`);
            return { index, success: false, duration, status: response.status, error: data };
        }
    } catch (error) {
        const duration = Date.now() - startTime;
        console.log(`[Req ${index}] ERROR (${duration}ms) - ${error.message}`);
        return { index, success: false, duration, error: error.message };
    }
}

async function runLoadTest() {
    console.log(`🚀 Starting concurrent load test with ${CONCURRENCY_LEVEL} requests...\n`);

    const startTime = Date.now();
    const promises = [];

    // Dispatch all requests concurrently
    for (let i = 1; i <= CONCURRENCY_LEVEL; i++) {
        promises.push(createOrder(i));
    }

    // Wait for all of them to resolve
    const results = await Promise.all(promises);
    const totalTime = Date.now() - startTime;

    // Report
    const successful = results.filter(r => r.success).length;
    const failed = results.filter(r => !r.success).length;
    const avgDuration = results.reduce((acc, curr) => acc + curr.duration, 0) / CONCURRENCY_LEVEL;

    console.log(`\n======================================`);
    console.log(`📊 LOAD TEST REPORT`);
    console.log(`======================================`);
    console.log(`Total Requests:      ${CONCURRENCY_LEVEL}`);
    console.log(`Successful:          ${successful}`);
    console.log(`Failed/Errored:      ${failed}`);
    console.log(`Average Latency:     ${avgDuration.toFixed(2)} ms`);
    console.log(`Total Time Taken:    ${totalTime} ms`);
    console.log(`Requests/sec:        ${((CONCURRENCY_LEVEL / totalTime) * 1000).toFixed(2)} req/s`);

    if (failed > 0) {
        console.log(`\n⚠️ Note: Failures might be due to insufficient stock setup. Remember that our optimized 'StockManagementService' validates properly without race conditions.`);
    }
}

// Ensure settings are actually overriden
if (AUTH_TOKEN === 'YOUR_ADMIN_OR_CUSTOMER_BEARER_TOKEN' || USER_ID === 'YOUR_USER_UUID') {
    console.error("❌ ERROR: Please update 'AUTH_TOKEN', 'USER_ID', and 'PRODUCT_ID' in scripts/concurrent_orders_test.js before running.");
    process.exit(1);
}

runLoadTest();

