import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Dev server proxies API + OAuth callback to the Spring backend on :8081,
// so the browser only ever talks to :5173 (no CORS preflight in dev).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8081', changeOrigin: true },
      '/google': { target: 'http://localhost:8081', changeOrigin: true },
    },
  },
});
