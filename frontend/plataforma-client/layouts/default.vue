<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const isMenuOpen = ref(false); // Mantive a lógica caso uses no mobile

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
            <NuxtLink to="/publicacoes" class="hover:text-blue-300 transition">Publicações</NuxtLink>
          </template>

          <template v-if="isAdmin">
            <NuxtLink to="/users/create" class="flex items-center gap-1 text-emerald-400 hover:text-emerald-300 transition">
              <svg xmlns="http://www.w3.org/2000/svg" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
              </svg>
              Gestor Users
            </NuxtLink>
          </template>

          <div class="h-6 w-px bg-slate-700 mx-2"></div>

          <div v-if="isAuthenticated" class="flex items-center gap-4">

            <NuxtLink
                to="/perfil"
                class="flex items-center gap-3 group hover:bg-slate-800 p-2 rounded-lg transition duration-200"
                title="Ir para o meu Perfil"
            >
              <div class="text-right hidden lg:block">
                <div class="text-sm font-semibold group-hover:text-blue-300 transition">
                  {{ authStore.user?.name || authStore.user?.username }}
                </div>
                <div class="text-xs text-slate-400 uppercase tracking-wider">
                  {{ authStore.user?.role }}
                </div>
              </div>

              <div class="h-9 w-9 rounded-full bg-blue-600 flex items-center justify-center font-bold shadow-md group-hover:ring-2 group-hover:ring-blue-400 transition">
                {{ authStore.user?.username?.charAt(0).toUpperCase() }}
              </div>
            </NuxtLink>

            <div class="h-8 w-px bg-slate-700"></div>

            <button @click="handleLogout" class="flex items-center gap-2 px-3 py-2 text-red-400 hover:text-red-300 hover:bg-red-400/10 rounded transition" title="Sair">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M17 16l4-4m0 0l-4-4m4 4H7m6 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h4a3 3 0 013 3v1" /></svg>
              <span class="hidden xl:inline">Sair</span>
            </button>
          </div>

          <div v-else>
            <NuxtLink to="/auth/login" class="px-5 py-2 bg-blue-600 rounded-lg text-sm font-bold hover:bg-blue-500 transition shadow-lg shadow-blue-900/50">
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