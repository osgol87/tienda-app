import { useState, useEffect } from 'react';

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
                // Asumimos que VITE_API_URL está configurada, de lo contrario usamos
                const gatewayUrl = import.meta.env.VITE_API_GATEWAY_URL;
                const productsPath = import.meta.env.VITE_API_PRODUCTS_URL;

                const apiUrl = (gatewayUrl && productsPath) ? `${gatewayUrl}${productsPath}` : 'http://localhost:8762/productservice/products';

                const response = await fetch(`${apiUrl}/${id}`);
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