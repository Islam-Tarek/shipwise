import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'
import type { Product } from '../api/types'

export interface CartLine {
  product: Product
  quantity: number
}

interface CartContextValue {
  lines: CartLine[]
  add: (product: Product, quantity?: number) => void
  remove: (productId: number) => void
  clear: () => void
  total: number
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [lines, setLines] = useState<CartLine[]>([])

  const value = useMemo<CartContextValue>(() => {
    const add = (product: Product, quantity = 1) => {
      setLines(prev => {
        const existing = prev.find(l => l.product.id === product.id)
        if (existing) {
          return prev.map(l =>
            l.product.id === product.id
              ? { ...l, quantity: Math.min(l.quantity + quantity, product.stockQuantity) }
              : l,
          )
        }
        return [...prev, { product, quantity }]
      })
    }
    const remove = (productId: number) =>
      setLines(prev => prev.filter(l => l.product.id !== productId))
    const total = lines.reduce((sum, l) => sum + l.product.price * l.quantity, 0)
    return { lines, add, remove, clear: () => setLines([]), total }
  }, [lines])

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart() {
  const ctx = useContext(CartContext)
  if (!ctx) throw new Error('useCart must be used within CartProvider')
  return ctx
}
