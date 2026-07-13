import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ProductListPage from './ProductListPage';
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

const renderAt = (path, ui) => render(ui, { wrapper: ({ children }) => (
    <MemoryRouter initialEntries={[path]}>{children}</MemoryRouter>
) });

describe('ProductListPage', () => {
    const handleGetProducts = vi.fn();

    beforeEach(() => {
        handleGetProducts.mockClear();
    });

    it('llama a handleGetProducts sin término cuando no hay búsqueda en la URL', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: null, handleGetProducts });
        renderAt('/products', <ProductListPage onAddToCart={vi.fn()} />);

        expect(handleGetProducts).toHaveBeenCalledWith('');
        expect(screen.getByText('Todos los Productos')).toBeInTheDocument();
    });

    it('llama a handleGetProducts con el término recibido en la URL', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: null, handleGetProducts });
        renderAt('/products?search=zapatillas', <ProductListPage onAddToCart={vi.fn()} />);

        expect(handleGetProducts).toHaveBeenCalledWith('zapatillas');
        expect(screen.getByText('Resultados para "zapatillas"')).toBeInTheDocument();
    });

    it('muestra el mensaje de carga mientras se obtienen los productos', () => {
        useProducts.mockReturnValue({ products: [], loading: true, error: null, handleGetProducts });
        renderAt('/products', <ProductListPage onAddToCart={vi.fn()} />);

        expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });

    it('muestra el error cuando el hook falla', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: 'Error al obtener los productos.', handleGetProducts });
        renderAt('/products', <ProductListPage onAddToCart={vi.fn()} />);

        expect(screen.getByRole('alert')).toHaveTextContent('Error al obtener los productos.');
    });

    it('muestra el mensaje de "sin resultados" cuando no hay productos', () => {
        useProducts.mockReturnValue({ products: [], loading: false, error: null, handleGetProducts });
        renderAt('/products?search=inexistente', <ProductListPage onAddToCart={vi.fn()} />);

        expect(screen.getByText('No se encontraron productos.')).toBeInTheDocument();
    });

    it('renderiza todos los productos recibidos', () => {
        const products = [1, 2, 3, 4].map(buildProduct);
        useProducts.mockReturnValue({ products, loading: false, error: null, handleGetProducts });
        renderAt('/products', <ProductListPage onAddToCart={vi.fn()} />);

        products.forEach(p => expect(screen.getByText(p.name)).toBeInTheDocument());
    });
});
