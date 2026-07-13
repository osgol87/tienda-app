import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import AppWrapper from './SneakerStoreApp';
import { API_ORDERS_URL } from './config/api';

vi.mock('@vercel/analytics/react', () => ({
    Analytics: () => null,
}));

vi.mock('./context/AuthContext', () => ({
    AuthProvider: ({ children }) => children,
    useAuth: () => ({ user: { username: 'oscar' }, loading: false, logout: vi.fn() }),
}));

vi.mock('./components/ProtectedRoute', () => ({
    default: ({ children }) => children,
}));

vi.mock('./pages/HomePage', () => ({
    default: ({ onAddToCart }) => (
        <button onClick={() => onAddToCart({ id: 1, name: 'Air Runner', price: 50 })}>
            Agregar producto de prueba
        </button>
    ),
}));

vi.mock('./pages/CartPage', () => ({
    default: ({ cartItems, onRemoveFromCart, onCheckout }) => (
        <div>
            <span>Cart items: {cartItems.length}</span>
            {cartItems.map(item => (
                <button key={item.id} onClick={() => onRemoveFromCart(item.id)}>
                    Eliminar {item.name}
                </button>
            ))}
            <button onClick={() => onCheckout().catch(() => {})}>Checkout</button>
        </div>
    ),
}));

vi.mock('./pages/ProductListPage', () => ({ default: () => <div>ProductListPage</div> }));
vi.mock('./pages/ProductDetailsPage', () => ({ default: () => <div>ProductDetailsPage</div> }));
vi.mock('./pages/ReturnsPolicyPage', () => ({ default: () => <div>ReturnsPolicyPage</div> }));
vi.mock('./pages/ContactPage', () => ({ default: () => <div>ContactPage</div> }));
vi.mock('./pages/OrderListPage', () => ({ default: () => <div>OrderListPage</div> }));
vi.mock('./pages/OrderDetailPage', () => ({ default: () => <div>OrderDetailPage</div> }));
vi.mock('./pages/LoginPage', () => ({ default: () => <div>LoginPage</div> }));
vi.mock('./pages/RegisterPage', () => ({ default: () => <div>RegisterPage</div> }));

const jsonResponse = (body, ok = true) => ({
    ok,
    json: () => Promise.resolve(body),
});

const renderAt = (path) => {
    window.history.pushState({}, '', path);
    return render(<AppWrapper />);
};

