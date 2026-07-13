import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import NavLinks from './NavLinks';

describe('NavLinks', () => {
    it('renderiza los enlaces de navegación con sus rutas correctas', () => {
        render(<NavLinks />, { wrapper: MemoryRouter });

        expect(screen.getByRole('link', { name: /todos los productos/i })).toHaveAttribute('href', '/products');
        expect(screen.getByRole('link', { name: /mis compras/i })).toHaveAttribute('href', '/orders');
    });
});
