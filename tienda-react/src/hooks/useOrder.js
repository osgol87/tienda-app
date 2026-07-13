import { useState, useEffect } from "react";
import { API_ORDERS_URL } from '../config/api';

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
                const response = await fetch(`${API_ORDERS_URL}/${id}`, { credentials: 'include' });
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