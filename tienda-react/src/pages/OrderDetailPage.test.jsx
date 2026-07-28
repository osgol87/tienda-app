import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import OrderDetailPage from './OrderDetailPage';
import { useOrder } from '../hooks/useOrder';

vi.mock('react-router-dom', async (importOriginal) => {
    const actual = await importOriginal();
    return {
        ...actual,
        useParams: () => ({ id: '7' }),
    };
});

vi.mock('../hooks/useOrder', () => ({
    useOrder: vi.fn(),
}));

const order = {
    id: 7,
    status: 'ENTREGADO',
    orderDate: '2024-03-15T00:00:00.000Z',
    totalAmount: 130,
    orderItems: [
        { id: 1, name: 'Air Runner', imageUrl: '/images/air-runner.jpg', quantity: 2, pricePerUnit: 50, subtotal: 100 },
        { id: 2, name: 'Sky Walker', imageUrl: '/images/sky-walker.jpg', quantity: 1, pricePerUnit: 30, subtotal: 30 },
    ],
};

describe('OrderDetailPage', () => {
    it('muestra el mensaje de carga mientras se obtiene la orden', () => {
        useOrder.mockReturnValue({ order: null, loading: true, error: null });
        render(<OrderDetailPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('Cargando...')).toBeInTheDocument();
    });

    it('muestra el error cuando el hook falla', () => {
        useOrder.mockReturnValue({ order: null, loading: false, error: 'Orden no encontrada' });
        render(<OrderDetailPage />, { wrapper: MemoryRouter });

        expect(screen.getByRole('alert')).toHaveTextContent('Orden no encontrada');
    });

    it('muestra "Compra no encontrada" cuando no hay orden ni error', () => {
        useOrder.mockReturnValue({ order: null, loading: false, error: null });
        render(<OrderDetailPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('Compra no encontrada')).toBeInTheDocument();
    });

    it('renderiza el detalle de la orden con sus artículos', () => {
        useOrder.mockReturnValue({ order, loading: false, error: null });
        render(<OrderDetailPage />, { wrapper: MemoryRouter });

        expect(screen.getByText('Detalle de la Compra #7')).toBeInTheDocument();
        expect(screen.getByText('Estado: ENTREGADO')).toBeInTheDocument();
        expect(screen.getByText('Total: $130.00')).toBeInTheDocument();
        expect(screen.getByText('Air Runner')).toBeInTheDocument();
        expect(screen.getByText('Cantidad: 2')).toBeInTheDocument();
        expect(screen.getByText('Subtotal: $100.00')).toBeInTheDocument();
    });

    it('renderiza el enlace para volver a mis compras', () => {
        useOrder.mockReturnValue({ order, loading: false, error: null });
        render(<OrderDetailPage />, { wrapper: MemoryRouter });

        expect(screen.getByRole('link', { name: /volver a mis compras/i })).toHaveAttribute('href', '/orders');
    });
});
