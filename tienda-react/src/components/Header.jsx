import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import SearchForm from './SearchForm';
import NavLinks from './NavLinks';
import CartLink from './CartLink';
import UserMenu from './UserMenu';

const Header = ({ cartCount }) => {

    const navigate = useNavigate();
    const { user, logout } = useAuth();

    const handleLogout = async () => {
        await logout();
        navigate('/login');
    };

    const handleSearch = (term) => {
        navigate(`/products?search=${encodeURIComponent(term)}`);
    };

    return (
        <header className='header__container'>
            <Link to="/" className='header__logo'>
                <img src="/logo-min.png" alt="Speed Sneakers" className='header__logo-image' />
            </Link>
            <nav className='header__nav'>
                <SearchForm onSearch={handleSearch} />
                <NavLinks />
                <CartLink cartCount={cartCount} />
                <UserMenu user={user} onLogout={handleLogout} />
            </nav>
        </header>
    );
};

export default Header;
