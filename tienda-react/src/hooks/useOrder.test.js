import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useOrder } from './useOrder';
import { API_ORDERS_URL } from '../config/api';

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

describe('useOrder', () => {
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn());
    });

    it('no realiza la petición y deja de cargar cuando no se recibe un id', () => {
        const { result } = renderHook(() => useOrder(undefined));

        expect(fetch).not.toHaveBeenCalled();
        expect(result.current.loading).toBe(false);
        expect(result.current.order).toBeNull();
        expect(result.current.error).toBeNull();
    });

    it('obtiene la orden por id y actualiza el estado', async () => {
        const order = { id: 7, status: 'ENTREGADO' };
        fetch.mockResolvedValueOnce(jsonResponse(order));

        const { result } = renderHook(() => useOrder(7));

        expect(result.current.loading).toBe(true);
        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(fetch).toHaveBeenCalledWith(`${API_ORDERS_URL}/7`, { credentials: 'include' });
        expect(result.current.order).toEqual(order);
        expect(result.current.error).toBeNull();
    });

    it('establece un error cuando la respuesta no es ok', async () => {
        fetch.mockResolvedValueOnce(jsonResponse(null, false));

        const { result } = renderHook(() => useOrder(999));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.order).toBeNull();
        expect(result.current.error).toBe('Orden no encontrada');
    });

    it('establece un error cuando la petición falla', async () => {
        fetch.mockRejectedValueOnce(new Error('network error'));

        const { result } = renderHook(() => useOrder(1));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.order).toBeNull();
        expect(result.current.error).toBe('network error');
    });

    it('vuelve a consultar cuando cambia el id', async () => {
        const firstOrder = { id: 1, status: 'PENDIENTE' };
        const secondOrder = { id: 2, status: 'ENVIADO' };
        fetch.mockResolvedValueOnce(jsonResponse(firstOrder));

        const { result, rerender } = renderHook(({ id }) => useOrder(id), { initialProps: { id: 1 } });
        await waitFor(() => expect(result.current.loading).toBe(false));
        expect(result.current.order).toEqual(firstOrder);

        fetch.mockResolvedValueOnce(jsonResponse(secondOrder));
        rerender({ id: 2 });

        await waitFor(() => expect(result.current.order).toEqual(secondOrder));
        expect(fetch).toHaveBeenLastCalledWith(`${API_ORDERS_URL}/2`, { credentials: 'include' });
    });
});
