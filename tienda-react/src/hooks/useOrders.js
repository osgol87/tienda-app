import { useState, useEffect } from 'react';
import { API_ORDERS_URL } from '../config/api';

export const useOrders = () => {
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchOrders = async () => {
            setLoading(true);
            setError(null);
            try {
                const response = await fetch(API_ORDERS_URL, { credentials: 'include' });
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