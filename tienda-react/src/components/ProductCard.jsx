import React from 'react';
import { Link } from 'react-router-dom';

const ProductCard = ({ product, onAddToCart }) => {
    return (
        <div className='product-card'>
            <Link to={`/products/${product.id}`}>
                <img src={`${product.imageUrl}`} alt={product.name} className='product-card__image' />
            </Link>
            <div className='product-card__body'>
                <h3 className='product-card__title'>{product.name}</h3>
                <p className='product-card__description'>{product.shortDescription}</p>
                <p className='product-card__brand'>Marca: {product.brand}</p>
                <p className='product-card__category'>Categoría: {product.category}</p>
                <p className='product-card__price'>${product.price.toFixed(2)}</p>
                <button onClick={() => onAddToCart(product)} className='product-card__add-to-cart-button'>
                    Agregar al Carrito
                </button>
            </div>
        </div>
    );
};

export default ProductCard;
