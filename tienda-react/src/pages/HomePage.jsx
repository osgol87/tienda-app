import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useProducts } from '../hooks/useProducts';
import ProductCard from '../components/ProductCard';

const HomePage = ({ onAddToCart }) => {
    
    // Obtiene los productos y el estado de carga desde el hook personalizado
    const { products, loading, error, handleGetProducts } = useProducts();

    useEffect(() => {
        handleGetProducts();
    }, [handleGetProducts]); // Llama a la API al montar el componente
    
    if (loading) {
        // Muestra un mensaje de carga mientras se obtienen los productos
        return <h2>Cargando productos...</h2>;
    }

    if (error) {
        return <h2>Error: {error}</h2>;
    }

    return (
        <div className="home-page">
            <h1 className="home-page__title">Bienvenidos a Speed Sneakers</h1>
            <p className="home-page__subtitle">Los mejores tenis, a un clic de distancia.</p>
            <h2 className="home-page__featured-title">Productos Destacados</h2>
            <div className="home-page__products">
                { products
                    .slice(0, 3)
                    .map(product => ( // Muestra solo los primeros 3
                        <ProductCard key={product.id} product={product} onAddToCart={onAddToCart} />
                    ))
                }
            </div>
            <div className="home-page__all-products">
                <Link to="/products">
                    <button className="home-page__all-products-button">
                        Ver todos los productos
                    </button>
                </Link>
            </div>
        </div>
    );
};

export default HomePage;
