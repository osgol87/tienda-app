import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import Notification from './Notification';

describe('Notification', () => {
    afterEach(() => {
        vi.useRealTimers();
    });

    it('no renderiza nada cuando no hay mensaje', () => {
        const { container } = render(<Notification onClose={vi.fn()} />);

        expect(container).toBeEmptyDOMElement();
    });

    it('renderiza el mensaje con la clase por defecto "info"', () => {
        render(<Notification message="Todo correcto" onClose={vi.fn()} />);

        expect(screen.getByText('Todo correcto')).toBeInTheDocument();
        expect(screen.getByRole('alert')).toHaveClass('notification--info');
    });

    it('aplica la clase correspondiente al tipo recibido por props', () => {
        render(<Notification type="error" message="Algo falló" onClose={vi.fn()} />);

        expect(screen.getByRole('alert')).toHaveClass('notification--error');
    });

    it('llama a onClose al hacer click en el botón de cerrar', async () => {
        const user = userEvent.setup();
        const onClose = vi.fn();
        render(<Notification message="Todo correcto" onClose={onClose} />);

        await user.click(screen.getByRole('button', { name: /cerrar notificación/i }));

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('llama a onClose automáticamente tras 5 segundos', () => {
        vi.useFakeTimers();
        const onClose = vi.fn();
        render(<Notification message="Todo correcto" onClose={onClose} />);

        expect(onClose).not.toHaveBeenCalled();
        vi.advanceTimersByTime(5000);

        expect(onClose).toHaveBeenCalledTimes(1);
    });

    it('no llama a onClose si el componente se desmonta antes de los 5 segundos', () => {
        vi.useFakeTimers();
        const onClose = vi.fn();
        const { unmount } = render(<Notification message="Todo correcto" onClose={onClose} />);

        unmount();
        vi.advanceTimersByTime(5000);

        expect(onClose).not.toHaveBeenCalled();
    });
});
