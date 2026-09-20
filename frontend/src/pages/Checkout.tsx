import { useNavigate } from 'react-router-dom'
import { checkout } from '../api/client'
import { useCart } from '../cart/CartContext'
import { useAuth } from '../auth/AuthContext'
import type { Order } from '../api/types'
import { useState } from 'react'

export default function Checkout() {
  const { lines, total, remove, clear } = useCart()
  const { user, token } = useAuth()
  const [name, setName] = useState(user?.name ?? '')
  const [email, setEmail] = useState(user?.email ?? '')
  const [address, setAddress] = useState(user?.address ?? '')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const navigate = useNavigate()

  if (!token) {
    return <p className="muted">log in to check out.</p>
  }

  if (lines.length === 0) {
    return <p className="muted">your cart is empty</p>
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      const order: Order = await checkout({
        customerName: name,
        customerEmail: email,
        address: address || undefined,
        items: lines.map(l => ({
          productId: l.product.id,
          quantity: l.quantity,
          productName: l.product.name,
          unitPrice: l.product.price,
          imageUrl: l.product.imageUrl ?? undefined,
        })),
      })
      clear()
      navigate(`/orders/${order.id}`, { state: { order } })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'checkout failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="checkout">
      <h2>checkout</h2>
      <ul className="summary">
        {lines.map(l => (
          <li key={l.product.id}>
            {l.quantity} x {l.product.name} — ${(l.product.price * l.quantity).toFixed(2)}
            <button className="link" onClick={() => remove(l.product.id)}>
              remove
            </button>
          </li>
        ))}
      </ul>
      <p>
        <strong>total: ${total.toFixed(2)}</strong>
      </p>

      <form onSubmit={submit}>
        <input placeholder="full name" value={name} onChange={e => setName(e.target.value)} required />
        <input placeholder="email" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        <input placeholder="address" value={address} onChange={e => setAddress(e.target.value)} />
        {error && <p className="error">{error}</p>}
        <button disabled={submitting}>{submitting ? 'placing order...' : 'place order'}</button>
      </form>
    </div>
  )
}