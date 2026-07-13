import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { AuthProvider, useAuth } from './AuthContext';
import { API_USERS_URL } from '../config/api';

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

describe('AuthContext', () => {
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn());
    });

    describe('carga inicial de sesión', () => {
        it('obtiene el usuario autenticado al montar', async () => {
            const user = { username: 'oscar' };
            fetch.mockResolvedValueOnce(jsonResponse(user));

            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

            expect(result.current.loading).toBe(true);
            await waitFor(() => expect(result.current.loading).toBe(false));

            expect(fetch).toHaveBeenCalledWith(`${API_USERS_URL}/auth/me`, { credentials: 'include' });
            expect(result.current.user).toEqual(user);
        });

        it('deja el usuario en null cuando no hay sesión activa', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false));

            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

            await waitFor(() => expect(result.current.loading).toBe(false));

            expect(result.current.user).toBeNull();
        });

        it('deja de cargar aunque la petición falle', async () => {
            fetch.mockRejectedValueOnce(new Error('network error'));

            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });

            await waitFor(() => expect(result.current.loading).toBe(false));

            expect(result.current.user).toBeNull();
        });
    });

    describe('login', () => {
        it('inicia sesión y actualiza el usuario', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            const loggedInUser = { username: 'oscar' };
            fetch.mockResolvedValueOnce(jsonResponse(loggedInUser));

            let returnedData;
            await act(async () => {
                returnedData = await result.current.login('oscar@test.com', 'secret');
            });

            expect(fetch).toHaveBeenLastCalledWith(`${API_USERS_URL}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ email: 'oscar@test.com', password: 'secret' }),
            });
            expect(returnedData).toEqual(loggedInUser);
            expect(result.current.user).toEqual(loggedInUser);
        });

        it('lanza el mensaje de error recibido del servidor cuando las credenciales son inválidas', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            fetch.mockResolvedValueOnce(jsonResponse({ message: 'Usuario o contraseña incorrectos' }, false));

            await expect(result.current.login('oscar@test.com', 'wrong')).rejects.toThrow('Usuario o contraseña incorrectos');
        });

        it('usa un mensaje por defecto si el servidor no devuelve uno', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            fetch.mockResolvedValueOnce({ ok: false, json: () => Promise.reject(new Error('sin cuerpo')) });

            await expect(result.current.login('oscar@test.com', 'wrong')).rejects.toThrow('Credenciales inválidas');
        });
    });

    describe('register', () => {
        it('registra al usuario y actualiza el estado', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            const newUser = { username: 'nuevo' };
            fetch.mockResolvedValueOnce(jsonResponse(newUser));

            let returnedData;
            await act(async () => {
                returnedData = await result.current.register('nuevo', 'nuevo@test.com', 'secret');
            });

            expect(fetch).toHaveBeenLastCalledWith(`${API_USERS_URL}/auth/register`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ username: 'nuevo', email: 'nuevo@test.com', password: 'secret' }),
            });
            expect(returnedData).toEqual(newUser);
            expect(result.current.user).toEqual(newUser);
        });

        it('lanza el primer error de validación cuando no hay "message"', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            fetch.mockResolvedValueOnce(jsonResponse({ email: 'El email ya está en uso' }, false));

            await expect(result.current.register('nuevo', 'repetido@test.com', 'secret'))
                .rejects.toThrow('El email ya está en uso');
        });

        it('usa un mensaje por defecto si el servidor no devuelve errores', async () => {
            fetch.mockResolvedValueOnce(jsonResponse(null, false)); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));

            fetch.mockResolvedValueOnce({ ok: false, json: () => Promise.reject(new Error('sin cuerpo')) });

            await expect(result.current.register('nuevo', 'nuevo@test.com', 'secret'))
                .rejects.toThrow('Error al registrarse');
        });
    });

    describe('logout', () => {
        it('cierra sesión y limpia el usuario', async () => {
            fetch.mockResolvedValueOnce(jsonResponse({ username: 'oscar' })); // fetch inicial /auth/me
            const { result } = renderHook(() => useAuth(), { wrapper: AuthProvider });
            await waitFor(() => expect(result.current.loading).toBe(false));
            expect(result.current.user).toEqual({ username: 'oscar' });

            fetch.mockResolvedValueOnce(jsonResponse({}));

            await act(async () => {
                await result.current.logout();
            });

            expect(fetch).toHaveBeenLastCalledWith(`${API_USERS_URL}/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });
            expect(result.current.user).toBeNull();
        });
    });
});
