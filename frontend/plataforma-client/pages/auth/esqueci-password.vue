<script setup>
import { ref } from 'vue';
const config = useRuntimeConfig();
const email = ref('');
const isSubmitting = ref(false);
// Novo estado para controlar as mensagens bonitas
const feedback = ref({ type: '', message: '' });

const sendRecoveryEmail = async () => {
  if (!email.value) return;
  isSubmitting.value = true;
  feedback.value = { type: '', message: '' }; // Limpa mensagens anteriores

  try {
    await $fetch(`${config.public.apiBase}/auth/esqueci-password`, {
      method: 'POST',
      body: { email: email.value }
    });

    feedback.value = {
      type: 'success',
      message: 'Email enviado! Verifique a sua caixa de correio (ou terminal) para obter o código.'
    };
    email.value = '';
  } catch (error) {
    feedback.value = { type: 'error', message: 'Ocorreu um erro. Verifique se o email está correto.' };
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="max-w-md w-full bg-white p-8 rounded-2xl shadow-xl border border-gray-100">

      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-14 h-14 rounded-full bg-indigo-50 text-indigo-600 mb-4">
          <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 7a2 2 0 012 2m4 0a6 6 0 01-7.743 5.743L11 17H9v2H7v2H4a1 1 0 01-1-1v-2.586a1 1 0 01.293-.707l5.964-5.964A6 6 0 1121 9z" /></svg>
        </div>
        <h2 class="text-2xl font-bold text-gray-900">Recuperar Password</h2>
        <p class="text-gray-500 mt-2 text-sm">Insira o email associado à sua conta.</p>
      </div>

      <div v-if="feedback.message"
           :class="[
             'mb-6 p-4 rounded-xl flex items-start gap-3 text-sm animate-fade-in border',
             feedback.type === 'success' ? 'bg-green-50 text-green-800 border-green-200' : 'bg-red-50 text-red-800 border-red-200'
           ]">
        <span class="text-lg mt-0.5">{{ feedback.type === 'success' ? '✅' : '⚠️' }}</span>
        <p>{{ feedback.message }}</p>
      </div>

      <form @submit.prevent="sendRecoveryEmail" class="space-y-5">
        <div>
          <label class="block text-sm font-bold text-gray-700 mb-1">Email</label>
          <input v-model="email" type="email" placeholder="exemplo@ipleiria.pt" required class="w-full border border-gray-300 rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500 outline-none transition" />
        </div>

        <button type="submit" :disabled="isSubmitting" class="w-full bg-indigo-600 text-white py-3 rounded-xl font-bold hover:bg-indigo-700 transition shadow-lg disabled:opacity-70 flex justify-center items-center">
          <span v-if="isSubmitting" class="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></span>
          {{ isSubmitting ? 'A enviar...' : 'Enviar Código' }}
        </button>
      </form>

      <div class="mt-8 pt-6 border-t border-gray-100 text-center">
        <p class="text-gray-400 text-xs uppercase tracking-wider font-semibold mb-3">Passos seguintes</p>
        <NuxtLink to="/auth/redefinir-password" class="block w-full py-3 bg-gray-50 text-gray-700 rounded-xl font-bold hover:bg-gray-100 transition border border-gray-200">
          Já tenho o Código →
        </NuxtLink>
        <div class="mt-4">
          <NuxtLink to="/auth/login" class="text-sm font-semibold text-gray-500 hover:text-indigo-600 transition">Voltar ao Login</NuxtLink>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
</style>