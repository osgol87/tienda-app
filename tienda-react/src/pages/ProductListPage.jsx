import React, { useEffect } from 'react';
import { useLocation } from 'react-router-dom';
import { useProducts } from '../hooks/useProducts';
import ProductCard from '../components/ProductCard';

const ProductListPage = ({ onAddToCart }) => {
    // Hook para obtener los parámetros de la URL (para la búsqueda)
    const location = useLocation();

    // Extrae el término de búsqueda de los parámetros de la URL
    const queryParams = new URLSearchParams(location.search);
    
    // Obtiene el término de búsqueda, o un string vacío si no existe
    const searchTerm = queryParams.get('search') || '';

    // Hook para obtener los productos y el estado de carga.
    // Le pasamos el `searchTerm` para que el hook realice la búsqueda en la API.
    const { products, loading, error, handleGetProducts } = useProducts(searchTerm);

    useEffect(() => {
        // Llama al hook para obtener los productos cuando el componente se monta   
        handleGetProducts(searchTerm);
    }, [handleGetProducts, searchTerm]); // Dependencia en searchTerm para actualizar cuando cambie
    
    if (loading) {
        return <h2>Cargando...</h2>;
    }

    if (error) {
        return <h2>Error: {error}</h2>;
    }

    return (
        <div className='product-list'>
            <h1 className='product-list__title'>
                {searchTerm ? `Resultados para "${searchTerm}"` : "Todos los Productos"}
            </h1>
            <div className='product-list__products'>
                {products.length > 0 ?
                (
                    // Ahora usamos 'products' directamente, que ya vienen filtrados desde la API.
                    products.map(product => (
                        <ProductCard key={product.id} product={product} onAddToCart={onAddToCart} />
                    ))
                ) : 
                (
                    <p className='products-list__empty'>No se encontraron productos.</p>
                )}
            </div>
        </div>
    );
};

export default ProductListPage;
