<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';
import CommentsSection from '~/components/comments/CommentsSection.vue';
import StarRating from '~/components/rating/StarRating.vue';
import PublicationStats from '~/components/rating/PublicationStats.vue';

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();

const pub = ref(null);
const id = route.params.id;
const isLoading = ref(true);

const liveAverage = ref(0);
const liveMyRating = ref(0);

const canEdit = computed(() => {
  if (!pub.value || !authStore.user) return false;
  return authStore.user.role === 'ADMINISTRATOR' || authStore.user.username === pub.value.uploaderUsername;
});

const formattedDate = computed(() => {
  if (!pub.value?.publicationDate) return '';
  return new Date(pub.value.publicationDate).toLocaleDateString('pt-PT');
});

const fetchPublication = async () => {
  if (!authStore.token) return router.push('/auth/login');
  isLoading.value = true;
  try {
    const data = await $fetch(`${api}/publicacoes/${id}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    pub.value = data;
    await fetchStats();
  } catch (e) {
    if (e.response?.status === 401) {
      authStore.logout();
      router.push('/auth/login');
    } else {
      router.push('/publicacoes');
    }
  } finally {
    isLoading.value = false;
  }
};

const fetchStats = async () => {
  if (!authStore.token) return;
  try {
    const avgRes = await $fetch(`${api}/publicacoes/${id}/rating-medio`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    liveAverage.value = avgRes.average || 0;

    const myRatings = await $fetch(`${api}/utilizadores/me/ratings`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    const found = myRatings.find(r => r.publicationId == id);
    liveMyRating.value = found ? found.value : 0;
  } catch (e) {}
};

const downloadFile = async () => {
  if (!authStore.token) return alert("Precisa de fazer login.");
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
  } catch (e) { alert("Erro ao descarregar."); }
};

onMounted(() => { fetchPublication(); });
</script>

<template>
  <div class="max-w-4xl mx-auto mt-8 mb-20 px-4 font-sans">
    <div v-if="isLoading" class="text-center py-20">
      <div class="text-indigo-600 font-bold animate-pulse">A carregar...</div>
    </div>

    <div v-else-if="pub">
      <NuxtLink to="/publicacoes" class="inline-flex items-center text-gray-500 hover:text-indigo-600 mb-6 text-sm">← Voltar</NuxtLink>

      <div class="bg-white p-8 rounded-xl shadow-sm border border-gray-200">
        <div class="flex justify-between items-start mb-4">
          <span class="bg-indigo-50 text-indigo-700 px-3 py-1 rounded-full text-xs font-bold uppercase">{{ pub.type }}</span>
          <span class="text-gray-400 text-sm bg-gray-50 px-2 py-1 rounded">📅 {{ formattedDate }}</span>
        </div>

        <h1 class="text-3xl md:text-4xl font-extrabold text-gray-900 mb-2">{{ pub.title }}</h1>
        <p class="text-lg text-indigo-600 font-medium">{{ pub.scientificArea }}</p>

        <div class="mt-4 pt-4 border-t border-gray-100">
          <PublicationStats :average="liveAverage" />
        </div>

        <div class="flex items-center gap-3 mt-6 pb-6 border-b border-gray-100">
          <div class="w-12 h-12 rounded-full bg-gray-200 flex items-center justify-center font-bold text-gray-600">{{ pub.uploaderUsername?.charAt(0).toUpperCase() }}</div>
          <div>
            <p class="text-gray-900 font-bold">@{{ pub.uploaderUsername }}</p>
            <p class="text-gray-500 text-sm">Autores: {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : pub.authors }}</p>
          </div>
        </div>

        <div class="mt-8 bg-gray-50 p-6 rounded-lg border border-gray-100">
          <h3 class="font-bold text-gray-800 text-sm uppercase mb-3">📄 Resumo</h3>
          <p class="text-gray-700 leading-relaxed whitespace-pre-wrap">{{ pub.description }}</p>
        </div>

        <div class="mt-6" v-if="pub.tags && pub.tags.length > 0">
          <div class="flex flex-wrap gap-2">
            <span v-for="tag in pub.tags" :key="tag.id || tag" class="bg-white border border-gray-200 text-gray-600 px-3 py-1 rounded-full text-sm">#{{ tag.name || tag }}</span>
          </div>
        </div>

        <div class="mt-10 pt-6 border-t border-gray-100 flex flex-wrap justify-end gap-3">
          <NuxtLink v-if="canEdit" :to="`/publicacoes/editar/${pub.id}`" class="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg font-bold hover:bg-gray-50 flex items-center gap-2">✏️ Editar</NuxtLink>
          <button @click="downloadFile" class="px-5 py-2.5 bg-indigo-600 text-white rounded-lg font-bold hover:bg-indigo-700 flex items-center gap-2 shadow-lg">⬇️ Download PDF</button>
        </div>
      </div>

      <div class="my-8">
        <StarRating :pubId="pub.id" />
      </div>

      <CommentsSection :publicationId="pub.id" />
    </div>
  </div>
</template>