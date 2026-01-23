<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';
// Importação dos Componentes
import CommentsSection from '~/components/comments/CommentsSection.vue';
import StarRatingInput from '~/components/rating/StarRatingInput.vue';
import PublicationStats from '~/components/rating/PublicationStats.vue';

const route = useRoute();
const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();

const pub = ref(null);
const id = route.params.id;
const isLoading = ref(true);
const myRating = ref(0);
const commentsCountFromChild = ref(0); // Para guardar contagem vinda do componente filho

// Verifica se pode editar (Admin ou Dono)
const canEdit = computed(() => {
  if (!pub.value || !authStore.user) return false;
  return authStore.user.role === 'ADMINISTRATOR' || authStore.user.username === pub.value.uploaderUsername;
});

// Formata a data (ex: 21/01/2025)
const formattedDate = computed(() => {
  if (!pub.value?.publicationDate) return '';
  return new Date(pub.value.publicationDate).toLocaleDateString('pt-PT');
});

// --- CÁLCULOS DE ESTATÍSTICA (FRONTEND) ---
const displayAverage = computed(() => {
  // Se o backend já enviar (futuro), usa. Senão, calcula.
  if (pub.value?.averageRating) return pub.value.averageRating;

  const ratings = pub.value?.ratings || [];
  if (ratings.length === 0) return 0;
  const sum = ratings.reduce((acc, r) => acc + r.value, 0);
  return sum / ratings.length;
});

const displayRatingsCount = computed(() => {
  if (pub.value?.ratingsCount) return pub.value.ratingsCount;
  return pub.value?.ratings ? pub.value.ratings.length : 0;
});

const displayCommentsCount = computed(() => {
  // Prioridade: Backend > Componente Filho > Lista no Pub
  if (pub.value?.commentsCount) return pub.value.commentsCount;
  if (commentsCountFromChild.value > 0) return commentsCountFromChild.value;
  return pub.value?.comments ? pub.value.comments.length : 0;
});


// Buscar Publicação
const fetchPublication = async () => {
  try {
    const data = await $fetch(`${api}/publicacoes/${id}`, {
      headers: authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {}
    });
    pub.value = data;

    // Tentar encontrar o meu voto se vier no objeto (solução temporária)
    if (authStore.user && data.ratings) {
      const myVote = data.ratings.find(r => r.userUsername === authStore.user.username);
      if(myVote) myRating.value = myVote.value;
    }
  } catch (e) {
    console.error(e);
    // alert("Publicação não encontrada."); // Removido para não ser intrusivo
  } finally {
    isLoading.value = false;
  }
};

