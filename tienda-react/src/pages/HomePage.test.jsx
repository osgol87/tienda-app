import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HomePage from './HomePage';
import { useProducts } from '../hooks/useProducts';

vi.mock('../hooks/useProducts', () => ({
    useProducts: vi.fn(),
}));

const buildProduct = (id) => ({
    id,
    name: `Producto ${id}`,
    shortDescription: 'Descripción corta',
    brand: 'Speed',
    category: 'Running',
    price: 10 * id,
    imageUrl: `/images/product-${id}.jpg`,
});

describe('HomePage', () => {
    const handleGetProducts = vi.fn();

    beforeEach(() => {
        handleGetProducts.mockClear();
    });

    it('llama a handleGetProducts al montar', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: null, handleGetProducts });
        render(<HomePage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(handleGetProducts).toHaveBeenCalledTimes(1);
    });

    it('muestra el mensaje de carga mientras se obtienen los productos', () => {
        useProducts.mockReturnValue({ products: [], loading: true, error: null, handleGetProducts });
        render(<HomePage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Cargando productos...')).toBeInTheDocument();
    });

    it('muestra el error cuando el hook falla', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: 'Error al obtener los productos.', handleGetProducts });
        render(<HomePage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByRole('alert')).toHaveTextContent('Error al obtener los productos.');
    });

    it('muestra solo los primeros 3 productos destacados', () => {
        const products = [1, 2, 3, 4].map(buildProduct);
        useProducts.mockReturnValue({ products, loading: false, error: null, handleGetProducts });
        render(<HomePage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Producto 1')).toBeInTheDocument();
        expect(screen.getByText('Producto 2')).toBeInTheDocument();
        expect(screen.getByText('Producto 3')).toBeInTheDocument();
        expect(screen.queryByText('Producto 4')).not.toBeInTheDocument();
    });

    it('renderiza el enlace a la página de todos los productos', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: null, handleGetProducts });
        render(<HomePage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByRole('link', { name: /ver todos los productos/i })).toHaveAttribute('href', '/products');
    });
});
