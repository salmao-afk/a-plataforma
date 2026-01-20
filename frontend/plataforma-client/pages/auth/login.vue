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
}

async function login() {
    isLoading.value = true;
    errorMessage.value = "";

    try {
        const response = await $fetch(`${api}/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json"
            },
            body: loginFormData
        });

        // Guardar token usando o método correto
        authStore.setToken(response.token);

        messages.value.push({
            method: "POST",
            url: `${api}/auth/login`,
            status: 200,
            payload: response
        });

        // Buscar informações do utilizador
        await getUserInfo();

        // Redirecionar para dashboard após login bem-sucedido
        router.push('/');

    } catch (e: any) {
        console.error("login failed:", e);
        errorMessage.value = "Credenciais inválidas. Por favor, tente novamente.";
        messages.value.push({
            method: "POST",
            url: `${api}/auth/login`,
            status: e?.response?.status || 500,
            payload: e
        });
    } finally {
        isLoading.value = false;
    }
}

async function getUserInfo() {
    try {
        const response = await $fetch(`${api}/auth/user`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
                Authorization: `Bearer ${authStore.token}`
            }
        });

        // Guardar user usando o método correto
        authStore.setUser(response);

        messages.value.push({
            method: "GET",
            url: `${api}/auth/user`,
            status: 200,
            payload: response
        });
    } catch (e: any) {
        console.error("user info request failed:", e);
        errorMessage.value = "Erro ao obter informações do utilizador.";
        messages.value.push({
            method: "GET",
            url: `${api}/auth/user`,
            status: e?.response?.status || 500,
            payload: e
        });
    }
}
</script>

<template>
    <div class="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
        <div class="sm:mx-auto sm:w-full sm:max-w-md">
            <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
                Entrar na Plataforma
            </h2>
            <p class="mt-2 text-center text-sm text-gray-600">
                Introduza as suas credenciais
            </p>
        </div>

        <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
            <div class="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">

                <!-- Mensagem de erro -->
                <div v-if="errorMessage" class="mb-4 bg-red-50 border border-red-200 rounded-md p-3">
                    <p class="text-sm text-red-800">{{ errorMessage }}</p>
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
                                class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100"
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
                                class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm disabled:bg-gray-100"
                            />
                        </div>
                    </div>

                    <div class="flex items-center justify-between gap-4">
                        <button
                            type="button"
                            @click="reset"
                            :disabled="isLoading"
                            class="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                        >
                            Reset
                        </button>
                        <button
                            type="submit"
                            :disabled="isLoading"
                            class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 disabled:opacity-50"
                        >
                            <span v-if="!isLoading">Login</span>
                            <span v-else class="flex items-center gap-2">
                <svg class="animate-spin h-4 w-4" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                A entrar...
              </span>
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- Debug info (pode remover em produção) -->
        <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-2xl px-4">
            <div v-if="authStore.token" class="mb-4 bg-green-50 border border-green-200 rounded-md p-4">
                <h3 class="text-sm font-medium text-green-800">✅ Login com Sucesso!</h3>
                <p class="mt-1 text-xs text-green-700">A redirecionar para a dashboard...</p>
            </div>

            <div v-if="messages.length > 0" class="bg-gray-800 rounded-md p-4 shadow-lg text-white">
                <h3 class="text-sm font-bold border-b border-gray-600 pb-2 mb-2">📡 Log de Rede</h3>
                <div class="space-y-3">
                    <div v-for="(msg, i) in messages" :key="i" class="text-xs font-mono">
                        <div class="flex items-center gap-2">
                            <span :class="msg.status === 200 ? 'text-green-400' : 'text-red-400'">[{{ msg.status }}]</span>
                            <span class="text-yellow-300">{{ msg.method }}</span>
                            <span class="text-gray-400">{{ msg.url }}</span>
                        </div>
                        <pre class="mt-1 text-gray-300 opacity-75 overflow-auto max-h-20">{{ msg.payload }}</pre>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
