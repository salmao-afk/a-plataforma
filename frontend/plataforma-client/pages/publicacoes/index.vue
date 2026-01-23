<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { usePublicationsStore } from '~/stores/publications-store.js';
import PublicationStats from '~/components/rating/PublicationStats.vue';

const config = useRuntimeConfig();
const authStore = useAuthStore();
const pubStore = usePublicationsStore();
const api = config.public.apiBase;

const searchQuery = ref("");
const isDeleteModalOpen = ref(false);
const publicationToDelete = ref(null);
const showMine = ref(false);

// Filtro
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

// Buscar Publicações + Média
const fetchPublications = async () => {
  if (!authStore.token) return;
  pubStore.isLoading = true;

  try {
    let pubs = [];
    // 1. Buscar Lista
    const endpoint = showMine.value ? `${api}/publicacoes/me` : `${api}/publicacoes`;
    pubs = await $fetch(endpoint, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });

    // 2. Buscar Média individualmente
    const pubsWithRatings = await Promise.all(pubs.map(async (pub) => {
      try {
        const res = await $fetch(`${api}/publicacoes/${pub.id}/rating-medio`, {
          headers: { Authorization: `Bearer ${authStore.token}` }
        });
        pub.averageRating = res.average || 0;
      } catch (e) {
        pub.averageRating = 0;
      }
      return pub;
    }));

    pubStore.publications = pubsWithRatings;
  } catch (error) {
    console.error(error);
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
    await fetchPublications();
    isDeleteModalOpen.value = false;
  } catch (e) { alert("Erro ao apagar."); }
};

watch(showMine, () => { fetchPublications(); });
onMounted(() => { if(authStore.token) fetchPublications(); });
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6 mb-20 font-sans">

    <div v-if="pubStore.isLoading" class="flex justify-center py-10">
      <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
    </div>

    <div v-if="!authStore.token" class="flex flex-col items-center justify-center py-20 bg-white rounded-xl shadow-sm border border-gray-100 text-center px-4">
      <h2 class="text-2xl font-bold text-gray-800 mb-2">Acesso Reservado</h2>
      <NuxtLink to="/auth/login" class="px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 transition">Entrar</NuxtLink>
    </div>

    <div v-else class="space-y-6">
      <div class="flex flex-col md:flex-row justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100 gap-4">
        <h1 class="text-2xl font-extrabold text-gray-900">Publicações</h1>
        <div class="flex items-center gap-4">
          <div class="flex items-center bg-indigo-50 px-4 py-2 rounded-lg border border-indigo-100 cursor-pointer">
            <input type="checkbox" id="filterMine" v-model="showMine" class="w-5 h-5 text-indigo-600 rounded cursor-pointer">
            <label for="filterMine" class="ml-2 text-sm font-bold text-indigo-800 cursor-pointer select-none">Minhas</label>
          </div>
          <NuxtLink to="/publicacoes/criar" class="bg-gray-900 text-white px-6 py-2.5 rounded-lg font-bold hover:bg-black transition flex items-center gap-2">
            <span>+</span> Nova
          </NuxtLink>
        </div>
      </div>

      <div class="relative group">
        <div class="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
          <svg class="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" /></svg>
        </div>
        <input v-model="searchQuery" type="text" placeholder="Pesquisar..." class="w-full pl-12 px-4 py-4 border border-gray-200 rounded-xl bg-white shadow-sm focus:ring-2 focus:ring-indigo-500 outline-none transition text-lg" />
      </div>

      <div v-if="filteredPublications.length === 0 && !pubStore.isLoading" class="text-center py-16 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
        <p class="text-gray-400 text-lg">Nenhuma publicação encontrada.</p>
      </div>

      <div v-else class="grid grid-cols-1 gap-4">
        <div v-for="pub in filteredPublications" :key="pub.id" class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 hover:shadow-md transition flex flex-col md:flex-row justify-between items-start gap-4">
          <div class="flex gap-5 w-full">
            <div class="shrink-0 hidden sm:block pt-1">
              <div class="h-14 w-14 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center shadow-sm">
                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253" /></svg>
              </div>
            </div>

            <div class="flex-grow">
              <div class="flex flex-wrap gap-2 mb-2">
                <span class="bg-blue-50 text-blue-700 px-2 py-0.5 rounded text-xs font-bold uppercase">{{ pub.scientificArea }}</span>
                <span class="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs font-bold uppercase">{{ pub.type }}</span>
              </div>
              <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-xl font-bold text-gray-900 hover:text-indigo-600 transition block mb-1">
                {{ pub.title }}
              </NuxtLink>
              <p class="text-sm text-gray-600 mb-3">
                <span class="font-semibold text-gray-800">Autores:</span> {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : (pub.authors || 'N/A') }}
              </p>

              <div class="pt-3 border-t border-gray-50 flex items-center justify-between">
                <PublicationStats :average="pub.averageRating || 0" />
                <div class="text-xs text-gray-400 font-medium">@{{ pub.uploaderUsername }}</div>
              </div>
            </div>
          </div>

          <div class="flex flex-col gap-2 shrink-0 border-l border-gray-50 pl-4 ml-2">
            <button @click="downloadPublication(pub.id)" class="text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 p-2 rounded-lg transition" title="Download">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4-4m0 0l-4 4m4-4v12" /></svg>
            </button>
            <template v-if="authStore.user?.role === 'ADMINISTRATOR' || authStore.user?.username === pub.uploaderUsername">
              <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="text-gray-400 hover:text-orange-500 hover:bg-orange-50 p-2 rounded-lg transition" title="Editar">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
              </NuxtLink>
              <button @click="requestDelete(pub)" class="text-gray-400 hover:text-red-500 hover:bg-red-50 p-2 rounded-lg transition" title="Apagar">
                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
              </button>
            </template>
          </div>
        </div>
      </div>

      <div v-if="isDeleteModalOpen" class="fixed inset-0 bg-gray-900/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
        <div class="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm">
          <h3 class="text-xl font-bold text-gray-900 mb-2">Apagar?</h3>
          <p class="text-gray-500 mb-8">Esta ação é irreversível.</p>
          <div class="flex flex-col gap-3">
            <button @click="confirmDelete" class="w-full py-3 bg-red-600 text-white rounded-xl font-bold">Sim, Apagar</button>
            <button @click="isDeleteModalOpen=false" class="w-full py-3 bg-white text-gray-700 border border-gray-200 rounded-xl">Cancelar</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>