<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';
import { useTagsStore } from '~/stores/tags-store.js';

const route = useRoute();
const config = useRuntimeConfig();
const authStore = useAuthStore();
const tagStore = useTagsStore();

const tag = ref(null);
const relatedPubs = ref([]);
const isLoading = ref(true);

const id = route.params.id;

onMounted(async () => {
  if (!authStore.token) return;

  try {
    // 1. Buscar detalhes da Tag
    tag.value = await tagStore.fetchTagById(id);

    // 2. Buscar publicações que tenham esta Tag
    // Usamos o endpoint de publicações com o filtro ?tags=NomeDaTag
    if (tag.value) {
      const pubsData = await $fetch(`${config.public.apiBase}/publicacoes?tags=${tag.value.name}`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      });
      relatedPubs.value = pubsData;
    }
  } catch (e) {
    console.error(e);
  } finally {
    isLoading.value = false;
  }
});
</script>

<template>
  <div class="max-w-4xl mx-auto space-y-8 mt-8 pb-12">

    <NuxtLink to="/tags" class="text-gray-500 hover:text-blue-600 transition flex items-center gap-1">
      ← Voltar às Tags
    </NuxtLink>

    <div v-if="isLoading" class="text-center py-12 text-gray-500">
      A carregar...
    </div>

    <div v-else-if="tag" class="space-y-8">

      <div class="bg-white p-8 rounded-xl shadow-sm border border-gray-100 text-center">
        <div class="inline-block bg-blue-50 text-blue-600 px-4 py-2 rounded-full text-2xl font-bold mb-4">
          #{{ tag.name }}
        </div>
        <h1 class="text-3xl font-bold text-gray-900 mb-2">{{ tag.name }}</h1>
        <p class="text-gray-600 max-w-2xl mx-auto">{{ tag.description || 'Sem descrição disponível.' }}</p>
      </div>

      <div>
        <h2 class="text-xl font-bold text-gray-800 mb-4 flex items-center gap-2">
          <span>📚</span> Publicações relacionadas
          <span class="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full">{{ relatedPubs.length }}</span>
        </h2>

        <div v-if="relatedPubs.length === 0" class="bg-gray-50 border border-dashed border-gray-300 rounded-xl p-8 text-center text-gray-500">
          Nenhuma publicação encontrada com a tag <b>#{{ tag.name }}</b>.
        </div>

        <div v-else class="grid gap-4">
          <div v-for="pub in relatedPubs" :key="pub.id" class="bg-white p-6 rounded-xl shadow-sm border border-gray-200 hover:border-blue-300 transition group">
            <div class="flex justify-between items-start">
              <div>
                <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-lg font-bold text-gray-900 group-hover:text-blue-600 transition">
                  {{ pub.title }}
                </NuxtLink>
                <div class="text-sm text-gray-500 mt-1">
                  {{ pub.scientificArea }} • {{ pub.type }}
                </div>
              </div>
              <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-blue-600 hover:bg-blue-50 px-3 py-1 rounded text-sm font-medium transition">
                Ver
              </NuxtLink>
            </div>
          </div>
        </div>
      </div>

    </div>

    <div v-else class="text-center py-12">
      <h2 class="text-xl font-bold text-red-500">Tag não encontrada</h2>
      <NuxtLink to="/tags" class="text-blue-600 hover:underline mt-2 block">Voltar à lista</NuxtLink>
    </div>

  </div>
</template>