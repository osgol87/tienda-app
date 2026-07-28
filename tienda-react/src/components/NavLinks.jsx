import React from 'react';
import { Link } from 'react-router-dom';

const NavLinks = () => (
    <>
        <Link to="/products" className='header__link'>Todos los Productos</Link>
        <Link to="/orders" className='header__link'>Mis Compras</Link>
    </>
);

export default NavLinks;
