<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { Shield, ArrowLeft, Edit, AlertCircle } from "lucide-vue-next";

definePageMeta({
    validate: async (route) => {
        const reservedWords = ['index', 'create', 'editar'];
        return !reservedWords.includes(route.params.id as string);
    }
});

const route = useRoute();
const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;

const userId = route.params.id as string;

// --- ESTADO ---
const activeTab = ref('dados');
const isLoading = ref(true);

const userProfile = ref(null);
const userPublications = ref([]);
// As tags agora vêm dentro do userProfile, mas podemos manter esta ref para facilitar a leitura no template
const userTags = ref([]);

// --- FETCH DADOS ---
async function fetchProfileData() {
    isLoading.value = true;
    try {
        // 1. Buscar Dados do Utilizador (Agora inclui Tags e Estado)
        const profileResponse = await $fetch(`${api}/utilizadores/${userId}`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        userProfile.value = profileResponse;

        // Mapear as tags diretamente do DTO (Sem pedido extra!)
        userTags.value = profileResponse.subscribedTags || [];

        // 2. Buscar Publicações (Este continua separado pois é uma lista pesada)
        try {
            const pubsResponse = await $fetch(`${api}/utilizadores/${userId}/publicacoes`, {
                headers: { Authorization: `Bearer ${authStore.token}` }
            });
            userPublications.value = pubsResponse || [];
        } catch (e) {
            userPublications.value = [];
        }

    } catch (error) {
        console.error("Erro ao carregar perfil:", error);
    } finally {
        isLoading.value = false;
    }
}

onMounted(() => {
    fetchProfileData();
});
</script>

<template>
    <div class="max-w-6xl mx-auto py-8 px-4 font-sans text-slate-800">

        <div v-if="isLoading" class="flex justify-center items-center py-20">
            <div class="text-indigo-600 font-bold text-lg animate-pulse">A carregar perfil de {{ userId }}...</div>
        </div>

        <div v-else-if="userProfile" class="flex flex-col md:flex-row gap-6">

            <aside class="w-full md:w-1/4">
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden sticky top-4">

                    <div class="p-6 bg-gradient-to-b from-blue-50 to-white text-center border-b border-gray-100">
                        <div class="w-20 h-20 bg-indigo-600 text-white rounded-full mx-auto flex items-center justify-center text-3xl font-bold mb-3 shadow-md">
                            {{ userProfile.name?.charAt(0).toUpperCase() }}
                        </div>
                        <h2 class="font-bold text-gray-900 text-lg truncate">{{ userProfile.name }}</h2>
                        <p class="text-sm text-gray-500 mb-2">@{{ userProfile.username }}</p>

                        <div class="flex flex-col gap-2 items-center">
                            <span class="inline-block bg-indigo-100 text-indigo-700 text-xs px-2 py-1 rounded-full font-semibold uppercase">
                                {{ userProfile.role }}
                            </span>

                            <div :class="`text-xs px-3 py-1 rounded-full font-bold border ${userProfile.active ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`">
                                {{ userProfile.active ? '● Ativo' : '● Suspenso' }}
                            </div>
                        </div>
                    </div>

                    <nav class="flex flex-col p-2 space-y-1">
                        <button @click="activeTab = 'dados'" :class="['nav-btn', activeTab === 'dados' ? 'active' : '']">
                            <span class="text-xl">👤</span> Dados do Utilizador
                        </button>

                        <button @click="activeTab = 'publicacoes'" :class="['nav-btn', activeTab === 'publicacoes' ? 'active' : '']">
                            <span class="text-xl">📄</span> Publicações
                            <span class="ml-auto bg-gray-100 text-gray-600 text-xs py-0.5 px-2 rounded-full">{{ userPublications.length }}</span>
                        </button>

                        <button @click="activeTab = 'tags'" :class="['nav-btn', activeTab === 'tags' ? 'active' : '']">
                            <span class="text-xl">🏷️</span> Tags Subscritas
                            <span class="ml-auto bg-gray-100 text-gray-600 text-xs py-0.5 px-2 rounded-full">{{ userTags.length }}</span>
                        </button>

                        <NuxtLink :to="`/utilizadores/editar/${userId}`" class="nav-btn hover:text-indigo-600">
                            <span class="text-xl"><Edit :size="20"/></span> Editar Perfil
                        </NuxtLink>

                        <NuxtLink to="/utilizadores" class="nav-btn mt-4 border-t pt-3 hover:text-red-600">
                            <span class="text-xl">⬅️</span> Voltar à Lista
                        </NuxtLink>
                    </nav>
                </div>
            </aside>

            <main class="w-full md:w-3/4 bg-white rounded-xl shadow-sm border border-gray-100 p-6 md:p-8 min-h-[500px]">

                <div v-if="activeTab === 'dados'" class="animate-fade-in space-y-6">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4">Dados de {{ userProfile.name }}</h2>

                    <div class="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-12">
                        <div>
                            <label class="label-view">Nome Completo</label>
                            <p class="value-view">{{ userProfile.name }}</p>
                        </div>
                        <div>
                            <label class="label-view">Email</label>
                            <p class="value-view">{{ userProfile.email }}</p>
                        </div>
                        <div>
                            <label class="label-view">Username</label>
                            <p class="value-view">{{ userProfile.username }}</p>
                        </div>
                        <div>
                            <label class="label-view">Função</label>
                            <p class="value-view flex items-center gap-2">
                                <Shield :size="16" class="text-indigo-600"/> {{ userProfile.role }}
                            </p>
                        </div>
                        <div>
                            <label class="label-view">Estado da Conta</label>
                            <p :class="`value-view ${userProfile.active ? 'text-green-600' : 'text-red-600'}`">
                                {{ userProfile.active ? 'Ativa' : 'Suspensa' }}
                            </p>
                            <div v-if="!userProfile.active && userProfile.statusReason" class="mt-2 bg-red-50 text-red-800 p-3 rounded-md text-sm border border-red-100 flex items-start gap-2">
                                <AlertCircle :size="16" class="mt-0.5 shrink-0"/>
                                <div>
                                    <strong>Motivo:</strong> {{ userProfile.statusReason }}
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="activeTab === 'publicacoes'" class="animate-fade-in">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Publicações de {{ userProfile.name }}</h2>

                    <div v-if="userPublications.length === 0" class="text-center py-10 bg-gray-50 rounded-lg border border-dashed">
                        <p class="text-gray-500">Este utilizador ainda não publicou nada.</p>
                    </div>

                    <div v-else class="space-y-4">
                        <div v-for="pub in userPublications" :key="pub.id" class="group flex flex-col justify-between items-start p-4 rounded-lg border hover:border-indigo-300 hover:shadow-md transition bg-white">
                            <div>
                                <h3 class="font-bold text-lg text-gray-800 group-hover:text-indigo-600 transition">{{ pub.title }}</h3>
                                <p class="text-sm text-gray-500 mt-1">
                                    {{ pub.scientificArea }} • {{ pub.type }}
                                </p>
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="activeTab === 'tags'" class="animate-fade-in">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Tags que {{ userProfile.name }} segue</h2>

                    <div v-if="userTags.length === 0" class="text-center py-12 bg-gray-50 rounded-lg border border-dashed">
                        <div class="text-4xl mb-3">🏷️</div>
                        <p class="text-gray-500">O utilizador não subscreveu nenhuma tag.</p>
                    </div>

                    <div v-else class="flex flex-wrap gap-3">
                        <span
                            v-for="tag in userTags"
                            :key="tag.id"
                            class="flex items-center gap-2 bg-blue-50 text-blue-700 px-4 py-2 rounded-full font-medium border border-blue-100 cursor-default"
                        >
                            #{{ tag.name }}
                        </span>
                    </div>
                </div>

            </main>
        </div>
    </div>
</template>

<style scoped>
.nav-btn {
    @apply w-full text-left px-4 py-3 rounded-lg text-sm font-medium transition flex items-center gap-3 text-gray-600 hover:bg-gray-50;
}
.nav-btn.active {
    @apply bg-indigo-50 text-indigo-700 border-l-4 border-indigo-600 rounded-l-none;
}
.label-view {
    @apply block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1;
}
.value-view {
    @apply text-lg font-medium text-gray-800;
}
.animate-fade-in {
    animation: fadeIn 0.3s ease-in-out;
}
@keyframes fadeIn {
    from { opacity: 0; transform: translateY(5px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
