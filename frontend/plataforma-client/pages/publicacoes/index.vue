<script setup>
import { ref, computed, onMounted, watch } from 'vue'; // Adicionei 'watch'
import { useAuthStore } from '~/stores/auth-store.js';
import { usePublicationsStore } from '~/stores/publications-store.js';

const config = useRuntimeConfig(); // Para aceder ao URL da API
const authStore = useAuthStore();
const pubStore = usePublicationsStore();

const searchQuery = ref("");
const isDeleteModalOpen = ref(false);
const publicationToDelete = ref(null);

// --- NOVO: Variável para controlar o filtro ---
const showMine = ref(false);

// Usamos os dados diretamente da Store
const filteredPublications = computed(() => {
  if (!searchQuery.value) return pubStore.publications;
  const q = searchQuery.value.toLowerCase();

  return pubStore.publications.filter(p => {
    const authorsString = Array.isArray(p.authors) ? p.authors.join(' ') : (p.authors || '');
    return p.title?.toLowerCase().includes(q) ||
        p.scientificArea?.toLowerCase().includes(q) ||
        authorsString.toLowerCase().includes(q);
  });
});

// --- LÓGICA DE BUSCA ALTERADA ---
const fetchPublications = async () => {
  if (!authStore.token) return;

  pubStore.isLoading = true; // Ativa o loading manual se necessário

  try {
    if (showMine.value) {
      // CASO 1: Apenas as minhas (Chama endpoint /me)
      const data = await $fetch(`${config.public.apiBase}/publicacoes/me`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      });
      // Atualizamos a lista da store manualmente com os dados recebidos
      pubStore.publications = data;
    } else {
      // CASO 2: Todas (Usa a ação normal da store)
      await pubStore.fetchPublications();
    }
  } catch (error) {
    console.error("Erro ao buscar publicações", error);
  } finally {
    pubStore.isLoading = false;
  }
};

const downloadPublication = (id) => {
  pubStore.downloadPublication(id).catch(() => alert("Erro ao baixar."));
};

const requestDelete = (pub) => {
  publicationToDelete.value = pub;
  isDeleteModalOpen.value = true;
};

const confirmDelete = async () => {
  if (!publicationToDelete.value) return;
  try {
    await pubStore.deletePublication(publicationToDelete.value.id);
    // Se estivermos no filtro "minhas", convém recarregar para garantir
    if(showMine.value) fetchPublications();
    isDeleteModalOpen.value = false;
  } catch (e) {
    alert("Erro ao apagar.");
  }
};

// Observa mudanças na checkbox para recarregar automaticamente
watch(showMine, () => {
  fetchPublications();
});

