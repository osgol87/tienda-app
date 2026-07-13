import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { useProducts } from './useProducts';
import { API_PRODUCTS_URL } from '../config/api';

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

describe('useProducts', () => {
    beforeEach(() => {
        vi.stubGlobal('fetch', vi.fn());
    });

    it('no realiza ninguna petición hasta que se llama a handleGetProducts', () => {
        renderHook(() => useProducts());

        expect(fetch).not.toHaveBeenCalled();
    });

    it('obtiene todos los productos cuando no se pasa término de búsqueda', async () => {
        const products = [{ id: 1, name: 'Air Runner' }];
        fetch.mockResolvedValueOnce(jsonResponse(products));

        const { result } = renderHook(() => useProducts());

        await act(async () => {
            await result.current.handleGetProducts();
        });

        expect(fetch).toHaveBeenCalledWith(API_PRODUCTS_URL, { credentials: 'include' });
        expect(result.current.products).toEqual(products);
        expect(result.current.loading).toBe(false);
        expect(result.current.error).toBeNull();
    });

    it('agrega el término de búsqueda codificado a la URL', async () => {
        fetch.mockResolvedValueOnce(jsonResponse([]));

        const { result } = renderHook(() => useProducts());

        await act(async () => {
            await result.current.handleGetProducts('zapatillas & running');
        });

        expect(fetch).toHaveBeenCalledWith(
            `${API_PRODUCTS_URL}?search=${encodeURIComponent('zapatillas & running')}`,
            { credentials: 'include' }
        );
    });

    it('recorta espacios del término de búsqueda antes de usarlo', async () => {
        fetch.mockResolvedValueOnce(jsonResponse([]));

        const { result } = renderHook(() => useProducts());

        await act(async () => {
            await result.current.handleGetProducts('   ');
        });

        expect(fetch).toHaveBeenCalledWith(API_PRODUCTS_URL, { credentials: 'include' });
    });

    it('establece un error cuando la respuesta no es ok', async () => {
        fetch.mockResolvedValueOnce(jsonResponse(null, false));

        const { result } = renderHook(() => useProducts());

        await act(async () => {
            await result.current.handleGetProducts();
        });

        expect(result.current.products).toEqual([]);
        expect(result.current.error).toBe('Error al obtener los productos.');
    });

    it('establece un error cuando la petición falla', async () => {
        fetch.mockRejectedValueOnce(new Error('network error'));

        const { result } = renderHook(() => useProducts());

        await act(async () => {
            await result.current.handleGetProducts();
        });

        expect(result.current.products).toEqual([]);
        expect(result.current.error).toBe('network error');
    });

    it('refleja el estado de carga mientras se resuelve la petición', async () => {
        let resolveFetch;
        fetch.mockReturnValueOnce(new Promise((resolve) => { resolveFetch = resolve; }));

        const { result } = renderHook(() => useProducts());

        act(() => {
            result.current.handleGetProducts();
        });

        expect(result.current.loading).toBe(true);

        await act(async () => {
            resolveFetch(jsonResponse([]));
        });

        await waitFor(() => expect(result.current.loading).toBe(false));
    });
});
