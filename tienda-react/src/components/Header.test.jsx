import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import Header from './Header';
import { useAuth } from '../context/AuthContext';

const mockNavigate = vi.fn();

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

vi.mock('../context/AuthContext', () => ({
    useAuth: vi.fn(),
}));

describe('Header', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
    });

    it('navega a /products con el término buscado', async () => {
        const user = userEvent.setup();
        useAuth.mockReturnValue({ user: null, logout: vi.fn() });
        render(<Header cartCount={0} />, { wrapper: MemoryRouter });

        await user.type(screen.getByPlaceholderText(/buscar por nombre/i), 'sneakers');
        await user.click(screen.getByRole('button', { name: /buscar/i }));

        expect(mockNavigate).toHaveBeenCalledWith('/products?search=sneakers');
    });

    it('codifica caracteres especiales del término buscado antes de navegar', async () => {
        const user = userEvent.setup();
        useAuth.mockReturnValue({ user: null, logout: vi.fn() });
        render(<Header cartCount={0} />, { wrapper: MemoryRouter });

        await user.type(screen.getByPlaceholderText(/buscar por nombre/i), 'zapatillas & running #1 50%');
        await user.click(screen.getByRole('button', { name: /buscar/i }));

        expect(mockNavigate).toHaveBeenCalledWith('/products?search=zapatillas%20%26%20running%20%231%2050%25');
    });

    it('cierra sesión y navega a /login al hacer click en "Salir"', async () => {
        const user = userEvent.setup();
        const logout = vi.fn().mockResolvedValue();
        useAuth.mockReturnValue({ user: { username: 'oscar' }, logout });
        render(<Header cartCount={0} />, { wrapper: MemoryRouter });

        await user.click(screen.getByRole('button', { name: /salir/i }));

        expect(logout).toHaveBeenCalledTimes(1);
        expect(mockNavigate).toHaveBeenCalledWith('/login');
    });

    it('muestra el contador del carrito recibido por props', () => {
        useAuth.mockReturnValue({ user: null, logout: vi.fn() });
        render(<Header cartCount={5} />, { wrapper: MemoryRouter });

        expect(screen.getByText('(5)')).toBeInTheDocument();
    });

    it('no muestra el saludo de usuario cuando no hay sesión iniciada', () => {
        useAuth.mockReturnValue({ user: null, logout: vi.fn() });
        render(<Header cartCount={0} />, { wrapper: MemoryRouter });

        expect(screen.queryByText(/hola,/i)).not.toBeInTheDocument();
    });
});
