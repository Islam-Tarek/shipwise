import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [address, setAddress] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await register({ name, email, password, address: address || undefined })
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'registration failed')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth">
      <h2>create account</h2>
      <form onSubmit={submit}>
        <input
          placeholder="full name"
          value={name}
          onChange={e => setName(e.target.value)}
          required
        />
        <input
          placeholder="email"
          type="email"
          value={email}
          onChange={e => setEmail(e.target.value)}
          required
        />
        <input
          placeholder="password (min 6 characters)"
          type="password"
          minLength={6}
          value={password}
          onChange={e => setPassword(e.target.value)}
          required
        />
        <input
          placeholder="address"
          value={address}
          onChange={e => setAddress(e.target.value)}
        />
        {error && <p className="error">{error}</p>}
        <button disabled={submitting}>{submitting ? 'creating account...' : 'create account'}</button>
      </form>
      <p className="muted">
        already have an account? <Link to="/login">log in</Link>
      </p>
    </div>
  )
}