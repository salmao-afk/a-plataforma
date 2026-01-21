<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import CommentForm from './CommentForm.vue';
import CommentItem from './CommentItem.vue';

const props = defineProps({
  publicationId: { type: Number, required: true }
});

const config = useRuntimeConfig();
const authStore = useAuthStore();
const comments = ref([]);
const isLoading = ref(true);

// Fetch Inicial
const fetchComments = async () => {
  try {
    const data = await $fetch(`${config.public.apiBase}/publicacoes/${props.publicationId}/comentarios`, {
      headers: authStore.token ? { Authorization: `Bearer ${authStore.token}` } : {}
    });
    comments.value = data;
  } catch (error) {
    console.error("Erro ao carregar comentários:", error);
  } finally {
    isLoading.value = false;
  }
};

// Event Handlers
const handleCommentAdded = (newComment) => {
  // Adiciona ao topo da lista
  comments.value.unshift(newComment);
};

const handleDeleted = (commentId) => {
  comments.value = comments.value.filter(c => c.id !== commentId);
};

const handleUpdated = (updatedComment) => {
  const index = comments.value.findIndex(c => c.id === updatedComment.id);
  if (index !== -1) {
    comments.value[index] = updatedComment;
  }
};

onMounted(() => {
  fetchComments();
});
</script>

<template>
  <div class="mt-10 border-t pt-8">
    <h3 class="text-xl font-bold text-gray-800 mb-6 flex items-center gap-2">
      Comentários
      <span class="bg-gray-100 text-gray-600 text-xs px-2 py-1 rounded-full">{{ comments.length }}</span>
    </h3>

    <CommentForm :publicationId="publicationId" @comment-added="handleCommentAdded" />

    <div v-if="isLoading" class="text-center py-4 text-gray-500">A carregar...</div>

    <div v-else-if="comments.length === 0" class="text-center py-8 text-gray-400 bg-gray-50 rounded-xl border border-dashed">
      Seja o primeiro a comentar!
    </div>

    <div v-else class="space-y-4">
      <CommentItem
          v-for="comment in comments"
          :key="comment.id"
          :comment="comment"
          :publicationId="publicationId"
          @deleted="handleDeleted"
          @updated="handleUpdated"
      />
    </div>
  </div>
</template>