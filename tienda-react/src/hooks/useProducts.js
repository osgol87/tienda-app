import { useState, useCallback } from 'react';

const getBaseApiUrl = () => {
    const gatewayUrl = import.meta.env.VITE_API_GATEWAY_URL;
    const productsPath = import.meta.env.VITE_API_PRODUCTS_URL;
    return (gatewayUrl && productsPath) ? `${gatewayUrl}${productsPath}` : 'http://localhost:8762/productservice/products';
};

export const useProducts = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchProducts = useCallback(async (apiUrl) => {
        setLoading(true);
        setError(null);
        try {
            const response = await fetch(apiUrl);
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
        const baseUrl = getBaseApiUrl();
        const cleanSearchTerm = searchTerm.trim();
        const apiUrl = cleanSearchTerm
            ? `${baseUrl}?search=${encodeURIComponent(cleanSearchTerm)}`
            : baseUrl;
        await fetchProducts(apiUrl);
    }, [fetchProducts]);

    return { 
        products,
        loading,
        error,
        handleGetProducts 
    };
};
