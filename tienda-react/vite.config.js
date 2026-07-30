import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// Cabeceras de seguridad (CSP incluida) para los servidores de dev y preview.
// En producción (Vercel) las mismas cabeceras se aplican via vercel.json.
function securityHeaders(gatewayUrl) {
  const gatewayOrigin = gatewayUrl
    ? new URL(gatewayUrl).origin
    : 'http://localhost:8762'

  const buildCsp = (scriptSrc) =>
    [
      "default-src 'self'",
      `script-src ${scriptSrc}`,
      "style-src 'self' 'unsafe-inline'",
      `img-src 'self' data: ${gatewayOrigin}`,
      "font-src 'self'",
      `connect-src 'self' ${gatewayOrigin} https://vitals.vercel-insights.com https://va.vercel-scripts.com`,
      "frame-ancestors 'none'",
      "base-uri 'self'",
      "form-action 'self'",
      "object-src 'none'",
    ].join('; ')

  // `vite dev` inyecta un <script> inline para React Fast Refresh en cada
  // index.html, así que script-src necesita 'unsafe-inline' solo en dev.
  // El build de producción (servido por `vite preview` y por Vercel) no
  // tiene scripts inline, así que ahí se mantiene estricto.
  const devCsp = buildCsp("'self' 'unsafe-inline'")
  const previewCsp = buildCsp("'self'")

  const applyHeaders = (csp) => (server) => {
    server.middlewares.use((_req, res, next) => {
      res.setHeader('Content-Security-Policy', csp)
      res.setHeader('X-Content-Type-Options', 'nosniff')
      res.setHeader('X-Frame-Options', 'DENY')
      res.setHeader('Referrer-Policy', 'strict-origin-when-cross-origin')
      next()
    })
  }

  return {
    name: 'security-headers',
    configureServer: applyHeaders(devCsp),
    configurePreviewServer: applyHeaders(previewCsp),
  }
}

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [react(), securityHeaders(env.VITE_API_GATEWAY_URL)],
    server: {
      proxy: {
        '/productservice': {
          target: 'http://localhost',
          changeOrigin: true,
        },
        '/orderservice': {
          target: 'http://localhost',
          changeOrigin: true,
        },
      }
    },
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.js',
      globals: true,
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/**/*.{js,jsx}'],
        exclude: ['src/main.jsx', 'src/test/**'],
      },
    },
  }
})
