<script setup>
import { ref, computed } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const props = defineProps({
  comment: { type: Object, required: true },
  publicationId: { type: Number, required: true }
});

const emit = defineEmits(['deleted', 'updated']);
const authStore = useAuthStore();
const config = useRuntimeConfig();

// Estado do Modal
const isModalOpen = ref(false);
const hideReasonInput = ref('');
const isProcessing = ref(false);

// Verifica permissões
const isOwner = computed(() => authStore.user?.username === props.comment.authorUsername);
const isAdmin = computed(() => ['ADMINISTRATOR', 'RESPONSIBLE'].includes(authStore.user?.role));

// --- APAGAR ---
const deleteComment = async () => {
  if (!confirm("Tem a certeza que deseja apagar este comentário?")) return;
  try {
    await $fetch(`${config.public.apiBase}/publicacoes/${props.publicationId}/comentarios/${props.comment.id}`, {
      method: 'DELETE',
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    emit('deleted', props.comment.id);
  } catch (error) {
    alert("Erro: " + (error.data?.message || "Sem permissão"));
  }
};

// --- PREPARAR PARA OCULTAR/MOSTRAR ---
const handleVisibilityClick = () => {
  if (props.comment.hidden) {
    // Se já está oculto, vamos mostrar (não precisa de motivo)
    updateVisibility(false, null);
  } else {
    // Se está visível, abre o modal para pedir motivo
    hideReasonInput.value = '';
    isModalOpen.value = true;
  }
};

// --- AÇÃO REAL DE ALTERAR VISIBILIDADE ---
const updateVisibility = async (shouldHide, reason) => {
  isProcessing.value = true;
  try {
    const payload = {
      hidden: shouldHide,
      hideReason: reason
    };

    await $fetch(`${config.public.apiBase}/publicacoes/${props.publicationId}/comentarios/${props.comment.id}/visibilidade`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${authStore.token}` },
      body: payload
    });

    // Atualiza localmente e fecha modal
    emit('updated', { ...props.comment, hidden: payload.hidden, hideReason: payload.hideReason });
    isModalOpen.value = false;

  } catch (error) {
    alert("Erro ao alterar visibilidade.");
  } finally {
    isProcessing.value = false;
  }
};
</script>

<template>
  <div :class="['p-4 rounded-xl border mb-3 transition relative group', comment.hidden ? 'bg-red-50 border-red-200' : 'bg-white border-gray-100 hover:border-blue-200']">

    <div class="flex justify-between items-start">
      <div class="flex items-center gap-3">
        <div class="w-9 h-9 rounded-full bg-gradient-to-br from-gray-100 to-gray-300 flex items-center justify-center text-sm font-bold text-gray-600 shadow-sm">
          {{ comment.authorUsername?.charAt(0).toUpperCase() }}
        </div>
        <div>
          <div class="text-sm font-bold text-gray-900">
            {{ comment.authorName }}
            <span class="text-xs font-normal text-gray-500">(@{{ comment.authorUsername }})</span>
          </div>
          <div class="text-xs text-gray-400">{{ comment.date }}</div>
        </div>
      </div>

      <div class="flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity duration-200">
        <button
            v-if="isAdmin"
            @click="handleVisibilityClick"
            class="px-2 py-1 text-xs rounded border transition font-medium"
            :class="comment.hidden ? 'bg-green-100 text-green-700 border-green-200 hover:bg-green-200' : 'bg-yellow-50 text-yellow-700 border-yellow-200 hover:bg-yellow-100'"
        >
          {{ comment.hidden ? '👁️ Mostrar' : '🚫 Ocultar' }}
        </button>

        <button
            v-if="isOwner || isAdmin"
            @click="deleteComment"
            class="px-2 py-1 text-xs rounded bg-gray-50 text-gray-500 border border-gray-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition"
            title="Apagar"
        >
          🗑️ Apagar
        </button>
      </div>
    </div>

    <div class="mt-3 text-sm text-gray-700 whitespace-pre-wrap pl-12 leading-relaxed">
      <div v-if="comment.hidden" class="flex items-center gap-2 text-red-600 bg-red-100 px-3 py-2 rounded-lg text-xs font-semibold mb-2">
        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
        <span>Motivo da Ocultação: {{ comment.hideReason }}</span>
      </div>

      <span :class="{ 'opacity-40 blur-[1px] select-none': comment.hidden }">
        {{ comment.content }}
      </span>
    </div>

    <div v-if="isModalOpen" class="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div class="absolute inset-0 bg-black/60 backdrop-blur-sm transition-opacity" @click="isModalOpen = false"></div>

      <div class="bg-white rounded-xl shadow-2xl w-full max-w-md relative z-10 overflow-hidden animate-fade-in-up">

        <div class="p-6">
          <h3 class="text-lg font-bold text-gray-900 mb-2">Ocultar Comentário</h3>
          <p class="text-sm text-gray-500 mb-4">Por favor, indique o motivo pelo qual este comentário está a ser ocultado. Esta mensagem será visível para todos.</p>

          <textarea
              v-model="hideReasonInput"
              class="w-full border border-gray-300 rounded-lg p-3 focus:ring-2 focus:ring-blue-500 outline-none text-sm h-24 resize-none"
              placeholder="Ex: Linguagem imprópria, spam, ofensivo..."
              autofocus
          ></textarea>
        </div>

        <div class="bg-gray-50 p-4 flex justify-end gap-3 border-t border-gray-100">
          <button
              @click="isModalOpen = false"
              class="px-4 py-2 text-gray-600 text-sm font-medium hover:bg-gray-200 rounded-lg transition"
          >
            Cancelar
          </button>
          <button
              @click="updateVisibility(true, hideReasonInput)"
              :disabled="!hideReasonInput.trim() || isProcessing"
              class="px-4 py-2 bg-red-600 text-white text-sm font-bold rounded-lg hover:bg-red-700 disabled:opacity-50 disabled:cursor-not-allowed shadow transition"
          >
            {{ isProcessing ? 'A processar...' : 'Confirmar Ocultação' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in-up {
  animation: fadeInUp 0.2s ease-out forwards;
}

@keyframes fadeInUp {
  from { opacity: 0; transform: scale(0.95) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>