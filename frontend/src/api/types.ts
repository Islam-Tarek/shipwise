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
  customerId: number
  customerName: string
  status: 'PENDING' | 'CONFIRMED' | 'CANCELLED'
  totalAmount: number
  items: OrderItemView[]
  createdAt: string
}
