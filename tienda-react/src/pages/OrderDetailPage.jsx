import React from 'react';
import { useParams, Link } from 'react-router-dom';
import { useOrder } from '../hooks/useOrder';

const OrderDetailPage = () => {

  const { id } = useParams();
  
  // Hook personalizado para obtener la orden por ID
  const { order, loading, error } = useOrder(id);

  if (loading) {
    return <h2 className="order-detail__loading">Cargando...</h2>;
  }

  if (error) {
    return <h2 className="order-detail__error">Error: {error}</h2>;
  }

  if (!order) {
    return <h2 className="order-detail__not-found">Compra no encontrada</h2>;
  }

  // Formatea la fecha en formato largo
  const formattedDate = new Date(order.orderDate).toLocaleDateString('es-MX', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  });

  return (
    <div className="order-detail">
      <h1 className="order-detail__title">Detalle de la Compra #{order.id}</h1>
      <p className="order-detail__date">Fecha: {formattedDate}</p>
      <p className="order-detail__status">Estado: {order.status}</p>
      <p className="order-detail__total">Total: ${order.totalAmount.toFixed(2)}</p>
      <ul className="order-detail__items">
        {order.orderItems.map(item => (
          <li key={item.id} className="order-detail__item">
            <img src={`${item.imageUrl}`} alt={item.name} className="order-detail__item-image" />
            <span className="order-detail__item-name">{item.name}</span>
            <span className="order-detail__item-quantity">Cantidad: {item.quantity}</span>
            <span className="order-detail__item-price">Precio: ${item.pricePerUnit.toFixed(2)}</span>
            <span className="order-detail__item-subtotal">Subtotal: ${item.subtotal.toFixed(2)}</span>
          </li>
        ))}
      </ul>
      <Link to="/orders" className="order-detail__back-link">Volver a mis compras</Link>
    </div>
  );
};

export default OrderDetailPage;