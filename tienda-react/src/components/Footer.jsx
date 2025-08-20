import React from 'react';
import { Link } from 'react-router-dom';
import Copyright from './Copyright';

const Footer = () => (
    <footer className='footer'>
        <Copyright />
        <div className='footer__links'>
            <Link className='footer__link' to="/returns">Política de Devoluciones</Link>
            <Link className='footer__link' to="/contact">Contacto</Link>
        </div>
    </footer>
);

export default Footer;
