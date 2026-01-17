<script setup lang="ts">
import { reactive, ref } from "vue";
import { storeToRefs } from "pinia";
import { useAuthStore } from "~/stores/auth-store.js";

const config = useRuntimeConfig();
const api = config.public.apiBase;

const loginFormData = reactive({
  username: "",
  password: ""
});

const authStore = useAuthStore();
const { token, user } = storeToRefs(authStore);

const messages = ref([]);

function reset() {
  authStore.logout();
  messages.value = [];
}

async function login() {
  try {
    const response = await $fetch(`${api}/auth/login`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Accept: "application/json"
      },
      body: loginFormData
    });

    token.value = response.token; // Supondo que a API retorna { token: "..." }

    messages.value.push({
      method: "POST",
      url: `${api}/auth/login`,
      status: 200,
      payload: response
    });

    await getUserInfo();
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
      </div>
    </div>
    <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-2xl px-4">

      <div v-if="token" class="mb-4 bg-green-50 border border-green-200 rounded-md p-4">
        <h3 class="text-sm font-medium text-green-800">✅ Login com Sucesso! (Token Recebido)</h3>
        <div class="mt-2 text-xs text-green-700 break-all font-mono bg-white p-2 rounded border border-green-100">
          {{ token }}
        </div>
      </div>

      <div v-if="user" class="mb-4 bg-blue-50 border border-blue-200 rounded-md p-4">
        <h3 class="text-sm font-medium text-blue-800">👤 Dados do Utilizador</h3>
        <pre class="mt-2 text-xs text-blue-700 font-mono bg-white p-2 rounded border border-blue-100 overflow-auto">{{ user }}</pre>
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