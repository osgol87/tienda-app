import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import Copyright from './Copyright';

describe('Copyright', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('usa los valores por defecto de compañía y año actual cuando no se pasan props', () => {
        vi.useFakeTimers();
        vi.setSystemTime(new Date(2026, 0, 1));

        render(<Copyright />);

        expect(screen.getByText('2026', { selector: '.copyright__year' })).toBeInTheDocument();
        expect(screen.getByText('Speed Sneakers', { selector: '.copyright__company' })).toBeInTheDocument();
    });

    it('muestra la compañía y el año recibidos por props', () => {
        render(<Copyright company="Mi Tienda" year={2020} />);

        expect(screen.getByText('2020', { selector: '.copyright__year' })).toBeInTheDocument();
        expect(screen.getByText('Mi Tienda', { selector: '.copyright__company' })).toBeInTheDocument();
    });

    it('renderiza el texto completo con el símbolo de copyright', () => {
        render(<Copyright company="Mi Tienda" year={2020} />);

        expect(screen.getByText(/todos los derechos reservados/i)).toBeInTheDocument();
        expect(screen.getByText(/©/)).toBeInTheDocument();
    });
});
