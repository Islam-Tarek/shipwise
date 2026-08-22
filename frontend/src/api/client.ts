import type { Category, Order, Page, Product } from './types'

const BASE_URL = 'http://localhost:8080/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  })
  if (!res.ok) {
    const body = await res.json().catch(() => null)
    throw new Error(body?.detail ?? `request failed with status ${res.status}`)
  }
  return res.json()
}

export function fetchProducts(page = 0, size = 12, name?: string) {
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  if (name) params.set('name', name)
  return request<Page<Product>>(`/products?${params}`)
}

export function fetchCategories() {
  return request<Category[]>('/categories')
}

export interface CheckoutPayload {
  customer: { name: string; email: string; address?: string }
  items: { productId: number; quantity: number }[]
}

export function checkout(payload: CheckoutPayload) {
  return request<Order>('/orders', { method: 'POST', body: JSON.stringify(payload) })
}

export function fetchCustomerOrders(customerId: number) {
  return request<Order[]>(`/orders?customerId=${customerId}`)
}
