<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';

const route = useRoute(); // Para pegar o ID da URL
const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();

const pub = ref(null);
const id = route.params.id; // O ID que vem na URL (ex: /publicacoes/15)

onMounted(async () => {
  try {
    pub.value = await $fetch(`${api}/publicacoes/${id}`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
  } catch (e) { alert("Publicação não encontrada."); }
});
</script>

<template>
  <div class="max-w-4xl mx-auto mt-8" v-if="pub">
    <NuxtLink to="/publicacoes" class="text-gray-500 hover:underline mb-4 block">← Voltar à lista</NuxtLink>

    <div class="bg-white p-8 rounded-xl shadow border">
      <div class="flex justify-between">
        <span class="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-bold">{{ pub.type }}</span>
        <span class="text-gray-400 text-sm">{{ pub.uploadDate }}</span>
      </div>

      <h1 class="text-3xl font-bold text-gray-900 mt-4">{{ pub.title }}</h1>
      <p class="text-lg text-gray-600 mt-2">{{ pub.scientificArea }}</p>

      <div class="mt-6 border-t pt-6">
        <h3 class="font-bold text-gray-800">Resumo</h3>
        <p class="text-gray-700 mt-2 leading-relaxed">{{ pub.description }}</p>
      </div>

      <div class="mt-6">
        <h3 class="font-bold text-gray-800">Autores</h3>
        <p class="text-gray-700">{{ Array.isArray(pub.authors) ? pub.authors.join(', ') : pub.authors }}</p>
      </div>

      <div class="mt-6">
        <h3 class="font-bold text-gray-800">Tags</h3>
        <div class="flex gap-2 mt-2">
          <span v-for="tag in pub.tags" :key="tag" class="bg-gray-100 px-2 py-1 rounded text-sm text-gray-600">#{{ typeof tag === 'string' ? tag : tag.name }}</span>
        </div>
      </div>

      <div class="mt-8 pt-6 border-t flex justify-end">
        <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="px-4 py-2 border rounded mr-2 hover:bg-gray-50">Editar</NuxtLink>
        <button class="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Download PDF</button>
      </div>
    </div>
  </div>
</template>