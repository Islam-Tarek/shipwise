import { useLocation, useParams } from 'react-router-dom'
import type { Order } from '../api/types'
import { orderTotal } from '../api/types'
import { useEffect, useState } from 'react'

export default function OrderConfirmation() {
  const { id } = useParams()
  const location = useLocation()
  const [order] = useState<Order | null>((location.state as { order?: Order })?.order ?? null)

  useEffect(() => {
    if (!order) {
      // order came from a page refresh, nothing cached to show
    }
  }, [order])

  if (!order) {
    return <p className="muted">order #{id} placed. refresh the orders page to see it.</p>
  }

  return (
    <div className="confirmation">
      <h2>order #{order.id} confirmed</h2>
      <p className="muted">thank you, {order.customerName}</p>
      <ul className="summary">
        {order.items.map(i => (
          <li key={i.productId}>
            {i.quantity} x {i.productName} — ${(i.unitPrice * i.quantity).toFixed(2)}
          </li>
        ))}
      </ul>
      <p>
        <strong>total: ${orderTotal(order).toFixed(2)}</strong>
      </p>
    </div>
  )
}
