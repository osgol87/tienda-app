import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import CartLink from './CartLink';

describe('CartLink', () => {
    it('muestra el número de artículos recibido por props', () => {
        render(<CartLink cartCount={3} />, { wrapper: MemoryRouter });

        expect(screen.getByRole('link', { name: /carrito/i })).toHaveAttribute('href', '/cart');
        expect(screen.getByText('(3)')).toBeInTheDocument();
    });

    it('muestra 0 cuando el carrito está vacío', () => {
        render(<CartLink cartCount={0} />, { wrapper: MemoryRouter });

        expect(screen.getByText('(0)')).toBeInTheDocument();
    });
});
