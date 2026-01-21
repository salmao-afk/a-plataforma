<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { useTagsStore } from '~/stores/tags-store.js';

const authStore = useAuthStore();
const tagStore = useTagsStore();

const searchQuery = ref("");
const isDeleteModalOpen = ref(false);
const tagToDelete = ref(null);
const toast = reactive({ show: false, message: '', type: 'success' });

// Permissões
const canManageTags = computed(() => {
  return authStore.user?.role === 'ADMINISTRATOR' || authStore.user?.role === 'RESPONSIBLE';
});

// Filtro
const filteredTags = computed(() => {
  if (!searchQuery.value) return tagStore.tags;
  const q = searchQuery.value.toLowerCase();
  return tagStore.tags.filter(t =>
      t.name.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q)
  );
});

// --- VERIFICAR SUBSCRIÇÃO ---
function isSubscribed(tagId) {
  if (!authStore.user || !authStore.user.subscribedTags) return false;
  // Verifica se o ID da tag existe na lista de tags subscritas do user
  return authStore.user.subscribedTags.some(t => t.id === tagId);
}

// Toast Helper
function showToast(message, type = 'success') {
  toast.message = message; toast.type = type; toast.show = true;
  setTimeout(() => { toast.show = false; }, 3000);
}

// Ações
function confirmDelete(tag) { tagToDelete.value = tag; isDeleteModalOpen.value = true; }

async function handleDelete() {
  if (!tagToDelete.value) return;
  try {
    await tagStore.deleteTag(tagToDelete.value.id);
    showToast("Tag eliminada.");
    isDeleteModalOpen.value = false;
  } catch (e) { showToast("Erro ao apagar.", "error"); }
}

async function handleSubscribe(tag) {
  try {
    await tagStore.subscribeTag(tag.id);

    // Atualizar localmente para o botão mudar logo
    if (!authStore.user.subscribedTags) authStore.user.subscribedTags = [];
    authStore.user.subscribedTags.push(tag);

    showToast(`Subscrito a #${tag.name}`);
  } catch(e) { showToast("Erro ao subscrever.", "error"); }
}

async function handleUnsubscribe(tag) {
  try {
    await tagStore.unsubscribeTag(tag.id);

    // Atualizar localmente
    if (authStore.user.subscribedTags) {
      authStore.user.subscribedTags = authStore.user.subscribedTags.filter(t => t.id !== tag.id);
    }

    showToast("Subscrição removida.");
  } catch(e) { showToast("Erro ao remover.", "error"); }
}

onMounted(() => { if (authStore.token) tagStore.fetchTags(); });
</script>

<template>
  <div class="max-w-6xl mx-auto space-y-6 relative">

    <div class="flex justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Gestão de Tags</h1>
        <NuxtLink to="/dashboard" class="text-sm text-gray-500 hover:text-blue-600 transition">← Voltar ao Dashboard</NuxtLink>
      </div>

      <NuxtLink v-if="canManageTags" to="/tags/criar" class="bg-blue-600 text-white px-6 py-2.5 rounded-lg font-bold hover:bg-blue-700 transition shadow-md flex items-center gap-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4" /></svg>
        Nova Tag
      </NuxtLink>
    </div>

    <div class="bg-white p-4 rounded-xl shadow-sm border border-gray-100">
      <input v-model="searchQuery" type="text" placeholder="Pesquisar tags..." class="w-full px-4 py-2 border rounded-lg bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-blue-500 transition" />
    </div>

    <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      <div v-for="tag in filteredTags" :key="tag.id" class="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:shadow-md transition flex flex-col justify-between h-full">
        <div>
          <div class="flex items-center gap-2 mb-2">
            <NuxtLink :to="`/tags/${tag.id}`" class="text-blue-600 font-bold text-lg hover:underline">
              #{{ tag.name }}
            </NuxtLink>
          </div>
          <p class="text-gray-600 text-sm mb-4">{{ tag.description || 'Sem descrição.' }}</p>
        </div>

        <div class="pt-4 border-t border-gray-100 flex justify-between items-center mt-auto">

          <div>
            <button
                v-if="isSubscribed(tag.id)"
                @click="handleUnsubscribe(tag)"
                class="group flex items-center gap-1 text-xs font-bold px-3 py-1.5 rounded-md transition bg-green-50 text-green-700 border border-green-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200"
                title="Clique para remover subscrição"
            >
              <span class="group-hover:hidden flex items-center gap-1">✅ Subscrito</span>
              <span class="hidden group-hover:flex items-center gap-1">❌ Remover</span>
            </button>

            <button
                v-else
                @click="handleSubscribe(tag)"
                class="flex items-center gap-1 text-xs font-bold px-3 py-1.5 rounded-md transition bg-blue-600 text-white hover:bg-blue-700 shadow-sm"
            >
              🔔 Subscrever
            </button>
          </div>

          <div v-if="canManageTags" class="flex gap-2">
            <NuxtLink :to="`/tags/editar/${tag.id}`" class="text-gray-400 hover:text-blue-600 p-1 rounded transition" title="Editar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
            </NuxtLink>
            <button @click="confirmDelete(tag)" class="text-gray-400 hover:text-red-600 p-1 rounded transition" title="Apagar">
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" /></svg>
            </button>
          </div>
        </div>
      </div>
    </div>

    <div v-if="toast.show" class="fixed bottom-6 right-6 z-50 animate-bounce-in">
      <div :class="toast.type === 'error' ? 'bg-red-500' : 'bg-green-600'" class="text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-3">
        <span class="text-xl">{{ toast.type === 'error' ? '⚠️' : '✅' }}</span>
        <p class="font-bold text-sm">{{ toast.message }}</p>
      </div>
    </div>

    <div v-if="isDeleteModalOpen" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
      <div class="bg-white rounded-xl shadow-xl p-6 text-center max-w-sm">
        <h3 class="text-lg font-bold text-gray-900 mb-2">Apagar Tag?</h3>
        <p class="text-gray-500 mb-6 text-sm">Tem a certeza que deseja remover "<b>{{ tagToDelete?.name }}</b>"?</p>
        <div class="flex justify-center gap-3"><button @click="isDeleteModalOpen=false" class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200">Cancelar</button><button @click="handleDelete" class="px-4 py-2 bg-red-600 text-white rounded-lg font-bold shadow hover:bg-red-700">Sim, Apagar</button></div>
      </div>
    </div>

  </div>
</template>