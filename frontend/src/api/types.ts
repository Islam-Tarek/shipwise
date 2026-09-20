export interface Category {
  id: number
  name: string
  slug: string
}

export interface Product {
  id: number
  name: string
  description: string | null
  price: number
  imageUrl: string | null
  stockQuantity: number
  categorySlug: string | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  page: number
  size: number
}

export interface OrderItemView {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
}

export interface Order {
  id: number
  userId: number | null
  customerName: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  totalAmount: number
  items: OrderItemView[]
  createdAt: string
}

export function orderTotal(order: Pick<Order, 'items' | 'totalAmount'>): number {
  if (typeof order.totalAmount === 'number' && Number.isFinite(order.totalAmount)) {
    return order.totalAmount
  }
  return order.items.reduce((sum, i) => sum + i.unitPrice * i.quantity, 0)
}

export interface User {
  id: number
  name: string
  email: string
  address: string | null
}

export interface AuthResponse {
  token: string
  user: User
}

export interface RegisterRequest {
  name: string
  email: string
  password: string
  address?: string
}

export interface LoginRequest {
  email: string
  password: string
}