describe('SneakerStoreApp', () => {
    beforeEach(() => {
        localStorage.clear();
        vi.stubGlobal('fetch', vi.fn());
    });

    it.each([
        ['/returns', 'ReturnsPolicyPage'],
        ['/contact', 'ContactPage'],
        ['/orders', 'OrderListPage'],
        ['/login', 'LoginPage'],
        ['/register', 'RegisterPage'],
    ])('renderiza la página correspondiente a la ruta %s', (path, expectedText) => {
        renderAt(path);

        expect(screen.getByText(expectedText)).toBeInTheDocument();
    });

    it('carga el carrito almacenado en localStorage y lo refleja en el header', () => {
        localStorage.setItem('cartItems', JSON.stringify([{ id: 1, name: 'Air Runner', price: 50, quantity: 3 }]));

        renderAt('/cart');

        expect(screen.getByText('(3)')).toBeInTheDocument();
        expect(screen.getByText('Cart items: 1')).toBeInTheDocument();
    });

    it('inicia con el carrito vacío si localStorage contiene JSON inválido', () => {
        localStorage.setItem('cartItems', 'esto-no-es-json');

        renderAt('/cart');

        expect(screen.getByText('(0)')).toBeInTheDocument();
        expect(screen.getByText('Cart items: 0')).toBeInTheDocument();
    });

    it('agrega un producto nuevo al carrito y lo persiste en localStorage', async () => {
        const user = userEvent.setup();
        renderAt('/');

        await user.click(screen.getByRole('button', { name: /agregar producto de prueba/i }));

        expect(screen.getByText('(1)')).toBeInTheDocument();
        expect(JSON.parse(localStorage.getItem('cartItems'))).toEqual([
            { id: 1, name: 'Air Runner', price: 50, quantity: 1 },
        ]);
    });

    it('incrementa la cantidad si el producto ya estaba en el carrito', async () => {
        const user = userEvent.setup();
        renderAt('/');

        await user.click(screen.getByRole('button', { name: /agregar producto de prueba/i }));
        await user.click(screen.getByRole('button', { name: /agregar producto de prueba/i }));

        expect(screen.getByText('(2)')).toBeInTheDocument();
        expect(JSON.parse(localStorage.getItem('cartItems'))[0].quantity).toBe(2);
    });

    it('elimina un producto del carrito y actualiza el header', async () => {
        localStorage.setItem('cartItems', JSON.stringify([
            { id: 1, name: 'Air Runner', price: 50, quantity: 1 },
            { id: 2, name: 'Sky Walker', price: 30, quantity: 2 },
        ]));
        const user = userEvent.setup();
        renderAt('/cart');

        expect(screen.getByText('(3)')).toBeInTheDocument();

        await user.click(screen.getByRole('button', { name: /eliminar air runner/i }));

        expect(screen.getByText('(2)')).toBeInTheDocument();
        expect(JSON.parse(localStorage.getItem('cartItems'))).toEqual([
            { id: 2, name: 'Sky Walker', price: 30, quantity: 2 },
        ]);
    });

    it('muestra un error y no llama a la API si se intenta pagar con el carrito vacío', async () => {
        const user = userEvent.setup();
        renderAt('/cart');

        await user.click(screen.getByRole('button', { name: /checkout/i }));

        expect(await screen.findByText('Tu carrito está vacío.')).toBeInTheDocument();
        expect(fetch).not.toHaveBeenCalled();
    });

    it('procesa el pago, limpia el carrito y navega al detalle de la orden creada', async () => {
        localStorage.setItem('cartItems', JSON.stringify([{ id: 1, name: 'Air Runner', price: 50, quantity: 1 }]));
        fetch.mockResolvedValueOnce(jsonResponse({ id: 99 }));
        const user = userEvent.setup();
        renderAt('/cart');

        await user.click(screen.getByRole('button', { name: /checkout/i }));

        expect(fetch).toHaveBeenCalledWith(API_ORDERS_URL, expect.objectContaining({
            method: 'POST',
            credentials: 'include',
            body: JSON.stringify({ orderItems: [{ productId: 1, quantity: 1, pricePerUnit: 50 }] }),
        }));
        expect(await screen.findByText(/compra realizada con éxito. número de la orden: 99/i)).toBeInTheDocument();
        expect(await screen.findByText('OrderDetailPage')).toBeInTheDocument();
        expect(window.location.pathname).toBe('/orders/99');
        await waitFor(() => expect(JSON.parse(localStorage.getItem('cartItems'))).toEqual([]));
    });

    it('muestra un error y conserva el carrito si la API rechaza la compra', async () => {
        localStorage.setItem('cartItems', JSON.stringify([{ id: 1, name: 'Air Runner', price: 50, quantity: 1 }]));
        fetch.mockResolvedValueOnce(jsonResponse(null, false));
        const user = userEvent.setup();
        renderAt('/cart');

        await user.click(screen.getByRole('button', { name: /checkout/i }));

        expect(await screen.findByText(/hubo un problema al procesar tu compra: error al registrar la compra\./i)).toBeInTheDocument();
        expect(screen.getByText('Cart items: 1')).toBeInTheDocument();
    });

    it('muestra un error cuando la petición de checkout falla por red', async () => {
        localStorage.setItem('cartItems', JSON.stringify([{ id: 1, name: 'Air Runner', price: 50, quantity: 1 }]));
        fetch.mockRejectedValueOnce(new Error('network error'));
        const user = userEvent.setup();
        renderAt('/cart');

        await user.click(screen.getByRole('button', { name: /checkout/i }));

        expect(await screen.findByText(/hubo un problema al procesar tu compra: network error/i)).toBeInTheDocument();
    });
});
