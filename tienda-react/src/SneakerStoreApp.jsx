import { useState, useEffect } from 'react'
import { BrowserRouter as Router, Routes, Route, useNavigate } from 'react-router-dom'
import Header from './components/Header'
import Footer from './components/Footer'
import HomePage from './pages/HomePage'
import ProductListPage from './pages/ProductListPage'
import ProductDetailsPage from './pages/ProductDetailsPage'
import CartPage from './pages/CartPage'
import ReturnsPolicyPage from './pages/ReturnsPolicyPage'
import ContactPage from './pages/ContactPage'
import './styles/styles.css'
import { Analytics } from '@vercel/analytics/react'
import OrderListPage from './pages/OrderListPage'
import OrderDetailPage from './pages/OrderDetailPage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import ProtectedRoute from './components/ProtectedRoute'
import { AuthProvider } from './context/AuthContext'
import Notification from './components/Notification'
import { API_ORDERS_URL } from './config/api'

const SneakerStoreApp = () => {
  const navigate = useNavigate();

  const [notification, setNotification] = useState(null);

  const [cartItems, setCartItems] = useState(() => {
    try {
      const storedCart = localStorage.getItem('cartItems');
      return storedCart ? JSON.parse(storedCart) : [];
    } catch (error) {
      console.error("Error al leer el carrito almacenado:", error);
      return [];
    }
  });

  useEffect(() => {
    localStorage.setItem('cartItems', JSON.stringify(cartItems));
  }, [cartItems]);

  const handleAddToCart = (product) => {
    
    // Actualiza el contador de productos en el carrito
    setCartItems((prevItems) => {

      // Revisa si el producto ya está en el carrito
      const existingItem = prevItems.find(item => item.id === product.id);

      if (existingItem) {

        // Si ya está, incrementa la cantidad
        return prevItems.map(item =>
          item.id === product.id ? { ...item, quantity: item.quantity + 1 }
          : item
        );
      }
      
      // Si no está, añade el producto con cantidad 1
      return [...prevItems, { ...product, quantity: 1 }];
    });
  };

  const handleRemoveFromCart = (productId) => {

    // Elimina un producto del carrito
    setCartItems((prevItems) => prevItems.filter(item => item.id !== productId));
  };

  const handleCheckout = async () => {
    if (cartItems.length === 0) {
      setNotification({ type: 'error', message: 'Tu carrito está vacío.' });
      return;
    }

    // Prepara el payload para la API. Asumimos que la API espera un objeto
    // con una lista de productos, donde cada uno tiene id, cantidad y precio.
    const orderItems = cartItems.map(item => ({
      productId: item.id,
      quantity: item.quantity,
      pricePerUnit: item.price
    }));

    try {
      const response = await fetch(API_ORDERS_URL, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        credentials: 'include',
        body: JSON.stringify({ orderItems }),
      });

      if (!response.ok) {
        throw new Error('Error al registrar la compra.');
      }

      const newOrder = await response.json();
      setCartItems([]); // Limpia el carrito
      setNotification({ type: 'success', message: `Compra realizada con éxito. Número de la orden: ${newOrder.id}` });
      navigate(`/orders/${newOrder.id}`); // Redirige al detalle de la nueva orden

    } catch (error) {
      console.error("Error en el checkout:", error);
      setNotification({ type: 'error', message: `Hubo un problema al procesar tu compra: ${error.message}` });
      throw error; // Relanzamos el error para que el componente CartPage pueda manejarlo
    }
  };

  return (
    <>
      <Notification
        type={notification?.type}
        message={notification?.message}
        onClose={() => setNotification(null)}
      />
      <Header cartCount={cartItems.reduce((total, item) => total + item.quantity, 0)} />
      <main>
        <Routes>
          <Route path='/login' element={<LoginPage />} />
          <Route path='/register' element={<RegisterPage />} />
          <Route path='/' element={<ProtectedRoute><HomePage onAddToCart={handleAddToCart} /></ProtectedRoute>} />
          <Route path='/products' element={<ProtectedRoute><ProductListPage onAddToCart={handleAddToCart} /></ProtectedRoute>} />
          <Route path='/products/:id' element={<ProtectedRoute><ProductDetailsPage onAddToCart={handleAddToCart} /></ProtectedRoute>} />
          <Route path='/cart' element={<ProtectedRoute><CartPage cartItems={cartItems} onRemoveFromCart={handleRemoveFromCart} onCheckout={handleCheckout} /></ProtectedRoute>} />
          <Route path="/returns" element={<ProtectedRoute><ReturnsPolicyPage /></ProtectedRoute>} />
          <Route path="/contact" element={<ProtectedRoute><ContactPage /></ProtectedRoute>} />
          <Route path="/orders" element={<ProtectedRoute><OrderListPage /></ProtectedRoute>} />
          <Route path="/orders/:id" element={<ProtectedRoute><OrderDetailPage /></ProtectedRoute>} />
        </Routes>
      </main>
      <Footer />
      <Analytics />
    </>
  )
}

const AppWrapper = () => (
  <Router>
    <AuthProvider>
      <SneakerStoreApp />
    </AuthProvider>
  </Router>
);

export default AppWrapper;
