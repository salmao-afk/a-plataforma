<script setup>
import { ref, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const config = useRuntimeConfig();
const authStore = useAuthStore();

// --- ESTADO ---
const activeTab = ref('dados'); // Abas: 'dados', 'editar', 'publicacoes', 'tags'
const isLoading = ref(true);

const userProfile = ref(null);    // Dados do utilizador
const myPublications = ref([]);   // Lista de publicações
const myTags = ref([]);           // Lista de tags

// Dados para o formulário de edição
const editForm = ref({
  name: '',
  email: '',
  password: ''
});

// --- FETCH DADOS ---
async function fetchProfileData() {
  if (!authStore.token) return;
  isLoading.value = true;

  try {
    // 1. Buscar Dados Pessoais (/utilizadores/me)
    const profileResponse = await $fetch(`${config.public.apiBase}/utilizadores/me`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });

    // TRATAMENTO ESPECÍFICO PARA O TEU BACKEND:
    // O UserService devolve "return Response.ok(List.of(dto))", logo vem um Array.
    // Acedemos ao índice [0].
    userProfile.value = Array.isArray(profileResponse) ? profileResponse[0] : profileResponse;

    // Preencher formulário de edição com os dados recebidos
    if (userProfile.value) {
      editForm.value.name = userProfile.value.name || '';
      editForm.value.email = userProfile.value.email || '';
    }

    // 2. Buscar Tags Subscritas (/utilizadores/me/tags-subscritas)
    const tagsResponse = await $fetch(`${config.public.apiBase}/utilizadores/me/tags-subscritas`, {
      headers: { Authorization: `Bearer ${authStore.token}` }
    });
    myTags.value = tagsResponse || [];

    // 3. Buscar Publicações (/publicacoes/me)
    // Nota: Este endpoint está no PublicationService
    try {
      const pubsResponse = await $fetch(`${config.public.apiBase}/publicacoes/me`, {
        headers: { Authorization: `Bearer ${authStore.token}` }
      });
      myPublications.value = pubsResponse || [];
    } catch (e) {
      console.warn("Aviso: Falha ao buscar publicações. Verifique se o endpoint existe.", e);
    }

  } catch (error) {
    console.error("Erro ao carregar perfil:", error);
    alert("Erro ao carregar dados. Por favor faça login novamente.");
  } finally {
    isLoading.value = false;
  }
}

// --- ATUALIZAR PERFIL ---
async function updateProfile() {
  try {
    // Cria o payload. A password só vai se tiver valor.
    const payload = {
      name: editForm.value.name,
      email: editForm.value.email,
      ...(editForm.value.password ? { password: editForm.value.password } : {})
    };

    const response = await $fetch(`${config.public.apiBase}/utilizadores/me`, {
      method: 'PUT',
      headers: { Authorization: `Bearer ${authStore.token}` },
      body: payload
    });

    alert("Perfil atualizado com sucesso!");

    // Atualiza os dados locais com a resposta do servidor
    // O Backend devolve o DTO atualizado
    userProfile.value = response;

    // Atualiza a store localmente para refletir logo no Header
    authStore.user.name = editForm.value.name;
    authStore.user.email = editForm.value.email;

    // Limpa a password do form
    editForm.value.password = '';

    // Volta para a aba de visualização
    activeTab.value = 'dados';

  } catch (error) {
    console.error(error);
    alert("Erro ao atualizar: " + (error.data?.message || "Verifique os dados."));
  }
}

onMounted(() => {
  fetchProfileData();
});
</script>

