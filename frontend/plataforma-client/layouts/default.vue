<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const isMenuOpen = ref(false);

const toggleMenu = () => {
  isMenuOpen.value = !isMenuOpen.value;
};

const isAuthenticated = computed(() => !!authStore.token);
const isAdmin = computed(() => authStore.user?.role === 'ADMINISTRATOR');

const handleLogout = () => {
  authStore.logout();
  isMenuOpen.value = false;
  router.push('/auth/login');
};
</script>

<template>
  <div class="flex flex-col min-h-screen font-sans text-gray-800 bg-gray-50">

    <header class="sticky top-0 z-50 w-full bg-slate-900 text-white shadow-lg">
      <div class="container mx-auto max-w-screen-xl px-4 py-3 flex justify-between items-center">

        <div class="flex items-center gap-2">
          <div class="bg-blue-600 p-1.5 rounded-lg">
            <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" />
            </svg>
          </div>
          <NuxtLink to="/" class="text-xl font-bold tracking-wide hover:text-blue-200 transition">
            A Plataforma
          </NuxtLink>
        </div>

        <nav class="hidden md:flex items-center space-x-6">
          <template v-if="isAuthenticated">
            <NuxtLink to="/dashboard" class="hover:text-blue-300 transition">Dashboard</NuxtLink>
          </template>

          <template v-if="isAdmin">
            <NuxtLink to="/users/create" class="flex items-center gap-1 text-emerald-400 hover:text-emerald-300 transition">
              <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" /></svg>
              Criar User
            </NuxtLink>
          </template>

          <div class="h-6 w-px bg-slate-700 mx-2"></div>

          <div v-if="isAuthenticated" class="flex items-center gap-4">
            <div class="text-right">
              <div class="text-sm font-semibold">{{ authStore.user?.name || 'User' }}</div>
              <div class="text-xs text-slate-400 uppercase">{{ authStore.user?.role }}</div>
            </div>
            <div class="h-8 w-8 rounded-full bg-blue-600 flex items-center justify-center font-bold">
              {{ authStore.user?.username?.charAt(0).toUpperCase() }}
            </div>
            <button @click="handleLogout" class="px-3 py-1 bg-red-600 rounded text-sm hover:bg-red-700 transition">
              Sair
            </button>
          </div>

          <div v-else>
            <NuxtLink to="/auth/login" class="px-4 py-2 bg-blue-600 rounded text-sm hover:bg-blue-500 transition font-bold">
              Entrar
            </NuxtLink>
          </div>
        </nav>
      </div>
    </header>

    <main class="flex-grow w-full">
      <div class="container mx-auto max-w-screen-xl px-4 py-8">
        <slot />
      </div>
    </main>

    <footer class="bg-slate-900 text-slate-400 text-sm py-6 border-t border-slate-800">
      <div class="container mx-auto max-w-screen-xl px-4 text-center md:text-left">
        <p>&copy; 2025 Plataforma DAE. Engenharia Informática.</p>
      </div>
    </footer>

  </div>
</template>