import React from 'react';
import { Link, useNavigate } from 'react-router-dom';

const Header = ({ cartCount }) => {

    // Estado para el término de búsqueda
    const [searchTerm, setSearchTerm] = React.useState('');

    // Hook para navegación
    const navigate = useNavigate();

    // Maneja el envío del formulario de búsqueda
    const handleSearch = (e) => {

        e.preventDefault();

        if (searchTerm.trim()) {
            navigate(`/products?search=${searchTerm.trim()}`);
        }
    };

    return (
        <header className='header__container'>
            <Link to="/" className='header__logo'>
                <img src="/logo-min.png" alt="Speed Sneakers" className='header__logo-image' />
            </Link>
            <nav className='header__nav'>
                <form className='header__search-form' onSubmit={handleSearch}>
                    <input
                        type="text"
                        className='header__search-input'
                        placeholder="Buscar por nombre, marca, categoría..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                    <button type="submit" className='header__search-button'>Buscar</button>
                </form>
                <Link to="/products" className='header__link'>Todos los Productos</Link>
                <Link to="/orders" className='header__link'>Mis Compras</Link>
                <Link to="/cart" className='header__cart-link'>
                    🛒 Carrito <span className='header__cart-count'>({cartCount})</span>
                </Link>
            </nav>
        </header>
    );
};

export default Header;
