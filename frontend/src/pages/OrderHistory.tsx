import { useState } from 'react'
import { fetchCustomerOrders } from '../api/client'
import type { Order } from '../api/types'

export default function OrderHistory() {
  const [customerId, setCustomerId] = useState('')
  const [orders, setOrders] = useState<Order[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  async function load(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    try {
      setOrders(await fetchCustomerOrders(Number(customerId)))
    } catch (err) {
      setError(err instanceof Error ? err.message : 'failed to load orders')
    }
  }

  return (
    <div>
      <h2>order history</h2>
      <form onSubmit={load} className="toolbar">
        <input
          placeholder="your customer id"
          type="number"
          value={customerId}
          onChange={e => setCustomerId(e.target.value)}
          required
        />
        <button>load</button>
      </form>

      {error && <p className="error">{error}</p>}
      {orders?.length === 0 && <p className="muted">no orders yet</p>}
      {orders?.map(o => (
        <div key={o.id} className="card order-card">
          <h3>
            order #{o.id} — {o.status.toLowerCase()}
          </h3>
          <ul className="summary">
            {o.items.map(i => (
              <li key={i.productId}>
                {i.quantity} x {i.productName}
              </li>
            ))}
          </ul>
          <p>
            <strong>${o.totalAmount.toFixed(2)}</strong>
          </p>
        </div>
      ))}
    </div>
  )
}
