import { useState, useEffect } from 'react';

export const useOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchOrders = async () => {
            setLoading(true);
            setError(null);
            try {
                const gatewayUrl = import.meta.env.VITE_API_GATEWAY_URL;
                const ordersPath = import.meta.env.VITE_API_ORDERS_URL;
                const apiUrl = (gatewayUrl && ordersPath) ? `${gatewayUrl}${ordersPath}` : 'http://localhost:8762/orderservice/orders';

                const response = await fetch(apiUrl);
                if (!response.ok) {
                    throw new Error('Error al obtener las órdenes');
                }
                const data = await response.json();
                setOrders(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchOrders();
    }, []);

    return { orders, loading, error };
}