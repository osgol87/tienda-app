import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import ProductCard from './ProductCard';

const product = {
    id: 42,
    name: 'Air Runner',
    shortDescription: 'Ligeras y cómodas',
    brand: 'Speed',
    category: 'Running',
    price: 99.9,
    imageUrl: '/images/air-runner.jpg',
};

describe('ProductCard', () => {
    it('renderiza la información del producto', () => {
        render(<ProductCard product={product} onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByText('Air Runner')).toBeInTheDocument();
        expect(screen.getByText('Ligeras y cómodas')).toBeInTheDocument();
        expect(screen.getByText('Marca: Speed')).toBeInTheDocument();
        expect(screen.getByText('Categoría: Running')).toBeInTheDocument();
        expect(screen.getByText('$99.90')).toBeInTheDocument();
    });

    it('renderiza la imagen con su alt y el enlace al detalle del producto', () => {
        render(<ProductCard product={product} onAddToCart={vi.fn()} />, { wrapper: MemoryRouter });

        expect(screen.getByRole('img', { name: 'Air Runner' })).toHaveAttribute('src', '/images/air-runner.jpg');
        expect(screen.getByRole('link')).toHaveAttribute('href', '/products/42');
    });

    it('llama a onAddToCart con el producto al hacer click en el botón', async () => {
        const user = userEvent.setup();
        const onAddToCart = vi.fn();
        render(<ProductCard product={product} onAddToCart={onAddToCart} />, { wrapper: MemoryRouter });

        await user.click(screen.getByRole('button', { name: /agregar al carrito/i }));

        expect(onAddToCart).toHaveBeenCalledTimes(1);
        expect(onAddToCart).toHaveBeenCalledWith(product);
    });
});
