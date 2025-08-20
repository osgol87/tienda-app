import React from 'react';
import { Link } from 'react-router-dom';
import { useOrders } from '../hooks/useOrders';

const OrderListPage = () => {

  // Hook para obtener las órdenes del usuario
  const { orders, loading, error } = useOrders(); 

  if (loading) {
    return <h2>Cargando ...</h2>;
  }
  
  if (error) {
    return <h2>Error: {error}</h2>;
  }

  return (
    <div className="order-list">
      <h1 className="order-list__title">Mis Compras</h1>
      {orders.length === 0 ? (
        <p className="order-list__empty">No has realizado compras aún.</p>
      ) : (
        <ul className="order-list__items">
          {orders.map(order => {
            const formattedDate = new Date(order.orderDate).toLocaleDateString('es-MX', {
              year: 'numeric',
              month: 'long',
              day: 'numeric'
            });
            return (
              <li key={order.id} className="order-list__item">
                <span className="order-list__item-id">Orden #{order.id}</span>
                <span className="order-list__item-date">{formattedDate}</span>
                <span className="order-list__item-status">{order.status}</span>
                <span className="order-list__item-total">Total: ${order.totalAmount.toFixed(2)}</span>
                <Link to={`/orders/${order.id}`} className="order-list__item-detail-link">
                  Ver Detalle
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </div>
  );
};

export default OrderListPage;
