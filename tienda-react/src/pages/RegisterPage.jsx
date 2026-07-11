import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

const RegisterPage = () => {
    const { register } = useAuth()
    const navigate = useNavigate()
    const [username, setUsername] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [error, setError] = useState('')
    const [loading, setLoading] = useState(false)

    const handleSubmit = async (e) => {
        e.preventDefault()
        setError('')
        setLoading(true)
        try {
            await register(username, email, password)
            navigate('/')
        } catch (err) {
            setError(err.message)
        } finally {
            setLoading(false)
        }
    }

    return (
        <div className='auth-page'>
            <div className='auth-card'>
                <h2 className='auth-card__title'>Crear cuenta</h2>
                <form className='auth-card__form' onSubmit={handleSubmit}>
                    <div className='auth-card__field'>
                        <label htmlFor='username'>Nombre de usuario</label>
                        <input
                            id='username'
                            type='text'
                            value={username}
                            onChange={e => setUsername(e.target.value)}
                            required
                            autoComplete='username'
                        />
                    </div>
                    <div className='auth-card__field'>
                        <label htmlFor='email'>Email</label>
                        <input
                            id='email'
                            type='email'
                            value={email}
                            onChange={e => setEmail(e.target.value)}
                            required
                            autoComplete='email'
                        />
                    </div>
                    <div className='auth-card__field'>
                        <label htmlFor='password'>Contraseña</label>
                        <input
                            id='password'
                            type='password'
                            value={password}
                            onChange={e => setPassword(e.target.value)}
                            required
                            autoComplete='new-password'
                            minLength={6}
                        />
                    </div>
                    {error && <p className='auth-card__error'>{error}</p>}
                    <button
                        type='submit'
                        className='auth-card__submit'
                        disabled={loading}
                    >
                        {loading ? 'Registrando...' : 'Crear cuenta'}
                    </button>
                </form>
                <p className='auth-card__footer'>
                    ¿Ya tienes cuenta?{' '}
                    <Link to='/login'>Inicia sesión</Link>
                </p>
            </div>
        </div>
    )
}

export default RegisterPage
