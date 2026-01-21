<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { useTagsStore } from '~/stores/tags-store.js';

const router = useRouter();
const tagStore = useTagsStore();
const isLoading = ref(false);

const form = reactive({ name: "", description: "" });

async function handleSubmit() {
  if (!form.name) return alert("O nome é obrigatório");
  isLoading.value = true;

  try {
    await tagStore.createTag(form);
    router.push('/tags'); // Volta para a lista
  } catch (e) {
    alert("Erro ao criar tag.");
  } finally {
    isLoading.value = false;
  }
}
</script>

<template>
  <div class="max-w-2xl mx-auto space-y-6 mt-8">

    <div class="flex justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100">
      <div>
        <h1 class="text-2xl font-bold text-gray-800">Nova Tag</h1>
        <NuxtLink to="/tags" class="text-sm text-gray-500 hover:text-blue-600 transition">← Voltar à lista</NuxtLink>
      </div>
    </div>

    <div class="bg-white p-8 rounded-xl shadow-sm border border-gray-100">
      <form @submit.prevent="handleSubmit" class="space-y-6">

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">Nome da Tag</label>
          <div class="flex items-center">
            <span class="bg-gray-100 border border-r-0 border-gray-300 px-3 py-2 rounded-l-lg text-gray-500 font-bold">#</span>
            <input v-model="form.name" type="text" required placeholder="ex: java" class="w-full border border-gray-300 px-4 py-2 rounded-r-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition" />
          </div>
        </div>

        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">Descrição</label>
          <textarea v-model="form.description" rows="4" placeholder="Para que serve esta tag?" class="w-full border border-gray-300 px-4 py-2 rounded-lg focus:ring-2 focus:ring-blue-500 focus:outline-none transition"></textarea>
        </div>

        <div class="pt-4 flex justify-end gap-3">
          <NuxtLink to="/tags" class="px-6 py-2 border border-gray-300 text-gray-600 rounded-lg hover:bg-gray-50 font-medium transition">
            Cancelar
          </NuxtLink>
          <button type="submit" :disabled="isLoading" class="px-6 py-2 bg-blue-600 text-white rounded-lg font-bold hover:bg-blue-700 transition shadow-md flex items-center gap-2">
            <span>{{ isLoading ? 'A guardar...' : 'Criar Tag' }}</span>
          </button>
        </div>

      </form>
    </div>

  </div>
</template>