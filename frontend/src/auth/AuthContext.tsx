import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import * as api from '../api/client'
import type { LoginRequest, RegisterRequest, User } from '../api/types'

const TOKEN_KEY = 'shipwise_token'
const USER_KEY = 'shipwise_user'

interface AuthContextValue {
  user: User | null
  token: string | null
  login: (req: LoginRequest) => Promise<void>
  register: (req: RegisterRequest) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

function readUser(): User | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as User
  } catch {
    return null
  }
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(readUser)
  const [token, setToken] = useState<string | null>(() => localStorage.getItem(TOKEN_KEY))

  useEffect(() => {
    const onUnauthorized = () => setUser(null)
    window.addEventListener('shipwise:unauthorized', onUnauthorized)
    return () => window.removeEventListener('shipwise:unauthorized', onUnauthorized)
  }, [])

  const value = useMemo<AuthContextValue>(() => {
    const login = async (req: LoginRequest) => {
      const res = await api.login(req)
      localStorage.setItem(TOKEN_KEY, res.token)
      localStorage.setItem(USER_KEY, JSON.stringify(res.user))
      setToken(res.token)
      setUser(res.user)
    }
    const register = async (req: RegisterRequest) => {
      const res = await api.register(req)
      localStorage.setItem(TOKEN_KEY, res.token)
      localStorage.setItem(USER_KEY, JSON.stringify(res.user))
      setToken(res.token)
      setUser(res.user)
    }
    const logout = () => {
      localStorage.removeItem(TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
      setToken(null)
      setUser(null)
    }
    return { user, token, login, register, logout }
  }, [user, token])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}