<template>
  <div class="max-w-6xl mx-auto py-8 px-4">

    <div v-if="isLoading" class="flex justify-center items-center py-20">
      <div class="text-blue-600 font-bold text-lg animate-pulse">A carregar o seu perfil...</div>
    </div>

    <div v-else class="flex flex-col md:flex-row gap-6">

      <aside class="w-full md:w-1/4">
        <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden sticky top-4">

          <div class="p-6 bg-gradient-to-b from-blue-50 to-white text-center border-b border-gray-100">
            <div class="w-20 h-20 bg-blue-600 text-white rounded-full mx-auto flex items-center justify-center text-3xl font-bold mb-3 shadow-md">
              {{ userProfile?.username?.charAt(0).toUpperCase() }}
            </div>
            <h2 class="font-bold text-gray-900 text-lg">@{{ userProfile?.username }}</h2>
            <span class="inline-block bg-blue-100 text-blue-700 text-xs px-2 py-1 rounded-full font-semibold mt-1 uppercase">
              {{ userProfile?.role }}
            </span>
          </div>

          <nav class="flex flex-col p-2 space-y-1">
            <button @click="activeTab = 'dados'" :class="['nav-btn', activeTab === 'dados' ? 'active' : '']">
              <span class="text-xl">👤</span> Dados Pessoais
            </button>

            <button @click="activeTab = 'editar'" :class="['nav-btn', activeTab === 'editar' ? 'active' : '']">
              <span class="text-xl">✏️</span> Editar Dados
            </button>

            <button @click="activeTab = 'publicacoes'" :class="['nav-btn', activeTab === 'publicacoes' ? 'active' : '']">
              <span class="text-xl">📄</span> Minhas Publicações
              <span class="ml-auto bg-gray-100 text-gray-600 text-xs py-0.5 px-2 rounded-full">{{ myPublications.length }}</span>
            </button>

            <button @click="activeTab = 'tags'" :class="['nav-btn', activeTab === 'tags' ? 'active' : '']">
              <span class="text-xl">🏷️</span> Tags Subscritas
              <span class="ml-auto bg-gray-100 text-gray-600 text-xs py-0.5 px-2 rounded-full">{{ myTags.length }}</span>
            </button>
          </nav>
        </div>
      </aside>


      <main class="w-full md:w-3/4 bg-white rounded-xl shadow-sm border border-gray-100 p-6 md:p-8 min-h-[500px]">

        <div v-if="activeTab === 'dados'" class="animate-fade-in space-y-6">
          <h2 class="text-2xl font-bold text-gray-800 border-b pb-4">Os Meus Dados</h2>

          <div class="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-12">
            <div>
              <label class="label-view">Nome Completo</label>
              <p class="value-view">{{ userProfile?.name || 'Não definido' }}</p>
            </div>
            <div>
              <label class="label-view">Email</label>
              <p class="value-view">{{ userProfile?.email || 'Não definido' }}</p>
            </div>
            <div>
              <label class="label-view">Username</label>
              <p class="value-view">{{ userProfile?.username }}</p>
            </div>
            <div>
              <label class="label-view">Função</label>
              <p class="value-view">{{ userProfile?.role }}</p>
            </div>
          </div>
        </div>


        <div v-if="activeTab === 'editar'" class="animate-fade-in">
          <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Editar Perfil</h2>

          <form @submit.prevent="updateProfile" class="max-w-md space-y-4">
            <div>
              <label class="label-form">Nome Completo</label>
              <input v-model="editForm.name" type="text" class="input-form" />
            </div>

            <div>
              <label class="label-form">Email</label>
              <input v-model="editForm.email" type="email" class="input-form" />
            </div>

            <div class="pt-2">
              <label class="label-form">Nova Password <span class="text-xs font-normal text-gray-400">(Deixe vazio para manter a atual)</span></label>
              <input v-model="editForm.password" type="password" placeholder="******" class="input-form" />
            </div>

            <div class="pt-4">
              <button type="submit" class="bg-blue-600 text-white px-6 py-2.5 rounded-lg font-bold hover:bg-blue-700 transition shadow-md w-full md:w-auto">
                Guardar Alterações
              </button>
            </div>
          </form>
        </div>


        <div v-if="activeTab === 'publicacoes'" class="animate-fade-in">
          <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Minhas Publicações</h2>

          <div v-if="myPublications.length === 0" class="text-center py-10 bg-gray-50 rounded-lg border border-dashed">
            <p class="text-gray-500 mb-4">Ainda não publicou nada.</p>
            <NuxtLink to="/publicacoes/criar" class="text-blue-600 font-bold hover:underline">
              Criar primeira publicação &rarr;
            </NuxtLink>
          </div>

          <div v-else class="space-y-4">
            <div v-for="pub in myPublications" :key="pub.id" class="group flex flex-col md:flex-row justify-between items-start md:items-center p-4 rounded-lg border hover:border-blue-300 hover:shadow-md transition bg-white">
              <div>
                <h3 class="font-bold text-lg text-gray-800 group-hover:text-blue-600 transition">{{ pub.title }}</h3>
                <p class="text-sm text-gray-500 mt-1">
                  {{ pub.scientificArea }} • {{ pub.type }} • {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : 'N/A' }}
                </p>
              </div>
              <div class="mt-3 md:mt-0 flex gap-3">
                <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-sm font-semibold text-blue-600 hover:bg-blue-50 px-3 py-1.5 rounded-lg transition">
                  Ver
                </NuxtLink>
                <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="text-sm font-semibold text-gray-500 hover:bg-gray-100 px-3 py-1.5 rounded-lg transition">
                  Editar
                </NuxtLink>
              </div>
            </div>
          </div>
        </div>


        <div v-if="activeTab === 'tags'" class="animate-fade-in">
          <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Tags que Sigo</h2>

          <div v-if="myTags.length === 0" class="text-center py-12 bg-gray-50 rounded-lg border border-dashed">
            <div class="text-4xl mb-3">🏷️</div>
            <p class="text-gray-500 mb-4">Não está a subscrever nenhuma tag.</p>
            <NuxtLink to="/tags" class="inline-block bg-blue-600 text-white px-6 py-2 rounded-lg font-bold hover:bg-blue-700 transition">
              Explorar Tags
            </NuxtLink>
          </div>

          <div v-else>
            <div class="flex flex-wrap gap-3">
              <NuxtLink
                  v-for="tag in myTags"
                  :key="tag.id"
                  :to="`/tags/${tag.id}`"
                  class="flex items-center gap-2 bg-blue-50 text-blue-700 px-4 py-2 rounded-full font-medium hover:bg-blue-100 hover:shadow-sm transition border border-blue-100"
              >
                #{{ tag.name }}
              </NuxtLink>
            </div>

            <div class="mt-10 p-6 bg-blue-50 rounded-xl border border-blue-100 flex justify-between items-center">
              <div>
                <h4 class="font-bold text-blue-900">Quer receber mais novidades?</h4>
                <p class="text-sm text-blue-700">Subscreva mais áreas de interesse na página de tags.</p>
              </div>
              <NuxtLink to="/tags" class="bg-white text-blue-600 border border-blue-200 px-4 py-2 rounded-lg font-bold hover:bg-white hover:shadow-sm transition text-sm">
                Ir para Tags &rarr;
              </NuxtLink>
            </div>
          </div>
        </div>

      </main>
    </div>
  </div>
</template>

<style scoped>
/* Estilos Utilitários para limpar o template */
.nav-btn {
  @apply w-full text-left px-4 py-3 rounded-lg text-sm font-medium transition flex items-center gap-3 text-gray-600 hover:bg-gray-50;
}
.nav-btn.active {
  @apply bg-blue-50 text-blue-700 border-l-4 border-blue-600 rounded-l-none;
}

.label-view {
  @apply block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1;
}
.value-view {
  @apply text-lg font-medium text-gray-800;
}

.label-form {
  @apply block text-sm font-medium text-gray-700 mb-1;
}
.input-form {
  @apply w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-blue-500 focus:border-blue-500 outline-none transition;
}

.animate-fade-in {
  animation: fadeIn 0.3s ease-in-out;
}
@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}
</style>