<script setup>
import { reactive, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { usePublicationsStore } from '~/stores/publications-store.js';

const route = useRoute();
const router = useRouter();
const pubStore = usePublicationsStore();
const id = route.params.id;

const form = reactive({ title: "", description: "", scientificArea: "", type: "", authors: "" });

onMounted(async () => {
  try {
    // Usa a store para buscar os dados
    const data = await pubStore.fetchPublicationById(id);
    form.title = data.title;
    form.description = data.description;
    form.scientificArea = data.scientificArea;
    form.type = data.type;
    form.authors = Array.isArray(data.authors) ? data.authors.join(', ') : data.authors;
  } catch (e) {
    alert("Erro ao carregar.");
    router.push('/publicacoes');
  }
});

async function submitUpdate() {
  try {
    const authorsList = form.authors.includes(',') ? form.authors.split(',').map(s=>s.trim()) : [form.authors];
    // Chama a ACTION da store
    await pubStore.updatePublication(id, { ...form, authors: authorsList });
    router.push('/publicacoes');
  } catch (e) {
    alert("Erro ao editar.");
  }
}
</script>

<template>
  <div class="max-w-2xl mx-auto bg-white p-8 rounded-xl shadow mt-8">
    <h1 class="text-2xl font-bold mb-6">Editar Publicação #{{ id }}</h1>

    <form @submit.prevent="submitUpdate" class="space-y-4">
      <div><label class="block font-bold text-sm">Título</label><input v-model="form.title" class="w-full border p-2 rounded" /></div>
      <div class="grid grid-cols-2 gap-4">
        <div><label class="block font-bold text-sm">Área</label><input v-model="form.scientificArea" required class="w-full border p-2 rounded" /></div>
        <div><label class="block font-bold text-sm">Tipo</label><select v-model="form.type" class="w-full border p-2 rounded bg-white"><option>Article</option><option>Thesis</option></select></div>
      </div>
      <div><label class="block font-bold text-sm">Descrição</label><textarea v-model="form.description" rows="4" class="w-full border p-2 rounded"></textarea></div>
      <div><label class="block font-bold text-sm">Autores</label><input v-model="form.authors" class="w-full border p-2 rounded" /></div>

      <div class="flex justify-end gap-2 pt-4">
        <NuxtLink to="/publicacoes" class="px-4 py-2 border rounded">Cancelar</NuxtLink>
        <button type="submit" :disabled="pubStore.isLoading" class="px-6 py-2 bg-blue-600 text-white rounded font-bold">Guardar Alterações</button>
      </div>
    </form>
  </div>
</template>