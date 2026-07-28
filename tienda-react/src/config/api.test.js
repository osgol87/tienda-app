import { describe, it, expect, afterEach, beforeEach } from 'vitest';

describe('config/api', () => {
    beforeEach(() => {
        vi.resetModules();
    });

    afterEach(() => {
        vi.unstubAllEnvs();
    });

    it('usa la URL del gateway y las rutas por defecto cuando no hay variables de entorno', async () => {
        vi.stubEnv('VITE_API_GATEWAY_URL', '');
        vi.stubEnv('VITE_API_PRODUCTS_URL', '');
        vi.stubEnv('VITE_API_ORDERS_URL', '');

        const { API_PRODUCTS_URL, API_ORDERS_URL, API_USERS_URL } = await import('./api.js');

        expect(API_PRODUCTS_URL).toBe('http://localhost:8762/productservice/products');
        expect(API_ORDERS_URL).toBe('http://localhost:8762/orderservice/orders');
        expect(API_USERS_URL).toBe('http://localhost:8762/userservice');
    });

    it('usa las variables de entorno cuando están definidas', async () => {
        vi.stubEnv('VITE_API_GATEWAY_URL', 'https://gateway.example.com');
        vi.stubEnv('VITE_API_PRODUCTS_URL', '/custom-products');
        vi.stubEnv('VITE_API_ORDERS_URL', '/custom-orders');

        const { API_PRODUCTS_URL, API_ORDERS_URL, API_USERS_URL } = await import('./api.js');

        expect(API_PRODUCTS_URL).toBe('https://gateway.example.com/custom-products');
        expect(API_ORDERS_URL).toBe('https://gateway.example.com/custom-orders');
        expect(API_USERS_URL).toBe('https://gateway.example.com/userservice');
    });
});
