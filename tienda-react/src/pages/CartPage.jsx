import React, { useState } from 'react';

const CartPage = ({ cartItems, onRemoveFromCart, onCheckout }) => {

    const [isCheckingOut, setIsCheckingOut] = useState(false);

    // Calcula el precio total del carrito
    const totalPrice = cartItems.reduce((total, item) => total + item.price * item.quantity, 0);

    const handleCheckoutClick = async () => {
        setIsCheckingOut(true);
        try {
            await onCheckout();
            // No es necesario cambiar isCheckingOut a false en caso de éxito,
            // ya que seremos redirigidos a otra página.
        } catch (error) {
            // Si onCheckout falla, el error ya se muestra en una alerta.
            // Aquí solo re-habilitamos el botón.
            setIsCheckingOut(false);
        }
    };

    return (
        <div className='cart'>
            <h1 className='cart__title'>Tu Carrito de Compras</h1>
            {cartItems.length === 0 ? (
                <p className='cart__empty--message'>Tu carrito está vacío.</p>
            ) : (
                <div className='cart__items'>
                    {cartItems.map(item => (
                        <div key={item.id} className='cart__item'>
                            <img src={`${item.imageUrl}`} alt={item.name} className='cart__item-image' />
                            <div className='cart__item-details'>
                                <h2 className='cart__item-name'>{item.name}</h2>
                                <p className='cart__item-quantity'>Cantidad: {item.quantity}</p>
                                <p className='cart__item-price'>Precio: ${item.price.toFixed(2)}</p>
                            </div>
                            <button onClick={() => onRemoveFromCart(item.id)} className='cart__item-remove-button'>
                                Eliminar
                            </button>
                        </div>
                    ))}
                    <div className='cart__summary'>
                        <h2 className='cart__summary-total'>Total: ${totalPrice.toFixed(2)}</h2>
                        <button 
                            onClick={handleCheckoutClick} 
                            className='cart__summary-checkout-button'
                            disabled={isCheckingOut || cartItems.length === 0}>
                            {isCheckingOut ? 'Procesando...' : 'Proceder al Pago'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
};

export default CartPage;
