import { useState } from 'react'
import type { Product } from '../api/types'
import ProductGrid from './ProductGrid'
import { useCart } from '../cart/CartContext'

export default function Home() {
  const [selected, setSelected] = useState<Product | null>(null)
  const { add } = useCart()

  return (
    <div>
      {selected && (
        <div className="modal-backdrop" onClick={() => setSelected(null)}>
          <div className="modal" onClick={e => e.stopPropagation()}>
            <img src={selected.imageUrl ?? ''} alt={selected.name} />
            <h2>{selected.name}</h2>
            <p>{selected.description}</p>
            <p className="price">${selected.price.toFixed(2)}</p>
            <button
              onClick={() => {
                add(selected)
                setSelected(null)
              }}
            >
              add to cart
            </button>
          </div>
        </div>
      )}
      <ProductGrid
        onOpenProduct={setSelected}
      />
    </div>
  )
}
