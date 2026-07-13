const GATEWAY_URL = import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8762';

export const API_PRODUCTS_URL = `${GATEWAY_URL}${import.meta.env.VITE_API_PRODUCTS_URL || '/productservice/products'}`;
export const API_ORDERS_URL = `${GATEWAY_URL}${import.meta.env.VITE_API_ORDERS_URL || '/orderservice/orders'}`;
export const API_USERS_URL = `${GATEWAY_URL}/userservice`;
