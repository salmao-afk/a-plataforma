<script setup>
import { useAuthStore } from '~/stores/auth-store.js';

const authStore = useAuthStore();
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-8">

    <div class="flex flex-col md:flex-row items-center justify-between gap-6 bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">
          Olá, {{ authStore.user?.name || 'Utilizador' }}! 👋
        </h1>
        <p class="text-gray-500">Bem-vindo ao seu painel de controlo.</p>
      </div>
      <div class="h-12 w-12 bg-blue-600 rounded-full flex items-center justify-center text-white font-bold text-xl shadow-md">
        {{ authStore.user?.username?.charAt(0).toUpperCase() }}
      </div>
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">

      <div class="bg-white rounded-xl shadow-md overflow-hidden border border-gray-100 flex flex-col h-full hover:shadow-lg transition">
        <div class="bg-blue-600 p-6 flex items-center justify-center">
          <svg class="h-16 w-16 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" />
          </svg>
        </div>
        <div class="p-6 flex flex-col flex-grow text-center">
          <h2 class="text-xl font-bold text-gray-900 mb-2">Publicações Científicas</h2>
          <p class="text-gray-500 text-sm mb-6 flex-grow">
            Consulte todo o acervo de artigos e teses da plataforma.
          </p>
          <NuxtLink to="/publicacoes" class="w-full py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition">
            Aceder às Publicações
          </NuxtLink>
        </div>
      </div>

      <div v-if="authStore.user?.role === 'ADMINISTRATOR'" class="bg-white rounded-xl shadow-md overflow-hidden border border-gray-100 flex flex-col h-full hover:shadow-lg transition">
        <div class="bg-slate-800 p-6 flex items-center justify-center">
          <svg class="h-16 w-16 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z" />
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
          </svg>
        </div>
        <div class="p-6 flex flex-col flex-grow text-center">
          <h2 class="text-xl font-bold text-gray-900 mb-2">Área de Gestão</h2>
          <p class="text-gray-500 text-sm mb-6 flex-grow">
            Adicione novos utilizadores e gira as permissões do sistema.
          </p>
          <NuxtLink to="/users/create" class="w-full py-2 bg-emerald-600 text-white rounded-lg font-medium hover:bg-emerald-700 transition">
            + Criar Novo Utilizador
          </NuxtLink>
        </div>
      </div>

    </div>

  </div>
</template>