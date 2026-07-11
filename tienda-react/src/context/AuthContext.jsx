import { createContext, useContext, useState, useEffect } from 'react'

const AuthContext = createContext(null)

const getGatewayUrl = () =>
    import.meta.env.VITE_API_GATEWAY_URL || 'http://localhost:8762'

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null)
    const [loading, setLoading] = useState(true)

    useEffect(() => {
        fetch(`${getGatewayUrl()}/userservice/auth/me`, { credentials: 'include' })
            .then(res => (res.ok ? res.json() : null))
            .then(data => { setUser(data); setLoading(false) })
            .catch(() => setLoading(false))
    }, [])

    const login = async (email, password) => {
        const res = await fetch(`${getGatewayUrl()}/userservice/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ email, password }),
        })
        if (!res.ok) {
            const err = await res.json().catch(() => ({}))
            throw new Error(err.message || 'Credenciales inválidas')
        }
        const data = await res.json()
        setUser(data)
        return data
    }

    const register = async (username, email, password) => {
        const res = await fetch(`${getGatewayUrl()}/userservice/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ username, email, password }),
        })
        if (!res.ok) {
            const err = await res.json().catch(() => ({}))
            throw new Error(err.message || Object.values(err)[0] || 'Error al registrarse')
        }
        const data = await res.json()
        setUser(data)
        return data
    }

    const logout = async () => {
        await fetch(`${getGatewayUrl()}/userservice/auth/logout`, {
            method: 'POST',
            credentials: 'include',
        })
        setUser(null)
    }

    return (
        <AuthContext.Provider value={{ user, loading, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    )
}

export const useAuth = () => useContext(AuthContext)
