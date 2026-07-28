import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import './styles/styles.css'
import SneakerStoreApp from './SneakerStoreApp.jsx'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <SneakerStoreApp />
  </StrictMode>,
)
