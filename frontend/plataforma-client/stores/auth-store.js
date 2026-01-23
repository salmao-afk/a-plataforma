import { defineStore } from "pinia";
import { ref } from "vue";

export const useAuthStore = defineStore("authStore", () => {
    // Verificar se está no navegador (client-side)
    const isClient = typeof window !== 'undefined';

    // Inicializar do localStorage apenas no cliente
    const token = ref(isClient ? localStorage.getItem('token') : null)

    // Tenta fazer parse, se der erro assume null
    let storedUser = null;
    try {
        if (isClient) storedUser = JSON.parse(localStorage.getItem('user') || 'null');
    } catch (e) {
        console.error("Erro ao ler user do localStorage", e);
    }
    const user = ref(storedUser);

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
        // --- A CORREÇÃO MÁGICA ESTÁ AQUI ---
        // Se recebermos um objeto que tem uma propriedade 'user' lá dentro (UserProfileDTO),
        // extraímos apenas o 'user' real. Caso contrário, usamos o objeto todo.
        let finalUser = newUser;

        if (newUser && newUser.user) {
            finalUser = newUser.user;
        }

        user.value = finalUser;

        if (isClient) {
            if (finalUser) {
                localStorage.setItem('user', JSON.stringify(finalUser))
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