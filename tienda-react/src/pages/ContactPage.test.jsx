import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import ContactPage from './ContactPage';

describe('ContactPage', () => {
    it('renderiza la información de contacto', () => {
        render(<ContactPage />);

        expect(screen.getByText(/soporte@speed-sneakers\.com/)).toBeInTheDocument();
        expect(screen.getByText(/\+52 55 1234 5678/)).toBeInTheDocument();
    });

    it('no muestra el mensaje de éxito antes de enviar el formulario', () => {
        render(<ContactPage />);

        expect(screen.queryByText(/mensaje enviado/i)).not.toBeInTheDocument();
    });

    it('permite escribir en los campos del formulario', async () => {
        const user = userEvent.setup();
        render(<ContactPage />);

        const nombreInput = screen.getByPlaceholderText('Tu nombre');
        const emailInput = screen.getByPlaceholderText('Tu email');
        const mensajeInput = screen.getByPlaceholderText('Escribe tu mensaje');

        await user.type(nombreInput, 'Oscar');
        await user.type(emailInput, 'oscar@test.com');
        await user.type(mensajeInput, 'Hola, tengo una duda.');

        expect(nombreInput).toHaveValue('Oscar');
        expect(emailInput).toHaveValue('oscar@test.com');
        expect(mensajeInput).toHaveValue('Hola, tengo una duda.');
    });

    it('muestra el mensaje de éxito al enviar el formulario completo', async () => {
        const user = userEvent.setup();
        render(<ContactPage />);

        await user.type(screen.getByPlaceholderText('Tu nombre'), 'Oscar');
        await user.type(screen.getByPlaceholderText('Tu email'), 'oscar@test.com');
        await user.type(screen.getByPlaceholderText('Escribe tu mensaje'), 'Hola, tengo una duda.');
        await user.click(screen.getByRole('button', { name: /enviar mensaje/i }));

        expect(screen.getByText('¡Mensaje enviado! Nos pondremos en contacto pronto.')).toBeInTheDocument();
    });
});
