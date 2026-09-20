import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { CartProvider, useCart } from './cart/CartContext'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Home from './pages/Home'
import Checkout from './pages/Checkout'
import OrderConfirmation from './pages/OrderConfirmation'
import OrderHistory from './pages/OrderHistory'
import Login from './pages/Login'
import Register from './pages/Register'
import ProtectedRoute from './components/ProtectedRoute'

function Header() {
  const { lines } = useCart()
  const { user, logout } = useAuth()
  return (
    <header className="header">
      <Link to="/" className="brand">
        shipwise
      </Link>
      <nav>
        <Link to="/">products</Link>
        <Link to="/checkout">cart ({lines.length})</Link>
        <Link to="/orders">my orders</Link>
        {user ? (
          <span className="user-menu">
            <span className="muted">{user.name}</span>
            <button className="link" onClick={logout}>
              logout
            </button>
          </span>
        ) : (
          <>
            <Link to="/login">log in</Link>
            <Link to="/register">register</Link>
          </>
        )}
      </nav>
    </header>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          <Header />
          <main className="container">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route
                path="/checkout"
                element={
                  <ProtectedRoute>
                    <Checkout />
                  </ProtectedRoute>
                }
              />
              <Route
                path="/orders"
                element={
                  <ProtectedRoute>
                    <OrderHistory />
                  </ProtectedRoute>
                }
              />
              <Route path="/orders/:id" element={<OrderConfirmation />} />
            </Routes>
          </main>
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  )
}