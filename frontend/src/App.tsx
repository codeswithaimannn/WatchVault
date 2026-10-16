import { Route, Routes } from 'react-router'

import { GuestOnly, RequireAuth } from './auth/routeGuards'
import { AuthLayout } from './components/AuthLayout'
import { Layout } from './components/Layout'
import { AboutPage } from './pages/AboutPage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { RegisterPage } from './pages/RegisterPage'
import { SearchPage } from './pages/SearchPage'
import { TitlePage } from './pages/TitlePage'

/** All routes. The router itself is created in main.tsx (and a test router in tests). */
export function App() {
  return (
    <Routes>
      {/* Login and register: full-screen cinematic layout, no app header. */}
      <Route element={<GuestOnly />}>
        <Route element={<AuthLayout />}>
          <Route path="login" element={<LoginPage />} />
          <Route path="register" element={<RegisterPage />} />
        </Route>
      </Route>

      {/* Everything else: the app layout with the header. */}
      <Route element={<Layout />}>
        <Route element={<RequireAuth />}>
          <Route index element={<HomePage />} />
          <Route path="search" element={<SearchPage />} />
          <Route path="title/:mediaType/:tmdbId" element={<TitlePage />} />
        </Route>
        {/* Public: data credits must be reachable without an account. */}
        <Route path="about" element={<AboutPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Route>
    </Routes>
  )
}
