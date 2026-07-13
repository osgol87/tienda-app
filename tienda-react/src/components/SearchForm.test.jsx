import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import SearchForm from './SearchForm';

describe('SearchForm', () => {
    it('llama a onSearch con el término escrito al enviar el formulario', async () => {
        const user = userEvent.setup();
        const onSearch = vi.fn();
        render(<SearchForm onSearch={onSearch} />);

        await user.type(screen.getByPlaceholderText(/buscar por nombre/i), 'zapatillas');
        await user.click(screen.getByRole('button', { name: /buscar/i }));

        expect(onSearch).toHaveBeenCalledWith('zapatillas');
    });

    it('recorta espacios en blanco antes de llamar a onSearch', async () => {
        const user = userEvent.setup();
        const onSearch = vi.fn();
        render(<SearchForm onSearch={onSearch} />);

        await user.type(screen.getByPlaceholderText(/buscar por nombre/i), '  botas  ');
        await user.click(screen.getByRole('button', { name: /buscar/i }));

        expect(onSearch).toHaveBeenCalledWith('botas');
    });

    it('no llama a onSearch si el término está vacío o solo tiene espacios', async () => {
        const user = userEvent.setup();
        const onSearch = vi.fn();
        render(<SearchForm onSearch={onSearch} />);

        await user.type(screen.getByPlaceholderText(/buscar por nombre/i), '   ');
        await user.click(screen.getByRole('button', { name: /buscar/i }));

        expect(onSearch).not.toHaveBeenCalled();
    });
});
