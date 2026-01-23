<script setup lang="ts">
import { reactive, ref } from "vue";
import { useAuthStore } from "~/stores/auth-store.js";

const config = useRuntimeConfig();
const api = config.public.apiBase;
const router = useRouter();
const authStore = useAuthStore();

const loginFormData = reactive({ username: "", password: "" });
const isLoading = ref(false);
const errorMessage = ref("");

function reset() {
  authStore.logout();
  errorMessage.value = "";
  loginFormData.username = "";
  loginFormData.password = "";
}

async function login() {
  isLoading.value = true;
  errorMessage.value = "";

  try {
    // 1. Login
    const response = await $fetch(`${api}/auth/login`, {
      method: "POST",
      body: loginFormData
    });

    authStore.setToken(response.token);

    // 2. Buscar User
    await getUserInfo();

    router.push('/');
  } catch (e: any) {
    authStore.logout();
    if (e.response?.status === 403) errorMessage.value = "Conta suspensa. Contacte o admin.";
    else if (e.response?.status === 401) errorMessage.value = "Credenciais inválidas.";
    else errorMessage.value = "Erro de servidor.";
  } finally {
    isLoading.value = false;
  }
}

async function getUserInfo() {
  // CORREÇÃO: URL alinhado com UserService.java
  const response = await $fetch(`${api}/utilizadores/me`, {
    headers: { Authorization: `Bearer ${authStore.token}` }
  });
  // O backend retorna uma lista [UserDTO], pegamos o primeiro
  const data = Array.isArray(response) ? response[0] : response;
  authStore.setUser(data);
}
</script>

<template>
  <div class="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">
    <div class="sm:mx-auto sm:w-full sm:max-w-md">
      <h2 class="text-center text-3xl font-extrabold text-gray-900">Entrar</h2>
    </div>

    <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
      <div class="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">
        <div v-if="errorMessage" class="mb-4 text-red-600 font-bold text-center border border-red-200 bg-red-50 p-2 rounded">
          {{ errorMessage }}
        </div>

        <form class="space-y-6" @submit.prevent="login">
          <div>
            <label class="block text-sm font-medium text-gray-700">Username</label>
            <input v-model="loginFormData.username" type="text" required class="w-full px-3 py-2 border border-gray-300 rounded-md" />
          </div>

          <div>
            <label class="block text-sm font-medium text-gray-700">Password</label>
            <input v-model="loginFormData.password" type="password" required class="w-full px-3 py-2 border border-gray-300 rounded-md" />
          </div>

          <div class="flex justify-end">
            <NuxtLink to="/auth/esqueci-password" class="text-sm font-medium text-indigo-600 hover:text-indigo-500">
              Esqueceu-se da password?
            </NuxtLink>
          </div>

          <button type="submit" :disabled="isLoading" class="w-full py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50">
            {{ isLoading ? 'A entrar...' : 'Entrar' }}
          </button>
        </form>
      </div>
    </div>
  </div>
</template>