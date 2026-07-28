import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import ProtectedRoute from './ProtectedRoute';
import { useAuth } from '../context/AuthContext';

vi.mock('../context/AuthContext', () => ({
    useAuth: vi.fn(),
}));

const renderWithRoutes = () => render(
    <MemoryRouter initialEntries={['/private']}>
        <Routes>
            <Route path='/login' element={<div>Página de login</div>} />
            <Route
                path='/private'
                element={
                    <ProtectedRoute>
                        <div>Contenido protegido</div>
                    </ProtectedRoute>
                }
            />
        </Routes>
    </MemoryRouter>
);

describe('ProtectedRoute', () => {
    it('muestra el mensaje de verificación mientras se comprueba la sesión', () => {
        useAuth.mockReturnValue({ user: null, loading: true });
        renderWithRoutes();

        expect(screen.getByText(/verificando sesión/i)).toBeInTheDocument();
        expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument();
    });

    it('redirige a /login cuando no hay usuario autenticado', () => {
        useAuth.mockReturnValue({ user: null, loading: false });
        renderWithRoutes();

        expect(screen.getByText('Página de login')).toBeInTheDocument();
        expect(screen.queryByText('Contenido protegido')).not.toBeInTheDocument();
    });

    it('renderiza los children cuando hay un usuario autenticado', () => {
        useAuth.mockReturnValue({ user: { username: 'oscar' }, loading: false });
        renderWithRoutes();

        expect(screen.getByText('Contenido protegido')).toBeInTheDocument();
        expect(screen.queryByText('Página de login')).not.toBeInTheDocument();
    });
});
