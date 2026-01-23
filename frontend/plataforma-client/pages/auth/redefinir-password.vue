<script setup>
import { ref } from 'vue';
const router = useRouter();
const config = useRuntimeConfig();

const form = ref({ token: '', newPassword: '', confirmPassword: '' });
const feedback = ref({ type: '', message: '' }); // Estado para feedback

const resetPassword = async () => {
  feedback.value = { type: '', message: '' };

  if (form.value.newPassword !== form.value.confirmPassword) {
    feedback.value = { type: 'error', message: 'As passwords não coincidem.' };
    return;
  }

  try {
    await $fetch(`${config.public.apiBase}/auth/recuperar-password`, {
      method: 'POST',
      body: {
        token: form.value.token.trim(),
        newPassword: form.value.newPassword
      }
    });

    feedback.value = { type: 'success', message: 'Sucesso! A redirecionar para o login...' };

    // Pequeno delay para o utilizador ler a mensagem verde antes de ir embora
    setTimeout(() => router.push('/auth/login'), 2000);

  } catch (error) {
    feedback.value = { type: 'error', message: error.data || "Token inválido ou expirado." };
  }
};
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-gray-50 px-4">
    <div class="max-w-md w-full bg-white p-8 rounded-2xl shadow-xl border border-gray-100">
      <h2 class="text-2xl font-bold text-center text-gray-900 mb-6">Nova Password</h2>

      <div v-if="feedback.message"
           :class="[
             'mb-6 p-4 rounded-xl flex items-center gap-3 text-sm animate-fade-in border',
             feedback.type === 'success' ? 'bg-green-50 text-green-800 border-green-200' : 'bg-red-50 text-red-800 border-red-200'
           ]">
        <span class="text-xl">{{ feedback.type === 'success' ? '🎉' : '🚫' }}</span>
        <span class="font-medium">{{ feedback.message }}</span>
      </div>

      <form @submit.prevent="resetPassword" class="space-y-4">
        <div>
          <label class="text-xs font-bold text-gray-500 uppercase tracking-wide">Código de Recuperação</label>
          <input v-model="form.token" placeholder="Cole aqui o token" required class="w-full mt-1 border border-gray-300 rounded-xl px-4 py-3 text-center font-mono tracking-widest bg-gray-50 focus:bg-white focus:ring-2 focus:ring-indigo-500 outline-none transition" />
        </div>

        <div class="grid grid-cols-2 gap-3 pt-2">
          <div>
            <label class="text-xs font-bold text-gray-500 uppercase">Nova</label>
            <input v-model="form.newPassword" type="password" required class="w-full mt-1 border border-gray-300 rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500 outline-none transition" />
          </div>
          <div>
            <label class="text-xs font-bold text-gray-500 uppercase">Confirmar</label>
            <input v-model="form.confirmPassword" type="password" required class="w-full mt-1 border border-gray-300 rounded-xl px-4 py-3 focus:ring-2 focus:ring-indigo-500 outline-none transition" />
          </div>
        </div>

        <button type="submit" class="w-full bg-indigo-600 text-white py-3 rounded-xl font-bold hover:bg-indigo-700 transition shadow-lg mt-4">
          Alterar Password
        </button>
      </form>
    </div>
  </div>
</template>

<style scoped>
.animate-fade-in { animation: fadeIn 0.4s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
</style>