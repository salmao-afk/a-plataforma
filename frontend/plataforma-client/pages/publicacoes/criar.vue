<script setup>
import { reactive, ref } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { useRouter } from 'vue-router';

const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();
const router = useRouter();

const form = reactive({ title: "", description: "", scientificArea: "", type: "Article", authors: "", tags: "" });
const fileInput = ref(null);
const isLoading = ref(false);

function handleFile(e) { fileInput.value = e.target.files[0]; }

async function submitUpload() {
  if (!fileInput.value) return alert("Ficheiro obrigatório");
  isLoading.value = true;
  try {
    const fd = new FormData();
    Object.keys(form).forEach(k => fd.append(k, form[k]));
    fd.append("file", fileInput.value);

    await $fetch(`${api}/publicacoes`, {
      method: 'POST', headers: { Authorization: `Bearer ${authStore.token}` }, body: fd
    });

    // Sucesso: Redireciona para a lista
    router.push('/publicacoes');
  } catch (e) { alert("Erro ao criar."); }
  finally { isLoading.value = false; }
}
</script>

<template>
  <div class="max-w-2xl mx-auto bg-white p-8 rounded-xl shadow mt-8">
    <h1 class="text-2xl font-bold mb-6">Nova Publicação</h1>

    <form @submit.prevent="submitUpload" class="space-y-4">
      <div><label class="block font-bold text-sm">Título</label><input v-model="form.title" required class="w-full border p-2 rounded" /></div>

      <div class="grid grid-cols-2 gap-4">
        <div><label class="block font-bold text-sm">Área</label><input v-model="form.scientificArea" required class="w-full border p-2 rounded" /></div>
        <div><label class="block font-bold text-sm">Tipo</label><select v-model="form.type" class="w-full border p-2 rounded bg-white"><option>Article</option><option>Thesis</option></select></div>
      </div>

      <div><label class="block font-bold text-sm">Descrição</label><textarea v-model="form.description" rows="3" class="w-full border p-2 rounded"></textarea></div>
      <div><label class="block font-bold text-sm">Autores</label><input v-model="form.authors" class="w-full border p-2 rounded" /></div>
      <div><label class="block font-bold text-sm">Tags</label><input v-model="form.tags" class="w-full border p-2 rounded" /></div>

      <div class="border-2 border-dashed p-4 text-center rounded bg-gray-50">
        <input type="file" required @change="handleFile" class="w-full" />
      </div>

      <div class="flex justify-end gap-2 pt-4">
        <NuxtLink to="/publicacoes" class="px-4 py-2 text-gray-600 border rounded">Cancelar</NuxtLink>
        <button type="submit" :disabled="isLoading" class="px-6 py-2 bg-blue-600 text-white rounded font-bold">
          {{ isLoading ? 'A enviar...' : 'Criar Publicação' }}
        </button>
      </div>
    </form>
  </div>
</template>