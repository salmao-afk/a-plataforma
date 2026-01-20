import {defineStore} from "pinia";
import { ref } from "vue";

export const useAuthStore = defineStore("authStore", () => {
    // Verificar se está no navegador (client-side)
    const isClient = typeof window !== 'undefined';

    // Inicializar do localStorage apenas no cliente
    const token = ref(isClient ? localStorage.getItem('token') : null)
    const user = ref(isClient ? JSON.parse(localStorage.getItem('user') || 'null') : null)

    function setToken(newToken) {
        token.value = newToken
        if (isClient) {
            if (newToken) {
                localStorage.setItem('token', newToken)
            } else {
                localStorage.removeItem('token')
            }
        }
    }

    function setUser(newUser) {
        user.value = newUser
        if (isClient) {
            if (newUser) {
                localStorage.setItem('user', JSON.stringify(newUser))
            } else {
                localStorage.removeItem('user')
            }
        }
    }

    function logout() {
        token.value = null
        user.value = null
        if (isClient) {
            localStorage.removeItem('token')
            localStorage.removeItem('user')
        }
    }

    return {
        token,
        user,
        setToken,
        setUser,
        logout
    }
})
