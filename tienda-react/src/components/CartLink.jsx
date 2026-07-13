import React from 'react';
import { Link } from 'react-router-dom';

const CartLink = ({ cartCount }) => (
    <Link to="/cart" className='header__cart-link'>
        Carrito <span className='header__cart-count'>({cartCount})</span>
    </Link>
);

export default CartLink;
