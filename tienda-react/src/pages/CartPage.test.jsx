import { describe, it, expect, vi } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import CartPage from './CartPage';

const cartItems = [
    { id: 1, name: 'Air Runner', imageUrl: '/images/air-runner.jpg', price: 50, quantity: 2 },
    { id: 2, name: 'Sky Walker', imageUrl: '/images/sky-walker.jpg', price: 30, quantity: 1 },
];

describe('CartPage', () => {
    it('muestra el mensaje de carrito vacío cuando no hay artículos', () => {
        render(<CartPage cartItems={[]} onRemoveFromCart={vi.fn()} onCheckout={vi.fn()} />);

        expect(screen.getByText('Tu carrito está vacío.')).toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /proceder al pago/i })).not.toBeInTheDocument();
    });

    it('renderiza los artículos del carrito con su cantidad y precio', () => {
        render(<CartPage cartItems={cartItems} onRemoveFromCart={vi.fn()} onCheckout={vi.fn()} />);

        expect(screen.getByText('Air Runner')).toBeInTheDocument();
        expect(screen.getByText('Cantidad: 2')).toBeInTheDocument();
        expect(screen.getByText('Precio: $50.00')).toBeInTheDocument();
        expect(screen.getByRole('img', { name: 'Air Runner' })).toHaveAttribute('src', '/images/air-runner.jpg');
    });

    it('calcula y muestra el precio total del carrito', () => {
        render(<CartPage cartItems={cartItems} onRemoveFromCart={vi.fn()} onCheckout={vi.fn()} />);

        expect(screen.getByText('Total: $130.00')).toBeInTheDocument();
    });

    it('llama a onRemoveFromCart con el id del artículo al hacer click en "Eliminar"', async () => {
        const user = userEvent.setup();
        const onRemoveFromCart = vi.fn();
        render(<CartPage cartItems={cartItems} onRemoveFromCart={onRemoveFromCart} onCheckout={vi.fn()} />);

        await user.click(screen.getAllByRole('button', { name: /eliminar/i })[0]);

        expect(onRemoveFromCart).toHaveBeenCalledWith(1);
    });

    it('deshabilita el botón y muestra "Procesando..." mientras se ejecuta el checkout', async () => {
        const user = userEvent.setup();
        let resolveCheckout;
        const onCheckout = vi.fn(() => new Promise((resolve) => { resolveCheckout = resolve; }));
        render(<CartPage cartItems={cartItems} onRemoveFromCart={vi.fn()} onCheckout={onCheckout} />);

        const button = screen.getByRole('button', { name: /proceder al pago/i });
        await user.click(button);

        expect(onCheckout).toHaveBeenCalledTimes(1);
        expect(screen.getByRole('button', { name: /procesando/i })).toBeDisabled();

        await resolveCheckout();
    });

    it('rehabilita el botón de pago cuando onCheckout falla', async () => {
        const user = userEvent.setup();
        const onCheckout = vi.fn().mockRejectedValue(new Error('Error de pago'));
        render(<CartPage cartItems={cartItems} onRemoveFromCart={vi.fn()} onCheckout={onCheckout} />);

        await user.click(screen.getByRole('button', { name: /proceder al pago/i }));

        await waitFor(() => {
            expect(screen.getByRole('button', { name: /proceder al pago/i })).not.toBeDisabled();
        });
    });
});
