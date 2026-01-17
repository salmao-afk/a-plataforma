<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';

const authStore = useAuthStore();
const router = useRouter();

onMounted(() => {
  if (authStore.token) {
    // Se já tiver login, vai para o Dashboard
    router.push('/dashboard');
  } else {
    // Se não tiver login, vai para a página de Login
    router.push('/auth/login');
  }
});
</script>

<template>
  <div class="flex items-center justify-center h-full">
    <p class="text-gray-500">A redirecionar...</p>
  </div>
</template>