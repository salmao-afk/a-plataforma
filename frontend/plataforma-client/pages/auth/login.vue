<script setup lang="ts">
import { reactive, ref } from "vue";
import { useAuthStore } from "~/stores/auth-store.js";

const config = useRuntimeConfig();
const api = config.public.apiBase;
const router = useRouter();

const loginFormData = reactive({
    username: "",
    password: ""
});

const authStore = useAuthStore();
const messages = ref([]);
const isLoading = ref(false);
const errorMessage = ref("");

function reset() {
    authStore.logout();
    messages.value = [];
    errorMessage.value = "";
    loginFormData.username = "";
    loginFormData.password = "";
}

async function login() {
    isLoading.value = true;
    errorMessage.value = "";

    try {
        // 1. Tentar Login
        const response = await $fetch(`${api}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: loginFormData
        });

        // Guardar token temporariamente
        authStore.setToken(response.token);

        // 2. Tentar obter dados do User (É aqui que vai falhar se estiver suspenso)
        await getUserInfo();

        // 3. Se chegou aqui, é sucesso absoluto
        router.push('/');

    } catch (e: any) {
        console.error("Login ou UserInfo falhou:", e);

        // Se houver erro, garantimos que não fica nenhum token guardado
        authStore.logout();

        // Tratamento de Erros Personalizado
        if (e.response?.status === 403) {
            // ERRO ESPECÍFICO PARA USER SUSPENSO
            errorMessage.value = "A sua conta está suspensa, por favor fale com um admin.";
        } else if (e.response?.status === 401) {
            errorMessage.value = "Credenciais inválidas. Por favor, tente novamente.";
        } else {
            errorMessage.value = "Erro de conexão ou servidor. Tente mais tarde.";
        }
    } finally {
        isLoading.value = false;
    }
}

// Removi o try/catch daqui para que o erro "suba" para a função login
async function getUserInfo() {
    const response = await $fetch(`${api}/auth/user`, {
        method: "GET",
        headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
            Authorization: `Bearer ${authStore.token}`
        }
    });

    // Se o fetch acima der erro (403), o código para aqui e vai para o catch do login()
    authStore.setUser(response);
}
</script>

<template>
    <div class="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
        <div class="sm:mx-auto sm:w-full sm:max-w-md">
            <div class="mx-auto h-12 w-12 bg-indigo-600 rounded-lg flex items-center justify-center text-white font-bold text-xl mb-4">
                P
            </div>
            <h2 class="text-center text-3xl font-extrabold text-gray-900">
                Entrar na Plataforma
            </h2>
            <p class="mt-2 text-center text-sm text-gray-600">
                Introduza as suas credenciais
            </p>
        </div>

        <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div class="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">

                <div v-if="errorMessage" class="mb-6 bg-red-50 border-l-4 border-red-500 p-4 rounded-r-md">
                    <div class="flex">
                        <div class="flex-shrink-0">
                            <svg class="h-5 w-5 text-red-400" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20" fill="currentColor">
                                <path fill-rule="evenodd" d="M10 18a8 8 0 100-16 8 8 0 000 16zM8.707 7.293a1 1 0 00-1.414 1.414L8.586 10l-1.293 1.293a1 1 0 101.414 1.414L10 11.414l1.293 1.293a1 1 0 001.414-1.414L11.414 10l1.293-1.293a1 1 0 00-1.414-1.414L10 8.586 8.707 7.293z" clip-rule="evenodd" />
                            </svg>
                        </div>
                        <div class="ml-3">
                            <p class="text-sm text-red-700 font-bold">
                                {{ errorMessage }}
                            </p>
                        </div>
                    </div>
                </div>

                <form class="space-y-6" @submit.prevent="login">
                    <div>
                        <label for="username" class="block text-sm font-medium text-gray-700">
                            Username
                        </label>
                        <div class="mt-1">
                            <input
                                id="username"
                                v-model="loginFormData.username"
                                type="text"
                                required
                                :disabled="isLoading"
                                class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100 transition"
                            />
                        </div>
                    </div>

                    <div>
                        <label for="password" class="block text-sm font-medium text-gray-700">
                            Password
                        </label>
                        <div class="mt-1">
                            <input
                                id="password"
                                v-model="loginFormData.password"
                                type="password"
                                required
                                :disabled="isLoading"
                                class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100 transition"
                            />
                        </div>
                    </div>

                    <div class="flex items-center justify-between gap-4 pt-2">
                        <button
                            type="button"
                            @click="reset"
                            :disabled="isLoading"
                            class="w-1/3 flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 transition"
                        >
                            Limpar
                        </button>
                        <button
                            type="submit"
                            :disabled="isLoading"
                            class="w-2/3 flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
                        >
                            <span v-if="!isLoading">Entrar</span>
                            <span v-else class="flex items-center gap-2">
                                <svg class="animate-spin h-4 w-4 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                                A validar...
                            </span>
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</template>
