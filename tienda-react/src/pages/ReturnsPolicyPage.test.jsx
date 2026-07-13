import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ReturnsPolicyPage from './ReturnsPolicyPage';

describe('ReturnsPolicyPage', () => {
    it('renderiza el título y las secciones de la política', () => {
        render(<ReturnsPolicyPage />, { wrapper: MemoryRouter });

        expect(screen.getByRole('heading', { name: /política de devoluciones/i, level: 1 })).toBeInTheDocument();
        expect(screen.getByText('¿Cómo solicitar una devolución?')).toBeInTheDocument();
        expect(screen.getByText('Condiciones para la devolución')).toBeInTheDocument();
        expect(screen.getByText('Proceso de devolución')).toBeInTheDocument();
        expect(screen.getByText('¿Tienes dudas?')).toBeInTheDocument();
    });

    it('renderiza los enlaces de contacto con la ruta correcta', () => {
        render(<ReturnsPolicyPage />, { wrapper: MemoryRouter });

        const contactLinks = screen.getAllByRole('link');
        expect(contactLinks).toHaveLength(2);
        contactLinks.forEach(link => expect(link).toHaveAttribute('href', '/contact'));
    });
});
