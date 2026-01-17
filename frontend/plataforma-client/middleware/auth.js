// middleware/auth.js
export default defineNuxtRouteMiddleware((to, from) => {
    const authStore = useAuthStore()

    // Se não tiver token, manda para o login
    if (!authStore.token) {
        return navigateTo('/login')
    }
})