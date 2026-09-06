import type {
  AuthResponse,
  Category,
  LoginRequest,
  Order,
  Page,
  Product,
  RegisterRequest,
} from './types'

const BASE_URL = 'http://localhost:9000/api'

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem('shipwise_token')
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(init?.headers ?? {}),
    },
    ...init,
  })
  if (res.status === 401) {
    localStorage.removeItem('shipwise_token')
    localStorage.removeItem('shipwise_user')
    window.dispatchEvent(new Event('shipwise:unauthorized'))
  }
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

export interface CheckoutItem {
  productId: number
  quantity: number
  productName: string
  unitPrice: number
  imageUrl?: string
}

export interface CheckoutPayload {
  customerName: string
  customerEmail: string
  address?: string
  items: CheckoutItem[]
}

export function checkout(payload: CheckoutPayload) {
  return request<Order>('/orders', { method: 'POST', body: JSON.stringify(payload) })
}

export function register(payload: RegisterRequest) {
  return request<AuthResponse>('/users', { method: 'POST', body: JSON.stringify(payload) })
}

export function login(payload: LoginRequest) {
  return request<AuthResponse>('/users/login', { method: 'POST', body: JSON.stringify(payload) })
}

export function fetchUserOrders() {
  return request<Order[]>('/orders')
}
