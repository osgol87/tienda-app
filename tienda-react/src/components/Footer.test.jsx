import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Footer from './Footer';

describe('Footer', () => {
    it('renderiza el aviso de copyright', () => {
        render(<Footer />, { wrapper: MemoryRouter });

        expect(screen.getByText(/todos los derechos reservados/i)).toBeInTheDocument();
    });

    it('renderiza los enlaces con sus rutas correctas', () => {
        render(<Footer />, { wrapper: MemoryRouter });

        expect(screen.getByRole('link', { name: /política de devoluciones/i })).toHaveAttribute('href', '/returns');
        expect(screen.getByRole('link', { name: /contacto/i })).toHaveAttribute('href', '/contact');
    });
});
