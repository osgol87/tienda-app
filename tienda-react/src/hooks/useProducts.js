import { useState, useCallback } from 'react';
import { API_PRODUCTS_URL } from '../config/api';

export const useProducts = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchProducts = useCallback(async (apiUrl) => {
        setLoading(true);
        setError(null);
        try {
            const response = await fetch(apiUrl, { credentials: 'include' });
            if (!response.ok) {
                throw new Error('Error al obtener los productos.');
            }
            const data = await response.json();
            setProducts(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    }, []);

    const handleGetProducts = useCallback(async (searchTerm = '') => {
        const cleanSearchTerm = searchTerm.trim();
        const apiUrl = cleanSearchTerm
            ? `${API_PRODUCTS_URL}?search=${encodeURIComponent(cleanSearchTerm)}`
            : API_PRODUCTS_URL;
        await fetchProducts(apiUrl);
    }, [fetchProducts]);

    return { 
        products,
        loading,
        error,
        handleGetProducts 
    };
};
