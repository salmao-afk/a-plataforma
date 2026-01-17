<script setup lang="ts">
import { reactive, ref } from "vue";
import { useAuthStore } from "~/stores/auth-store.js";

const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();
const router = useRouter();

// Dados do formulário
const userFormData = reactive({
  username: "",
  password: "",
  name: "",
  email: "",
  role: "COLLABORATOR" // Valor por defeito
});

const feedbackMessage = ref("");
const isError = ref(false);

async function createUser() {
  // Limpar mensagens anteriores
  feedbackMessage.value = "";
  isError.value = false;

  // Verificação de segurança local
  if (!authStore.token) {
    feedbackMessage.value = "Erro: Não tem sessão iniciada. Apenas Admins podem criar users.";
    isError.value = true;
    return;
  }

  try {
    const response = await $fetch(`${api}/utilizadores`, { // O endpoint do seu Java
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        // Enviar o Token é OBRIGATÓRIO porque o Java tem @RolesAllowed
        Authorization: `Bearer ${authStore.token}`
      },
      body: userFormData
    });

    // Sucesso
    feedbackMessage.value = `Utilizador "${userFormData.username}" criado com sucesso!`;
    isError.value = false;

    // Limpar formulário
    userFormData.username = "";
    userFormData.password = "";
    userFormData.name = "";
    userFormData.email = "";
    userFormData.role = "COLLABORATOR";

  } catch (e: any) {
    isError.value = true;
    // Tenta ler a mensagem de erro que vem do Java (ex: "Password is mandatory")
    if (e.response && e.response._data) {
      feedbackMessage.value = `Erro: ${e.response._data}`;
    } else {
      feedbackMessage.value = "Ocorreu um erro ao criar o utilizador (Verifique se é Admin).";
    }
  }
}
</script>

<template>
  <div v-if="!authStore.user || authStore.user.role !== 'ADMINISTRATOR'"
       class="min-h-screen flex items-center justify-center bg-gray-100 text-red-600 font-bold text-xl">
    ACESSO NEGADO: Apenas Administradores.
  </div>

  <div v-else class="min-h-screen bg-gray-100 flex flex-col justify-center py-12 sm:px-6 lg:px-8">

    <div class="sm:mx-auto sm:w-full sm:max-w-md">
      <h2 class="mt-6 text-center text-3xl font-extrabold text-gray-900">
        Registar Novo Utilizador
      </h2>
      <p class="mt-2 text-center text-sm text-gray-600">
        Apenas administradores podem realizar esta ação.
      </p>
    </div>

    <div class="mt-8 sm:mx-auto sm:w-full sm:max-w-md">
      <div class="bg-white py-8 px-4 shadow sm:rounded-lg sm:px-10">

        <form class="space-y-6" @submit.prevent="createUser">

          <div>
            <label for="name" class="block text-sm font-medium text-gray-700">Nome Completo</label>
            <div class="mt-1">
              <input id="name" v-model="userFormData.name" type="text" required
                     class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>

          <div>
            <label for="email" class="block text-sm font-medium text-gray-700">Email</label>
            <div class="mt-1">
              <input id="email" v-model="userFormData.email" type="email" required
                     class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>

          <div>
            <label for="username" class="block text-sm font-medium text-gray-700">Username</label>
            <div class="mt-1">
              <input id="username" v-model="userFormData.username" type="text" required
                     class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>

          <div>
            <label for="password" class="block text-sm font-medium text-gray-700">Password</label>
            <div class="mt-1">
              <input id="password" v-model="userFormData.password" type="password" required
                     class="appearance-none block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm placeholder-gray-400 focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm" />
            </div>
          </div>

          <div>
            <label for="role" class="block text-sm font-medium text-gray-700">Função (Role)</label>
            <div class="mt-1">
              <select id="role" v-model="userFormData.role"
                      class="block w-full px-3 py-2 border border-gray-300 bg-white rounded-md shadow-sm focus:outline-none focus:ring-indigo-500 focus:border-indigo-500 sm:text-sm">
                <option value="COLLABORATOR">Collaborator</option>
                <option value="RESPONSIBLE">Responsible</option>
                <option value="ADMINISTRATOR">Administrator</option>
              </select>
            </div>
          </div>

          <div>
            <button type="submit"
                    class="w-full flex justify-center py-2 px-4 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500">
              Criar Utilizador
            </button>
          </div>
        </form>

        <div v-if="feedbackMessage" class="mt-4 p-4 rounded-md text-sm font-medium text-center"
             :class="isError ? 'bg-red-50 text-red-800' : 'bg-green-50 text-green-800'">
          {{ feedbackMessage }}
        </div>

        <div class="mt-6 text-center">
          <NuxtLink to="/" class="text-sm font-medium text-indigo-600 hover:text-indigo-500">
            Voltar ao Início
          </NuxtLink>
        </div>

      </div>
    </div>
  </div>
</template>