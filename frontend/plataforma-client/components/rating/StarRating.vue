<script setup>
import { ref, onMounted, watch } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const props = defineProps({
  pubId: {
    type: [Number, String],
    required: true
  }
});

const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;

// Estado
const average = ref(0);
const myRating = ref(0);
const isLoading = ref(false);
const hoverValue = ref(0);
const showDeleteModal = ref(false); // <--- NOVO: Controla o modal

// --- 1. BUSCAR DADOS INICIAIS ---
async function fetchRatingData() {
  if (!authStore.token) return;

  try {
    const avgRes = await $fetch(`${api}/publicacoes/${props.pubId}/rating-medio`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    average.value = avgRes.average || 0;

    const myRatings = await $fetch(`${api}/utilizadores/me/ratings`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });

    const found = myRatings.find(r => r.publicationId == props.pubId);
    myRating.value = found ? found.value : 0;

  } catch (e) {
    console.error("Erro ao carregar ratings", e);
  }
}

// --- 2. ENVIAR AVALIAÇÃO ---
async function setRating(value) {
  if (!authStore.token) return alert("Tem de fazer login para avaliar.");

  isLoading.value = true;
  const previousRating = myRating.value;
  myRating.value = value;

  try {
    await $fetch(`${api}/publicacoes/${props.pubId}/rating`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authStore.token}` },
      body: { value: value }
    });

    await fetchRatingData();

  } catch (error) {
    myRating.value = previousRating;
    // alert("Erro ao avaliar."); // Opcional
  } finally {
    isLoading.value = false;
  }
}

// --- 3. ABRIR MODAL (Substitui o confirm nativo) ---
function openRemoveModal() {
  showDeleteModal.value = true;
}

// --- 4. CONFIRMAR REMOÇÃO (Ação Real) ---
async function confirmRemove() {
  showDeleteModal.value = false;
  isLoading.value = true;

  try {
    await $fetch(`${api}/publicacoes/${props.pubId}/rating`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authStore.token}` }
    });

    myRating.value = 0;
    await fetchRatingData();

  } catch (e) {
    alert("Erro ao remover avaliação.");
  } finally {
    isLoading.value = false;
  }
}

watch(() => authStore.token, (newToken) => {
  if (newToken) fetchRatingData();
  else {
    average.value = 0;
    myRating.value = 0;
  }
});

onMounted(() => {
  fetchRatingData();
});
</script>

<template>
  <div class="bg-white p-6 rounded-xl border border-gray-100 shadow-sm flex flex-col items-center gap-4">

    <div class="text-center">
      <h3 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-1">Classificação Média</h3>
      <div class="flex items-center justify-center gap-2">
        <span class="text-3xl font-bold text-gray-800">{{ average.toFixed(1) }}</span>
        <div class="flex text-yellow-400">
          <svg v-for="i in 5" :key="`avg-${i}`" class="w-5 h-5" :class="i <= Math.round(average) ? 'fill-current' : 'text-gray-200 fill-current'" viewBox="0 0 24 24"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
        </div>
      </div>
    </div>

    <div class="w-full h-px bg-gray-100"></div>

    <div class="text-center">
      <h3 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">
        {{ myRating > 0 ? 'A tua classificação' : 'Avaliar Publicação' }}
      </h3>

      <div class="flex justify-center gap-1" @mouseleave="hoverValue = 0">
        <button
            v-for="star in 5"
            :key="star"
            @click="setRating(star)"
            @mouseover="hoverValue = star"
            :disabled="isLoading || !authStore.token"
            class="transition transform hover:scale-110 focus:outline-none disabled:opacity-50 disabled:cursor-not-allowed"
        >
          <svg
              class="w-8 h-8 transition-colors duration-200"
              :class="(hoverValue || myRating) >= star ? 'text-yellow-400 fill-current' : 'text-gray-200 fill-current'"
              viewBox="0 0 24 24"
          >
            <path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/>
          </svg>
        </button>
      </div>

      <button
          v-if="myRating > 0"
          @click="openRemoveModal"
          class="text-xs text-red-400 hover:text-red-600 mt-2 underline transition font-medium"
      >
        Remover classificação
      </button>

      <p v-if="!authStore.token" class="text-xs text-gray-400 mt-2">Faça login para votar</p>
    </div>

    <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center p-4 bg-gray-900/60 backdrop-blur-sm animate-fade-in">
      <div class="bg-white rounded-2xl shadow-2xl p-6 w-full max-w-sm text-center border border-gray-100 transform transition-all scale-100">

        <div class="mx-auto flex items-center justify-center h-14 w-14 rounded-full bg-red-100 mb-4">
          <svg class="h-8 w-8 text-red-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
          </svg>
        </div>

        <h3 class="text-lg font-bold text-gray-900">Remover Classificação?</h3>
        <p class="text-sm text-gray-500 mt-2 mb-6">
          Tens a certeza que queres remover a tua avaliação nesta publicação?
        </p>

        <div class="flex flex-col gap-3">
          <button
              @click="confirmRemove"
              class="w-full inline-flex justify-center rounded-xl border border-transparent shadow-sm px-4 py-3 bg-red-600 text-base font-bold text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:text-sm transition"
          >
            Sim, Remover
          </button>
          <button
              @click="showDeleteModal = false"
              class="w-full inline-flex justify-center rounded-xl border border-gray-300 shadow-sm px-4 py-3 bg-white text-base font-bold text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:text-sm transition"
          >
            Cancelar
          </button>
        </div>
      </div>
    </div>

  </div>
</template>

<style scoped>
.animate-fade-in {
  animation: fadeIn 0.2s ease-out;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>