import { useEffect, useState } from 'react'
import { fetchUserOrders } from '../api/client'
import type { Order } from '../api/types'

export default function OrderHistory() {
  const [orders, setOrders] = useState<Order[] | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    setError(null)
    fetchUserOrders()
      .then(setOrders)
      .catch(err => setError(err instanceof Error ? err.message : 'failed to load orders'))
  }, [])

  return (
    <div>
      <h2>order history</h2>

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
                {i.quantity} x {i.productName} — ${(i.unitPrice * i.quantity).toFixed(2)}
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