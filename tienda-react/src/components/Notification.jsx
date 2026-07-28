import { useEffect } from 'react';

const Notification = ({ type = 'info', message, onClose }) => {

    useEffect(() => {
        if (!message) return;
        const timer = setTimeout(onClose, 5000);
        return () => clearTimeout(timer);
    }, [message, onClose]);

    if (!message) return null;

    return (
        <div className={`notification notification--${type}`} role="alert">
            <p className='notification__message'>{message}</p>
            <button onClick={onClose} className='notification__close-button' aria-label="Cerrar notificación">
                &times;
            </button>
        </div>
    );
};

export default Notification;
