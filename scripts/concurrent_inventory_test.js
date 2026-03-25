const fs = require('fs');

/**
 * Script to test concurrent inventory requests using parallel non-blocking calls.
 * Ensure your APIs have been refactored to use CompletableFuture.
 */
const BASE_URL = 'http://localhost:8080/api';
const AUTH_TOKEN = 'YOUR_ADMIN_BEARER_TOKEN';
const PRODUCT_ID = 'YOUR_PRODUCT_UUID';

const CONCURRENCY_LEVEL = 20;

async function fetchProducts(index) {
    const startTime = Date.now();
    try {
        const response = await fetch(`${BASE_URL}/products`, {
            method: 'GET',
            headers: {
                'Authorization': `Bearer ${AUTH_TOKEN}`
            }
        });

        const data = await response.json();
        const duration = Date.now() - startTime;

        if (response.ok && data.status) {
            console.log(`[GET Req ${index}] SUCCESS (${duration}ms)`);
            return { index, success: true, duration, status: response.status };
        } else {
            console.log(`[GET Req ${index}] FAILED (${duration}ms) - Status: ${response.status}`);
            return { index, success: false, duration, status: response.status, error: data };
        }
    } catch (error) {
        const duration = Date.now() - startTime;
        console.log(`[GET Req ${index}] ERROR (${duration}ms) - ${error.message}`);
        return { index, success: false, duration, error: error.message };
    }
}

async function updateStock(index) {
    const startTime = Date.now();
    try {
        const response = await fetch(`${BASE_URL}/products/${PRODUCT_ID}/stock?quantity=-1`, {
            method: 'PATCH',
            headers: {
                'Authorization': `Bearer ${AUTH_TOKEN}`
            }
        });

        const data = await response.json();
        const duration = Date.now() - startTime;

        if (response.ok && data.status) {
            console.log(`[PATCH Req ${index}] SUCCESS (${duration}ms)`);
            return { index, success: true, duration, status: response.status };
        } else {
            console.log(`[PATCH Req ${index}] FAILED (${duration}ms) - Status: ${response.status}`);
            return { index, success: false, duration, status: response.status, error: data };
        }
    } catch (error) {
        const duration = Date.now() - startTime;
        console.log(`[PATCH Req ${index}] ERROR (${duration}ms) - ${error.message}`);
        return { index, success: false, duration, error: error.message };
    }
}

async function runLoadTest() {
    console.log(`Starting load test with concurrency level: ${CONCURRENCY_LEVEL}...`);

    console.log('\n--- Testing Concurrent GET /products ---');
    const getPromises = [];
    for (let i = 0; i < CONCURRENCY_LEVEL; i++) {
        getPromises.push(fetchProducts(i));
    }
    const getResults = await Promise.all(getPromises);
    analyzeResults(getResults, 'GET /products');

    console.log('\n--- Testing Concurrent PATCH /products/:id/stock ---');
    if (PRODUCT_ID !== 'YOUR_PRODUCT_UUID') {
        const patchPromises = [];
        for (let i = 0; i < CONCURRENCY_LEVEL; i++) {
            patchPromises.push(updateStock(i));
        }
        const patchResults = await Promise.all(patchPromises);
        analyzeResults(patchResults, 'PATCH /products/:id/stock');
    } else {
        console.log('Skipping PATCH test. Please set a valid PRODUCT_ID string in the script.');
    }
}

function analyzeResults(results, testName) {
    const successful = results.filter(r => r.success);
    const failed = results.filter(r => !r.success);
    const totalDuration = results.reduce((sum, r) => sum + r.duration, 0);
    const avgDuration = totalDuration / results.length;
    const maxDuration = Math.max(...results.map(r => r.duration));
    const minDuration = Math.min(...results.map(r => r.duration));

    console.log(`\n=== Results for ${testName} ===`);
    console.log(`Total Requests: ${results.length}`);
    console.log(`Successful: ${successful.length}`);
    console.log(`Failed: ${failed.length}`);
    console.log(`Min Duration: ${minDuration}ms`);
    console.log(`Max Duration: ${maxDuration}ms`);
    console.log(`Average Duration: ${avgDuration.toFixed(2)}ms`);
}

runLoadTest();
