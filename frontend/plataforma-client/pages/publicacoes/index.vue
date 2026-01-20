<script setup>
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();

const publications = ref([]);
const searchQuery = ref("");
const isDeleteModalOpen = ref(false);
const publicationToDelete = ref(null);

// Filtro
const filteredPublications = computed(() => {
  if (!searchQuery.value) return publications.value;
  const q = searchQuery.value.toLowerCase();
  return publications.value.filter(p =>
      p.title?.toLowerCase().includes(q) || p.scientificArea?.toLowerCase().includes(q)
  );
});

// Fetch Inicial
async function fetchPublications() {
  try {
    const data = await $fetch(`${api}/publicacoes`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    publications.value = data;
  } catch (e) { console.error(e); }
}

// Download Seguro
async function downloadPublication(id) {
  try {
    const response = await $fetch.raw(`${api}/publicacoes/${id}/download`, {
      headers: { Authorization: `Bearer ${authStore.token}` },
      responseType: 'blob'
    });
    const blob = response._data;
    const header = response.headers.get('content-disposition');
    let filename = `doc_${id}`;
    if (header) {
      const matches = header.match(/filename="?([^"]+)"?/);
      if (matches && matches[1]) filename = matches[1];
    }
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url; link.setAttribute('download', filename);
    document.body.appendChild(link); link.click();
    document.body.removeChild(link); window.URL.revokeObjectURL(url);
  } catch (e) { alert("Erro ao baixar."); }
}

// Delete
function requestDelete(pub) { publicationToDelete.value = pub; isDeleteModalOpen.value = true; }
async function confirmDelete() {
  if (!publicationToDelete.value) return;
  try {
    await $fetch(`${api}/publicacoes/${publicationToDelete.value.id}`, {
      method: 'DELETE', headers: { Authorization: `Bearer ${authStore.token}` }
    });
    publications.value = publications.value.filter(p => p.id !== publicationToDelete.value.id);
    isDeleteModalOpen.value = false;
  } catch (e) { alert("Erro ao apagar."); }
}

onMounted(() => { fetchPublications(); });
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6">

    <div class="flex justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Publicações</h1>
        <NuxtLink to="/dashboard" class="text-sm text-gray-500 hover:text-blue-600 transition">← Voltar ao Dashboard</NuxtLink>
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
        <input v-model="searchQuery" type="text" placeholder="Pesquisar por título ou área..." class="w-full pl-10 px-4 py-2 border border-gray-300 rounded-lg bg-gray-50 focus:bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none transition" />
      </div>
    </div>

    <div class="grid grid-cols-1 gap-4">
      <div v-for="pub in filteredPublications" :key="pub.id" class="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-md transition flex flex-col md:flex-row justify-between items-start gap-4">

        <div class="flex gap-4">
          <div class="shrink-0 hidden sm:block">
            <div class="h-12 w-12 bg-blue-50 text-blue-600 rounded-lg flex items-center justify-center">
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" /></svg>
            </div>
          </div>

          <div>
            <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-lg font-bold text-gray-900 hover:text-blue-600 transition">
              {{ pub.title }}
            </NuxtLink>
            <div class="flex flex-wrap gap-2 text-sm text-gray-500 mt-1">
              <span class="bg-gray-100 px-2 py-0.5 rounded border border-gray-200">{{ pub.scientificArea }}</span>
              <span class="bg-gray-100 px-2 py-0.5 rounded border border-gray-200">{{ pub.type }}</span>
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
</template>