import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ErrorMessage from './ErrorMessage';

describe('ErrorMessage', () => {
    it('renderiza el mensaje con la clase "error-message" y rol de alerta', () => {
        render(<ErrorMessage message="No se pudo cargar la información" />);

        const alert = screen.getByRole('alert');
        expect(alert).toHaveClass('error-message');
        expect(alert).toHaveTextContent('Error: No se pudo cargar la información');
    });
});
