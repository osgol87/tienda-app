import React from 'react';

const UserMenu = ({ user, onLogout }) => {

    if (!user) {
        return null;
    }

    return (
        <span className='header__user'>
            Hola, {user.username}
            <button className='header__logout-btn' onClick={onLogout}>
                Salir
            </button>
        </span>
    );
};

export default UserMenu;
