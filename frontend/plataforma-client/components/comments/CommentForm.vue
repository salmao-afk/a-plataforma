<script setup>
import { ref } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const props = defineProps({
  publicationId: { type: Number, required: true }
});

const emit = defineEmits(['comment-added']);
const authStore = useAuthStore();
const config = useRuntimeConfig();

const content = ref('');
const isSubmitting = ref(false);
const errorMessage = ref(''); // Nova variável
const successMessage = ref(''); // Nova variável

const submitComment = async () => {
  // Limpa mensagens anteriores
  errorMessage.value = '';
  successMessage.value = '';

  if (!content.value.trim()) return;

  isSubmitting.value = true;
  try {
    const response = await $fetch(`${config.public.apiBase}/publicacoes/${props.publicationId}/comentarios`, {
      method: 'POST',
      headers: { Authorization: `Bearer ${authStore.token}` },
      body: { content: content.value }
    });

    emit('comment-added', response.comentario);

    // Sucesso visual
    content.value = '';
    successMessage.value = "Comentário publicado com sucesso!";

    // Remove a mensagem de sucesso após 3 segundos
    setTimeout(() => successMessage.value = '', 3000);

  } catch (error) {
    console.error("Erro ao comentar:", error);
    // Tenta pegar a mensagem do backend, senão usa uma genérica
    errorMessage.value = error.data?.message || "Ocorreu um erro ao enviar o comentário. Verifique a sua sessão.";
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="bg-gray-50 p-6 rounded-xl border border-gray-200 mb-8 shadow-sm">

    <div v-if="!authStore.token" class="text-center text-gray-500 py-4 bg-white rounded-lg border border-dashed border-gray-300">
      <p>
        <NuxtLink to="/auth/login" class="text-blue-600 font-bold hover:underline">Faça login</NuxtLink>
        para participar na discussão.
      </p>
    </div>

    <form v-else @submit.prevent="submitComment">
      <h3 class="text-sm font-bold text-gray-700 mb-2 uppercase tracking-wide">Deixe a sua opinião</h3>

      <textarea
          v-model="content"
          rows="3"
          class="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none resize-none text-sm bg-white transition"
          placeholder="Escreva aqui o seu comentário..."
          required
      ></textarea>

      <div v-if="errorMessage" class="mt-3 p-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded-lg flex items-center gap-2 animate-pulse">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
        {{ errorMessage }}
      </div>

      <div v-if="successMessage" class="mt-3 p-3 bg-green-50 border border-green-200 text-green-700 text-sm rounded-lg flex items-center gap-2">
        <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
        {{ successMessage }}
      </div>

      <div class="flex justify-end mt-3">
        <button
            type="submit"
            :disabled="isSubmitting || !content.trim()"
            class="bg-blue-600 text-white px-6 py-2 rounded-lg text-sm font-bold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition shadow-md flex items-center gap-2"
        >
          <span v-if="isSubmitting" class="animate-spin h-4 w-4 border-2 border-white border-t-transparent rounded-full"></span>
          {{ isSubmitting ? 'A enviar...' : 'Publicar' }}
        </button>
      </div>
    </form>
  </div>
</template>