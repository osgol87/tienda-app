import React from 'react';

const SearchForm = ({ onSearch }) => {

    const [searchTerm, setSearchTerm] = React.useState('');

    const handleSubmit = (e) => {
        e.preventDefault();

        if (searchTerm.trim()) {
            onSearch(searchTerm.trim());
        }
    };

    return (
        <form className='header__search-form' onSubmit={handleSubmit}>
            <input
                type="text"
                className='header__search-input'
                placeholder="Buscar por nombre, marca, categoría..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
            />
            <button type="submit" className='header__search-button'>Buscar</button>
        </form>
    );
};

export default SearchForm;
