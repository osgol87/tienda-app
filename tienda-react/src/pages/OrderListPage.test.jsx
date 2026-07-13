import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderListPage from './OrderListPage';
import { useOrders } from '../hooks/useOrders';

vi.mock('../hooks/useOrders', () => ({
    useOrders: vi.fn(),
}));

const orders = [
    { id: 1, orderDate: '2024-03-15T00:00:00.000Z', status: 'ENTREGADO', totalAmount: 100 },
    { id: 2, orderDate: '2024-04-01T00:00:00.000Z', status: 'PENDIENTE', totalAmount: 50 },
];

describe('OrderListPage', () => {
    it('muestra el mensaje de carga mientras se obtienen las órdenes', () => {
        useOrders.mockReturnValue({ orders: [], loading: true, error: null });
        render(<OrderListPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('Cargando ...')).toBeInTheDocument();
    });

    it('muestra el error cuando el hook falla', () => {
        useOrders.mockReturnValue({ orders: [], loading: false, error: 'Error al obtener las órdenes' });
        render(<OrderListPage />, { wrapper: MemoryRouter });

        expect(screen.getByRole('alert')).toHaveTextContent('Error al obtener las órdenes');
    });

    it('muestra el mensaje de lista vacía cuando no hay compras', () => {
        useOrders.mockReturnValue({ orders: [], loading: false, error: null });
        render(<OrderListPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('No has realizado compras aún.')).toBeInTheDocument();
    });

    it('renderiza la lista de órdenes con su enlace de detalle', () => {
        useOrders.mockReturnValue({ orders, loading: false, error: null });
        render(<OrderListPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('Orden #1')).toBeInTheDocument();
        expect(screen.getByText('ENTREGADO')).toBeInTheDocument();
        expect(screen.getByText('Total: $100.00')).toBeInTheDocument();
        expect(screen.getByText('Orden #2')).toBeInTheDocument();

        const detailLinks = screen.getAllByRole('link', { name: /ver detalle/i });
        expect(detailLinks[0]).toHaveAttribute('href', '/orders/1');
        expect(detailLinks[1]).toHaveAttribute('href', '/orders/2');
    });
});
