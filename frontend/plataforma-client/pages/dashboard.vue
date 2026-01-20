<script setup>
import { useAuthStore } from '~/stores/auth-store.js';
const authStore = useAuthStore();
</script>

<template>
  <div class="max-w-4xl mx-auto py-12 space-y-8">

    <div class="bg-white p-8 rounded-xl shadow-sm border border-gray-100 flex items-center justify-between">
      <div>
        <h1 class="text-3xl font-bold text-gray-800">Olá, {{ authStore.user?.name || 'Utilizador' }}! 👋</h1>
        <p class="text-gray-500 mt-2">Bem-vindo ao painel de gestão científica.</p>
      </div>
      <div class="h-16 w-16 bg-blue-600 rounded-full flex items-center justify-center text-white text-2xl font-bold">
        {{ authStore.user?.username?.charAt(0).toUpperCase() }}
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

      <NuxtLink to="/publicacoes" class="group block bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-md transition hover:border-blue-300">
        <div class="h-12 w-12 bg-blue-100 text-blue-600 rounded-lg flex items-center justify-center mb-4 group-hover:bg-blue-600 group-hover:text-white transition">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 20H5a2 2 0 01-2-2V6a2 2 0 012-2h10a2 2 0 012 2v1m2 13a2 2 0 01-2-2V7m2 13a2 2 0 002-2V9a2 2 0 00-2-2h-2m-4-3H9M7 16h6M7 8h6v4H7V8z" /></svg>
        </div>
        <h3 class="text-xl font-bold text-gray-800">Gerir Publicações</h3>
        <p class="text-gray-500 mt-2">Consultar acervo, fazer upload de novos documentos ou editar existentes.</p>
      </NuxtLink>

      <div class="bg-gray-50 p-6 rounded-xl border border-gray-200 opacity-75">
        <div class="h-12 w-12 bg-gray-200 text-gray-400 rounded-lg flex items-center justify-center mb-4">
          <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 012 2h2a2 2 0 012-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" /></svg>
        </div>
        <h3 class="text-xl font-bold text-gray-400">Estatísticas (Em breve)</h3>
        <p class="text-gray-400 mt-2">Módulo em desenvolvimento.</p>
      </div>

    </div>
  </div>
</template>