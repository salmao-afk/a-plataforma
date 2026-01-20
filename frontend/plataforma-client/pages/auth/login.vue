<script setup lang="ts">
import { reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { useAuthStore } from "~/stores/auth-store.js";
import { useRouter } from "vue-router"; // 1. Importar Router

const config = useRuntimeConfig();
const api = config.public.apiBase;
const router = useRouter(); // 2. Inicializar Router

const loginFormData = reactive({
  username: "",
  password: ""
});

const authStore = useAuthStore();
const { token, user } = storeToRefs(authStore);

const messages = ref<any[]>([]);

function reset() {
  authStore.logout();
  messages.value = [];
}

async function login() {
  try {
    const response = await $fetch<any>(`${api}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
      },
      body: loginFormData
    });

    token.value = response.token;

    messages.value.push({
      method: "POST",
      url: `${api}/auth/login`,
      status: 200,
      payload: response
    });

    // Buscar dados do utilizador
    await getUserInfo();

    // 3. SE CHEGOU AQUI SEM ERROS, REDIRECIONA PARA A DASHBOARD
    // Dei um pequeno timeout de 500ms só para veres o log verde a piscar antes de mudar de página
    // (Podes remover o setTimeout e deixar só o router.push se quiseres instantâneo)
    setTimeout(() => {
      router.push('/dashboard');
    }, 500);

  } catch (e: any) {
    console.error("login failed:", e);
    messages.value.push({
      method: "POST",
      url: `${api}/auth/login`,
      status: e?.response?.status || 500,
      payload: e
    });
  }
}

async function getUserInfo() {
  try {
    const response = await $fetch(`${api}/auth/user`, {
      method: "GET",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json",
        Authorization: `Bearer ${token.value}`
      }
    });

    user.value = response;

    messages.value.push({
      method: "GET",
      url: `${api}/auth/user`,
      status: 200,
      payload: response
    });
  } catch (e: any) {
    console.error("user info request failed:", e);
    messages.value.push({
      method: "GET",
      url: `${api}/auth/user`,
      status: e?.response?.status || 500,
      payload: e
    });
    // Se falhar ao buscar o user, lançamos o erro para impedir o redirect
    throw e;
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
                  class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
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
                  class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm"
              />
            </div>
          </div>

          <div class="flex items-center justify-between gap-4">
            <button
                type="button"
                @click="reset"
                class="w-full flex justify-center py-2 px-4 border border-gray-300 rounded-md shadow-sm text-sm font-medium text-gray-700 bg-white hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Reset
            </button>
            <button
                type="submit"
                class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
            >
              Login
            </button>
          </div>
        </form>

        <div class="mt-6 text-center">
          <NuxtLink to="/" class="text-sm font-medium text-indigo-600 hover:text-indigo-500">
            Voltar ao Início
          </NuxtLink>
        </div>
      </div>
    </div>
  </div>
</template>