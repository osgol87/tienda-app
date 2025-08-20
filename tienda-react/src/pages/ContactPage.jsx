import React, { useState } from 'react';

const ContactPage = () => {
  const [form, setForm] = useState({ nombre: '', email: '', mensaje: '' });
  const [enviado, setEnviado] = useState(false);

  const handleChange = e => {
    setForm({ ...form, [e.target.name]: e.target.value });
  };

  const handleSubmit = e => {
    e.preventDefault();
    setEnviado(true);
    // Aquí podrías enviar los datos a un backend o servicio externo
  };

  return (
    <div className='contact-page'>
      <h1 className='contact-page__title'>Contacto</h1>
      <p className='contact-page__description'>¿Tienes dudas, sugerencias o necesitas ayuda? Completa el formulario o escríbenos directamente.</p>
      <div className='contact-page__info'>
        <strong>Email:</strong> soporte@speed-sneakers.com<br />
        <strong>Teléfono:</strong> +52 55 1234 5678
      </div>
      <form onSubmit={handleSubmit} className='contact-page__form'>
        <input
          type="text"
          name="nombre"
          placeholder="Tu nombre"
          value={form.nombre}
          onChange={handleChange}
          required
          className='contact-page__input'
        />
        <input
          type="email"
          name="email"
          placeholder="Tu email"
          value={form.email}
          onChange={handleChange}
          required
          className='contact-page__input'
        />
        <textarea
          name="mensaje"
          placeholder="Escribe tu mensaje"
          value={form.mensaje}
          onChange={handleChange}
          required
          rows={5}
          className='contact-page__textarea'
        />
        <button
          type="submit"
          className='contact-page__submit-button'
        >
          Enviar mensaje
        </button>
        {enviado && <div className='contact-page__success-message'>¡Mensaje enviado! Nos pondremos en contacto pronto.</div>}
      </form>
    </div>
  );
};

export default ContactPage;
