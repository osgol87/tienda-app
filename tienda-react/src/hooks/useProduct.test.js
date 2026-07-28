import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { useProduct } from './useProduct';
import { API_PRODUCTS_URL } from '../config/api';

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

describe('useProduct', () => {
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn());
    });

    it('no realiza la petición y deja de cargar cuando no se recibe un id', () => {
        const { result } = renderHook(() => useProduct(undefined));

        expect(fetch).not.toHaveBeenCalled();
        expect(result.current.loading).toBe(false);
        expect(result.current.product).toBeNull();
        expect(result.current.error).toBeNull();
    });

    it('obtiene el producto por id y actualiza el estado', async () => {
        const product = { id: 5, name: 'Air Runner' };
        fetch.mockResolvedValueOnce(jsonResponse(product));

        const { result } = renderHook(() => useProduct(5));

        expect(result.current.loading).toBe(true);
        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(fetch).toHaveBeenCalledWith(`${API_PRODUCTS_URL}/5`, { credentials: 'include' });
        expect(result.current.product).toEqual(product);
        expect(result.current.error).toBeNull();
    });

    it('establece un error cuando la respuesta no es ok', async () => {
        fetch.mockResolvedValueOnce(jsonResponse(null, false));

        const { result } = renderHook(() => useProduct(999));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.product).toBeNull();
        expect(result.current.error).toBe('Producto no encontrado');
    });

    it('establece un error cuando la petición falla', async () => {
        fetch.mockRejectedValueOnce(new Error('network error'));

        const { result } = renderHook(() => useProduct(1));

        await waitFor(() => expect(result.current.loading).toBe(false));

        expect(result.current.product).toBeNull();
        expect(result.current.error).toBe('network error');
    });

    it('vuelve a consultar cuando cambia el id', async () => {
        const firstProduct = { id: 1, name: 'Air Runner' };
        const secondProduct = { id: 2, name: 'Sky Walker' };
        fetch.mockResolvedValueOnce(jsonResponse(firstProduct));

        const { result, rerender } = renderHook(({ id }) => useProduct(id), { initialProps: { id: 1 } });
        await waitFor(() => expect(result.current.loading).toBe(false));
        expect(result.current.product).toEqual(firstProduct);

        fetch.mockResolvedValueOnce(jsonResponse(secondProduct));
        rerender({ id: 2 });

        await waitFor(() => expect(result.current.product).toEqual(secondProduct));
        expect(fetch).toHaveBeenLastCalledWith(`${API_PRODUCTS_URL}/2`, { credentials: 'include' });
    });
});
