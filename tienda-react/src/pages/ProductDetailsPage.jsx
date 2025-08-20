import React from 'react';
import { useParams } from 'react-router-dom';
import { useProduct } from '../hooks/useProduct';

const ProductDetailPage = ({ onAddToCart }) => {
    // useParams se usa para obtener los parámetros de la URL
    const { id } = useParams();

    // useProduct es un hook personalizado que obtiene un producto por su ID y su estado de carga/error
    const { product, loading, error } = useProduct(id);
    
    if (loading) {
        return <h2>Cargando...</h2>;
    }

    if (error) {
        return <h2>Error: {error}</h2>;
    }

    if (!product) {
        return <h2>Producto no encontrado</h2>;
    }

    return (
        <div className='product-detail-page'>
            <img src={`${product.imageUrl}`} alt={product.name} className='product-detail__image'/>
            <div className='product-detail__details'>
                <h1 className='product-detail__title'>{product.name}</h1>
                <p className='product-detail__brand'>Marca: {product.brand}</p>
                <p className='product-detail__category'>Categoría: {product.category}</p>
                <p className='product-detail__price'>${product.price.toFixed(2)}</p>
                <p className='product-detail__description'><strong>Descripción:</strong> {product.longDescription}</p>
                <button onClick={() => onAddToCart(product)} className='product-detail__add-to-cart-button'>
                    Agregar al Carrito
                </button>
            </div>
        </div>
    );
};

export default ProductDetailPage;
