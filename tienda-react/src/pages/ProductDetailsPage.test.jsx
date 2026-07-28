import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProductDetailPage from './ProductDetailsPage';
import { useProduct } from '../hooks/useProduct';

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useParams: () => ({ id: '5' }),
    };
});

vi.mock('../hooks/useProduct', () => ({
    useProduct: vi.fn(),
}));

const product = {
    id: 5,
    name: 'Air Runner',
    brand: 'Speed',
    category: 'Running',
    price: 99.9,
    longDescription: 'Zapatillas ligeras pensadas para largas distancias.',
    imageUrl: '/images/air-runner.jpg',
};

describe('ProductDetailPage', () => {
    it('muestra el mensaje de carga mientras se obtiene el producto', () => {
        useProduct.mockReturnValue({ product: null, loading: true, error: null });
        render(<ProductDetailPage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });

    it('muestra el error cuando el hook falla', () => {
        useProduct.mockReturnValue({ product: null, loading: false, error: 'Producto no encontrado' });
        render(<ProductDetailPage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByRole('alert')).toHaveTextContent('Producto no encontrado');
    });

    it('muestra "Producto no encontrado" cuando no hay producto ni error', () => {
        useProduct.mockReturnValue({ product: null, loading: false, error: null });
        render(<ProductDetailPage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Producto no encontrado')).toBeInTheDocument();
    });

    it('renderiza el detalle del producto', () => {
        useProduct.mockReturnValue({ product, loading: false, error: null });
        render(<ProductDetailPage onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Air Runner')).toBeInTheDocument();
        expect(screen.getByText('Marca: Speed')).toBeInTheDocument();
        expect(screen.getByText('Categoría: Running')).toBeInTheDocument();
        expect(screen.getByText('$99.90')).toBeInTheDocument();
        expect(screen.getByText(/zapatillas ligeras pensadas/i)).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'Air Runner' })).toHaveAttribute('src', '/images/air-runner.jpg');
    });

    it('llama a onAddToCart con el producto al hacer click en el botón', async () => {
        const user = userEvent.setup();
        const onAddToCart = vi.fn();
        useProduct.mockReturnValue({ product, loading: false, error: null });
        render(<ProductDetailPage onAddToCart={onAddToCart} />, { wrapper: MemoryRouter });

        await user.click(screen.getByRole('button', { name: /agregar al carrito/i }));

        expect(onAddToCart).toHaveBeenCalledWith(product);
    });
});
