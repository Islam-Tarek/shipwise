import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { CartProvider, useCart } from './cart/CartContext'
import Home from './pages/Home'
import Checkout from './pages/Checkout'
import OrderConfirmation from './pages/OrderConfirmation'
import OrderHistory from './pages/OrderHistory'

function Header() {
  const { lines } = useCart()
  return (
    <header className="header">
      <Link to="/" className="brand">
        shipwise
      </Link>
      <nav>
        <Link to="/">products</Link>
        <Link to="/checkout">cart ({lines.length})</Link>
        <Link to="/orders">my orders</Link>
      </nav>
    </header>
  )
}

export default function App() {
  return (
    <CartProvider>
      <BrowserRouter>
        <Header />
        <main className="container">
          <Routes>
            <Route path="/" element={<Home />} />
            <Route path="/checkout" element={<Checkout />} />
            <Route path="/orders/:id" element={<OrderConfirmation />} />
            <Route path="/orders" element={<OrderHistory />} />
          </Routes>
        </main>
      </BrowserRouter>
    </CartProvider>
  )
}