onMounted(() => { fetchPublications(); });
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">
    <div v-if="pubStore.isLoading" class="text-center py-4 text-blue-600 font-semibold">A carregar publicações...</div>

    <div v-if="!authStore.token" class="flex flex-col items-center justify-center py-20 bg-white rounded-xl shadow-sm border border-gray-100 text-center px-4">
      <NuxtLink to="/auth/login" class="px-6 py-3 bg-blue-600 text-white rounded-lg font-bold hover:bg-blue-700 transition">Fazer Login</NuxtLink>
    </div>

    <div v-else class="space-y-6">

      <div class="flex flex-col md:flex-row justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100 gap-4">
        <div>
          <h1 class="text-2xl font-bold text-gray-800">Publicações</h1>
          <NuxtLink to="/dashboard" class="text-sm text-gray-500 hover:text-blue-600 transition">← Voltar ao Dashboard</NuxtLink>
        </div>

        <div class="flex items-center bg-gray-50 px-4 py-2 rounded-lg border border-gray-200">
          <input
              type="checkbox"
              id="filterMine"
              v-model="showMine"
              class="w-5 h-5 text-blue-600 rounded focus:ring-blue-500 cursor-pointer"
          >
          <label for="filterMine" class="ml-2 text-sm font-medium text-gray-700 cursor-pointer select-none">
            Ver apenas as minhas
          </label>
        </div>

        <NuxtLink to="/publicacoes/criar" class="bg-blue-600 text-white px-6 py-2.5 rounded-lg font-bold hover:bg-blue-700 transition shadow-md flex items-center gap-2">
          <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
          Nova Publicação
        </NuxtLink>
      </div>

      <div class="bg-white p-4 rounded-xl shadow-sm border border-gray-100">
        <div class="relative">
          <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
          </div>
          <input v-model="searchQuery" type="text" placeholder="Pesquisar por título, autores ou área..." class="w-full pl-10 px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none transition" />
        </div>
      </div>

      <div v-if="filteredPublications.length === 0" class="text-center py-10 bg-gray-50 rounded-xl border border-dashed border-gray-300">
        <p class="text-gray-500">Nenhuma publicação encontrada.</p>
      </div>

      <div v-else class="grid grid-cols-1 gap-4">
        <div v-for="pub in filteredPublications" :key="pub.id" class="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-md transition flex flex-col md:flex-row justify-between items-start gap-4">

          <div class="flex gap-4 w-full">
            <div class="shrink-0 hidden sm:block">
              <div class="h-12 w-12 bg-blue-50 text-blue-600 rounded-lg flex items-center justify-center">
                <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
              </div>
            </div>

            <div class="flex-grow">
              <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-lg font-bold text-gray-900 hover:text-blue-600 transition block">
                {{ pub.title }}
              </NuxtLink>

              <p class="text-sm text-gray-700 mt-1">
                <span class="font-semibold text-gray-900">Autores:</span>
                {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : (pub.authors || 'N/A') }}
              </p>

              <div class="flex flex-wrap items-center gap-2 text-xs text-gray-500 mt-2">
                <span class="bg-gray-100 px-2 py-0.5 rounded border border-gray-200">{{ pub.scientificArea }}</span>
                <span class="bg-gray-100 px-2 py-0.5 rounded border border-gray-200">{{ pub.type }}</span>
                <span class="text-gray-400 pl-1 border-l border-gray-300 ml-1">
                  Publicado por: <span class="font-medium text-gray-600">{{ pub.uploaderUsername }}</span>
                </span>
              </div>

              <p class="text-gray-600 mt-2 line-clamp-2 text-sm">{{ pub.description }}</p>
            </div>
          </div>

          <div class="flex gap-2 shrink-0">
            <button @click="downloadPublication(pub.id)" class="text-blue-600 hover:bg-blue-50 p-2 rounded-full transition border border-transparent hover:border-blue-100" title="Download">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4-4m0 0l-4 4m4-4v12" /></svg>
            </button>

            <template v-if="authStore.user?.role === 'ADMINISTRATOR' || authStore.user?.username === pub.uploaderUsername">
              <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="text-gray-500 hover:bg-gray-100 p-2 rounded-full transition border border-transparent hover:border-gray-200" title="Editar">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
              </NuxtLink>
              <button @click="requestDelete(pub)" class="text-red-500 hover:bg-red-50 p-2 rounded-full transition border border-transparent hover:border-red-100" title="Apagar">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              </button>
            </template>
          </div>
        </div>
      </div>

      <div v-if="isDeleteModalOpen" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
        <div class="bg-white rounded-xl shadow-xl p-6 text-center max-w-sm">
          <div class="h-16 w-16 bg-red-100 text-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
          </div>
          <h3 class="text-xl font-bold text-gray-900 mb-2">Apagar Publicação?</h3>
          <p class="text-gray-500 mb-6 text-sm">Tem a certeza que deseja remover "<b>{{ publicationToDelete?.title }}</b>"?</p>
          <div class="flex justify-center gap-3">
            <button @click="isDeleteModalOpen=false" class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">Cancelar</button>
            <button @click="confirmDelete" class="px-4 py-2 bg-red-600 text-white rounded-lg font-bold shadow hover:bg-red-700">Sim, Apagar</button>
          </div>
        </div>
      </div>
    </div>

  </div>
</template>