import { useState, useEffect } from "react";

export const useOrder = (id) => {
    const [order, setOrder] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        }

        const fetchOrder = async () => {
            setLoading(true);
            setError(null);
            try {
                const gatewayUrl = import.meta.env.VITE_API_GATEWAY_URL;
                const ordersPath = import.meta.env.VITE_API_ORDERS_URL;
                const apiUrl = (gatewayUrl && ordersPath) ? `${gatewayUrl}${ordersPath}` : 'http://localhost:8762/orderservice/orders';

                const response = await fetch(`${apiUrl}/${id}`);
                if (!response.ok) {
                    throw new Error('Orden no encontrada');
                }
                const data = await response.json();
                setOrder(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchOrder();
    }, [id]);

    return { order, loading, error };
};