// Submeter Voto
const submitRating = async (val) => {
  if(!authStore.token) return alert("Faça login para avaliar!");

  try {
    await $fetch(`${api}/publicacoes/${id}/rating`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authStore.token}` },
      body: { value: val }
    });
    myRating.value = val;
    // Recarrega para atualizar médias se o backend calcular
    await fetchPublication();
    alert("Avaliação registada!");
  } catch(e) {
    alert("Erro ao avaliar.");
  }
};

// Callback quando um comentário é adicionado
const handleCommentAdded = () => {
  // Incrementa contador visualmente
  commentsCountFromChild.value++;
  // Opcional: recarregar pub se o backend contar
  // fetchPublication();
};


// Função de Download
const downloadFile = async () => {
  if (!authStore.token) return alert("Precisa de fazer login para descarregar ficheiros.");

  try {
    const blob = await $fetch(`${api}/publicacoes/${id}/download`, {
      headers: { Authorization: `Bearer ${authStore.token}` },
      responseType: 'blob'
    });

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.setAttribute('download', pub.value.filename || `documento_${id}.pdf`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (e) {
    alert("Erro ao descarregar ficheiro.");
  }
};

onMounted(() => {
  fetchPublication();
});
</script>

<template>
  <div class="max-w-4xl mx-auto mt-8 mb-20 px-4">

    <div v-if="isLoading" class="text-center py-20 text-gray-500">A carregar publicação...</div>

    <div v-else-if="pub">
      <NuxtLink to="/publicacoes" class="inline-flex items-center text-gray-500 hover:text-blue-600 mb-6 transition">
        <svg class="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
        Voltar à lista
      </NuxtLink>

      <div class="bg-white p-8 rounded-xl shadow-sm border border-gray-200">

        <div class="flex justify-between items-center mb-4">
          <span class="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-xs font-bold uppercase tracking-wide">
            {{ pub.type }}
          </span>
          <span class="text-gray-400 text-sm flex items-center gap-1">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 7V3m8 4V3m-9 8h10M5 21h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v12a2 2 0 002 2z" /></svg>
            {{ formattedDate }}
          </span>
        </div>

        <h1 class="text-3xl md:text-4xl font-bold text-gray-900 leading-tight">{{ pub.title }}</h1>
        <p class="text-lg text-blue-600 font-medium mt-2">{{ pub.scientificArea }}</p>

        <div class="mt-4 pt-4 border-t border-gray-100">
          <PublicationStats
              :average="displayAverage"
              :ratingsCount="displayRatingsCount"
              :commentsCount="displayCommentsCount"
          />
        </div>

        <div class="flex items-center gap-2 mt-6 pb-6 border-b border-gray-100">
          <div class="w-10 h-10 rounded-full bg-gray-200 flex items-center justify-center font-bold text-gray-600">
            {{ pub.uploaderUsername?.charAt(0).toUpperCase() }}
          </div>
          <div class="text-sm">
            <p class="text-gray-900 font-semibold">Publicado por @{{ pub.uploaderUsername }}</p>
            <p class="text-gray-500">Autores: {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : pub.authors }}</p>
          </div>
        </div>

        <div class="mt-8">
          <h3 class="font-bold text-gray-800 text-lg mb-3">Resumo</h3>
          <p class="text-gray-700 leading-relaxed whitespace-pre-wrap">{{ pub.description }}</p>
        </div>

        <div class="mt-8" v-if="pub.tags && pub.tags.length > 0">
          <h3 class="font-bold text-gray-800 text-sm uppercase tracking-wide mb-3">Tags Relacionadas</h3>
          <div class="flex flex-wrap gap-2">
            <NuxtLink
                v-for="tag in pub.tags"
                :key="typeof tag === 'object' ? tag.id : tag"
                :to="`/tags/${typeof tag === 'object' ? tag.id : ''}`"
                class="bg-gray-100 hover:bg-gray-200 text-gray-600 px-3 py-1.5 rounded-lg text-sm transition"
            >
              #{{ typeof tag === 'string' ? tag : tag.name }}
            </NuxtLink>
          </div>
        </div>

        <div class="mt-10 pt-6 border-t border-gray-100 flex flex-wrap justify-end gap-3">
          <NuxtLink
              v-if="canEdit"
              :to="`/publicacoes/editar/${pub.id}`"
              class="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg font-medium hover:bg-gray-50 transition flex items-center gap-2"
          >
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z" /></svg>
            Editar
          </NuxtLink>

          <button
              @click="downloadFile"
              class="px-5 py-2.5 bg-blue-600 text-white rounded-lg font-bold hover:bg-blue-700 transition flex items-center gap-2 shadow-md hover:shadow-lg"
          >
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4-4m0 0l-4 4m4-4v12" /></svg>
            Download PDF
          </button>
        </div>
      </div>

      <div class="my-8 p-6 bg-gradient-to-r from-gray-50 to-white border border-gray-100 rounded-xl flex flex-col sm:flex-row items-center justify-between gap-4 shadow-sm">
        <div>
          <h3 class="font-bold text-gray-800 text-lg">Classifique este documento</h3>
          <p class="text-gray-500 text-sm">A sua opinião é importante para a comunidade.</p>
        </div>
        <div class="flex flex-col items-center sm:items-end">
          <StarRatingInput
              :modelValue="myRating"
              @rate="submitRating"
              :readonly="!authStore.token"
          />
          <span v-if="!authStore.token" class="text-xs text-red-400 mt-1 font-medium">Faça login para votar</span>
        </div>
      </div>

      <CommentsSection
          :publicationId="pub.id"
          @comment-added="handleCommentAdded"
      />

    </div>
  </div>
</template>