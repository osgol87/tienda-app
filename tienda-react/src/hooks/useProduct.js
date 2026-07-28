import { useState, useEffect } from 'react';
import { API_PRODUCTS_URL } from '../config/api';

export const useProduct = (id) => {
    const [product, setProduct] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (!id) {
            setLoading(false);
            return;
        };

        const fetchProduct = async () => {
            setLoading(true);
            setError(null);
            try {
                const response = await fetch(`${API_PRODUCTS_URL}/${id}`, { credentials: 'include' });
                if (!response.ok) {
                    throw new Error('Producto no encontrado');
                }
                const data = await response.json();
                setProduct(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchProduct();
    }, [id]);

    return { product, loading, error };
};