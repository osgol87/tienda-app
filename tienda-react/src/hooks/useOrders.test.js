import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useOrders } from './useOrders';
import { API_ORDERS_URL } from '../config/api';

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

describe('useOrders', () => {
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn());
    });

    it('obtiene las órdenes al montar y actualiza el estado', async () => {
        const orders = [{ id: 1, status: 'PENDIENTE' }, { id: 2, status: 'ENTREGADO' }];
        fetch.mockResolvedValueOnce(jsonResponse(orders));

        const { result } = renderHook(() => useOrders());

        expect(result.current.loading).toBe(true);
        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(fetch).toHaveBeenCalledWith(API_ORDERS_URL, { credentials: 'include' });
        expect(result.current.orders).toEqual(orders);
        expect(result.current.error).toBeNull();
    });

    it('establece un error cuando la respuesta no es ok', async () => {
        fetch.mockResolvedValueOnce(jsonResponse(null, false));

        const { result } = renderHook(() => useOrders());

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.orders).toEqual([]);
        expect(result.current.error).toBe('Error al obtener las órdenes');
    });

    it('establece un error cuando la petición falla', async () => {
        fetch.mockRejectedValueOnce(new Error('network error'));

        const { result } = renderHook(() => useOrders());

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.orders).toEqual([]);
        expect(result.current.error).toBe('network error');
    });
});
