import { useEffect, useState } from 'react'
import { fetchCategories, fetchProducts } from '../api/client'
import type { Category, Product } from '../api/types'
import { useCart } from '../cart/CartContext'

interface Props {
  onOpenProduct: (product: Product) => void
}

export default function ProductGrid({ onOpenProduct }: Props) {
  const [products, setProducts] = useState<Product[]>([])
  const [categories, setCategories] = useState<Category[]>([])
  const [category, setCategory] = useState<string>('')
  const [search, setSearch] = useState('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(1)
  const [loading, setLoading] = useState(true)
  const { add } = useCart()

  useEffect(() => {
    fetchCategories().then(setCategories).catch(console.error)
  }, [])

  useEffect(() => {
    setLoading(true)
    const name = [search, category].filter(Boolean).join(' ')
    fetchProducts(page, 12, name || undefined)
      .then(res => {
        setProducts(res.content)
        setTotalPages(Math.max(1, Math.ceil(res.totalElements / res.size)))
      })
      .catch(console.error)
      .finally(() => setLoading(false))
  }, [page, search, category])

  return (
    <div>
      <div className="toolbar">
        <input
          placeholder="search products..."
          value={search}
          onChange={e => {
            setSearch(e.target.value)
            setPage(0)
          }}
        />
        <select
          value={category}
          onChange={e => {
            setCategory(e.target.value)
            setPage(0)
          }}
        >
          <option value="">all categories</option>
          {categories.map(c => (
            <option key={c.id} value={c.name}>
              {c.name}
            </option>
          ))}
        </select>
      </div>

      {loading ? (
        <p className="muted">loading...</p>
      ) : (
        <div className="grid">
          {products.map(p => (
            <div key={p.id} className="card" onClick={() => onOpenProduct(p)}>
              <img src={p.imageUrl ?? ''} alt={p.name} loading="lazy" />
              <h3>{p.name}</h3>
              <p className="price">${p.price.toFixed(2)}</p>
              <p className="muted">{p.categorySlug}</p>
              <button
                disabled={p.stockQuantity === 0}
                onClick={e => {
                  e.stopPropagation()
                  add(p)
                }}
              >
                {p.stockQuantity === 0 ? 'out of stock' : 'add to cart'}
              </button>
            </div>
          ))}
        </div>
      )}

      <div className="pager">
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>
          prev
        </button>
        <span className="muted">
          page {page + 1} of {totalPages}
        </span>
        <button disabled={page >= totalPages - 1} onClick={() => setPage(p => p + 1)}>
          next
        </button>
      </div>
    </div>
  )
}
