import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import UserMenu from './UserMenu';

describe('UserMenu', () => {
    it('no renderiza nada si no hay usuario autenticado', () => {
        const { container } = render(<UserMenu user={null} onLogout={vi.fn()} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('saluda al usuario autenticado por su username', () => {
        render(<UserMenu user={{ username: 'oscar' }} onLogout={vi.fn()} />);

        expect(screen.getByText(/hola, oscar/i)).toBeInTheDocument();
    });

    it('llama a onLogout al hacer click en "Salir"', async () => {
        const user = userEvent.setup();
        const onLogout = vi.fn();
        render(<UserMenu user={{ username: 'oscar' }} onLogout={onLogout} />);

        await user.click(screen.getByRole('button', { name: /salir/i }));

        expect(onLogout).toHaveBeenCalledTimes(1);
    });
});
