const fs = require('fs');

const BASE_URL = 'http://localhost:8080/api';
// Need ADMIN token for reading all orders
const AUTH_TOKEN = 'YOUR_ADMIN_BEARER_TOKEN';

const CONCURRENCY_LEVEL = 50;

async function getOrders(index) {
    const startTime = Date.now();
    try {
        const response = await fetch(`${BASE_URL}/orders?page=0&size=10`, {
            method: 'GET',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${AUTH_TOKEN}`
            }
        });

        const data = await response.json();
        const endTime = Date.now();
        const duration = endTime - startTime;

        if (response.ok && data.status) {
            console.log(`[Req ${index}] SUCCESS (${duration}ms) - Retrieved ${data.data.content?.length} orders`);
            return { index, success: true, duration, status: response.status };
        } else {
            console.log(`[Req ${index}] FAILED (${duration}ms) - Status: ${response.status}`);
            return { index, success: false, duration, status: response.status };
        }
    } catch (error) {
        const duration = Date.now() - startTime;
        console.log(`[Req ${index}] ERROR (${duration}ms) - ${error.message}`);
        return { index, success: false, duration, error: error.message };
    }
}

async function runLoadTest() {
    console.log(`🚀 Starting concurrent read load test with ${CONCURRENCY_LEVEL} requests...\n`);

    const startTime = Date.now();
    const promises = [];

    for (let i = 1; i <= CONCURRENCY_LEVEL; i++) {
        promises.push(getOrders(i));
    }

    const results = await Promise.all(promises);
    const totalTime = Date.now() - startTime;

    const successful = results.filter(r => r.success).length;
    const failed = results.filter(r => !r.success).length;
    const avgDuration = results.reduce((acc, curr) => acc + curr.duration, 0) / CONCURRENCY_LEVEL;

    console.log(`\n======================================`);
    console.log(`📊 READ LOAD TEST REPORT`);
    console.log(`======================================`);
    console.log(`Total Requests:      ${CONCURRENCY_LEVEL}`);
    console.log(`Successful:          ${successful}`);
    console.log(`Failed/Errored:      ${failed}`);
    console.log(`Average Latency:     ${avgDuration.toFixed(2)} ms`);
    console.log(`Total Time Taken:    ${totalTime} ms`);
    console.log(`Requests/sec:        ${((CONCURRENCY_LEVEL / totalTime) * 1000).toFixed(2)} req/s`);
}

if (AUTH_TOKEN === 'YOUR_ADMIN_BEARER_TOKEN') {
    console.error("❌ ERROR: Please update 'AUTH_TOKEN' in scripts/concurrent_orders_read_test.js before running.");
    process.exit(1);
}

runLoadTest();

