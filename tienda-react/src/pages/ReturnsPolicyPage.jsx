import React from 'react';
import { Link } from 'react-router-dom';

const ReturnsPolicyPage = () => (
  <div className='returns-policy'>
    <h1 className='returns-policy__title'>Política de Devoluciones</h1>
    <section className='returns-policy__section'>
      <h2 className='returns-policy__subtitle'>¿Cómo solicitar una devolución?</h2>
      <p className='returns-policy__text'>
        Si no estás satisfecho con tu compra, puedes solicitar una devolución dentro de los 30 días posteriores a la recepción del producto. Por favor, asegúrate de que el producto esté en su estado original y sin usar.
      </p>
    </section>
    <section className='returns-policy__section'>
      <h2 className='returns-plicy__subtitle'>Condiciones para la devolución</h2>
      <ul className='returns-policy__conditions' style={{ paddingLeft: 20 }}>
        <li>El producto debe estar sin usar y en su empaque original.</li>
        <li>Debes presentar el comprobante de compra.</li>
        <li>No se aceptan devoluciones de productos en oferta o personalizados.</li>
      </ul>
    </section>
    <section className='returns-policy__section'>
      <h2 className='returns-policy__subtitle'>Proceso de devolución</h2>
      <ol className='returns-policy__steps'>
        <li>Contacta a nuestro equipo de atención al cliente a través del formulario de <Link to="/contact">Contacto</Link>.</li>
        <li>Empaqueta el producto de forma segura e incluye el comprobante de compra.</li>
        <li>Te enviaremos instrucciones para el envío del producto.</li>
        <li>Una vez recibido y verificado, procesaremos tu reembolso en un plazo de 7 días hábiles.</li>
      </ol>
    </section>
    <section className='returns-policy__section'>
      <h2 className='returns-policy__subtitle'>¿Tienes dudas?</h2>
      <p className='returns-policy__text'>
        Si tienes alguna pregunta sobre nuestra política de devoluciones, no dudes en <Link to="/contact">contactarnos</Link>.
      </p>
    </section>
  </div>
);

export default ReturnsPolicyPage;
