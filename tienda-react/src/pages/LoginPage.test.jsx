import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from './LoginPage';
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

describe('LoginPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
    });

    it('renderiza el formulario de inicio de sesión y el enlace de registro', () => {
        useAuth.mockReturnValue({ login: vi.fn() });
        render(<LoginPage />, { wrapper: MemoryRouter });

        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /regístrate aquí/i })).toHaveAttribute('href', '/register');
    });

    it('inicia sesión con las credenciales ingresadas y navega a la página principal', async () => {
        const user = userEvent.setup();
        const login = vi.fn().mockResolvedValue({ username: 'oscar' });
        useAuth.mockReturnValue({ login });
        render(<LoginPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Email'), 'oscar@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'secret');
        await user.click(screen.getByRole('button', { name: /ingresar/i }));

        expect(login).toHaveBeenCalledWith('oscar@test.com', 'secret');
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });

    it('muestra el error y no navega cuando el login falla', async () => {
        const user = userEvent.setup();
        const login = vi.fn().mockRejectedValue(new Error('Credenciales inválidas'));
        useAuth.mockReturnValue({ login });
        render(<LoginPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Email'), 'oscar@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'wrong');
        await user.click(screen.getByRole('button', { name: /ingresar/i }));

        expect(await screen.findByText('Credenciales inválidas')).toBeInTheDocument();
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('deshabilita el botón y muestra "Ingresando..." mientras se procesa el login', async () => {
        const user = userEvent.setup();
        let resolveLogin;
        const login = vi.fn(() => new Promise((resolve) => { resolveLogin = resolve; }));
        useAuth.mockReturnValue({ login });
        render(<LoginPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Email'), 'oscar@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'secret');
        await user.click(screen.getByRole('button', { name: /ingresar/i }));

        expect(screen.getByRole('button', { name: /ingresando/i })).toBeDisabled();

        await resolveLogin({ username: 'oscar' });
    });
});
