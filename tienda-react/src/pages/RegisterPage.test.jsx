import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from './RegisterPage';
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

describe('RegisterPage', () => {
    beforeEach(() => {
        mockNavigate.mockClear();
    });

    it('renderiza el formulario de registro y el enlace de login', () => {
        useAuth.mockReturnValue({ register: vi.fn() });
        render(<RegisterPage />, { wrapper: MemoryRouter });

        expect(screen.getByLabelText('Nombre de usuario')).toBeInTheDocument();
        expect(screen.getByLabelText('Email')).toBeInTheDocument();
        expect(screen.getByLabelText('Contraseña')).toBeInTheDocument();
        expect(screen.getByRole('link', { name: /inicia sesión/i })).toHaveAttribute('href', '/login');
    });

    it('registra al usuario y navega a la página principal', async () => {
        const user = userEvent.setup();
        const register = vi.fn().mockResolvedValue({ username: 'nuevo' });
        useAuth.mockReturnValue({ register });
        render(<RegisterPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Nombre de usuario'), 'nuevo');
        await user.type(screen.getByLabelText('Email'), 'nuevo@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'secret1');
        await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

        expect(register).toHaveBeenCalledWith('nuevo', 'nuevo@test.com', 'secret1');
        expect(mockNavigate).toHaveBeenCalledWith('/');
    });

    it('muestra el error y no navega cuando el registro falla', async () => {
        const user = userEvent.setup();
        const register = vi.fn().mockRejectedValue(new Error('El email ya está en uso'));
        useAuth.mockReturnValue({ register });
        render(<RegisterPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Nombre de usuario'), 'nuevo');
        await user.type(screen.getByLabelText('Email'), 'repetido@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'secret1');
        await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

        expect(await screen.findByText('El email ya está en uso')).toBeInTheDocument();
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('deshabilita el botón y muestra "Registrando..." mientras se procesa el registro', async () => {
        const user = userEvent.setup();
        let resolveRegister;
        const register = vi.fn(() => new Promise((resolve) => { resolveRegister = resolve; }));
        useAuth.mockReturnValue({ register });
        render(<RegisterPage />, { wrapper: MemoryRouter });

        await user.type(screen.getByLabelText('Nombre de usuario'), 'nuevo');
        await user.type(screen.getByLabelText('Email'), 'nuevo@test.com');
        await user.type(screen.getByLabelText('Contraseña'), 'secret1');
        await user.click(screen.getByRole('button', { name: /crear cuenta/i }));

        expect(screen.getByRole('button', { name: /registrando/i })).toBeDisabled();

        await resolveRegister({ username: 'nuevo' });
    });
});
