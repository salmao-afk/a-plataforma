<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';

const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;

// --- ESTADO ---
const activeTab = ref('dados'); // Abas: 'dados', 'editar', 'publicacoes', 'tags'
const isLoading = ref(false);

const userProfile = ref({});
const myPublications = ref([]);
const myTags = ref([]);

// Formulário de Dados Pessoais
const editForm = ref({
    name: '',
    email: ''
});

// Formulário de Password (NOVO)
const passwordForm = reactive({
    oldPassword: '',
    newPassword: '',
    confirmPassword: ''
});

// --- 1. BUSCAR DADOS DO PERFIL (CORRIGIDO) ---
async function fetchProfileData() {
    if (!authStore.token) return;

    isLoading.value = true;
    console.log(">>> A carregar perfil...");

    try {
        // Buscar User
        const response = await $fetch(`${api}/utilizadores/me`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        // O Backend envia uma lista [DTO]. Vamos extrair.
        let dto = Array.isArray(response) ? response[0] : response;

        // Se o DTO tiver o objeto 'user' lá dentro
        if (dto && dto.user) {
            userProfile.value = dto.user;

            // Preencher listas se vierem no DTO
            if (dto.publicacoes) myPublications.value = dto.publicacoes;
        } else {
            // Fallback
            userProfile.value = dto;
        }

        // Preencher o formulário de edição com os dados atuais
        if (userProfile.value) {
            editForm.value.name = userProfile.value.name || '';
            editForm.value.email = userProfile.value.email || '';
        }

        // Buscar Tags (separado, caso não venha no DTO)
        try {
            const tags = await $fetch(`${api}/utilizadores/me/tags-subscritas`, {
                headers: { Authorization: `Bearer ${authStore.token}` }
            });
            myTags.value = tags || [];
        } catch (e) { console.warn("Tags erro:", e); }

    } catch (error) {
        console.error("Erro perfil:", error);
        alert("Erro ao carregar dados. Tente fazer login novamente.");
    } finally {
        isLoading.value = false; // Garante que o loading pára
    }
}

// --- 2. ATUALIZAR NOME E EMAIL ---
async function updateProfile() {
    try {
        // O Backend espera um UserProfileDTO que contém um UserDTO
        const payload = {
            user: {
                name: editForm.value.name,
                email: editForm.value.email
            }
        };

        const response = await $fetch(`${api}/utilizadores/me`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: payload
        });

        alert("Dados atualizados com sucesso!");

        // Atualiza visualmente
        userProfile.value.name = editForm.value.name;
        userProfile.value.email = editForm.value.email;

        // Atualiza a store (Header)
        authStore.user.name = editForm.value.name;
        authStore.user.email = editForm.value.email;

    } catch (error) {
        console.error(error);
        alert("Erro ao atualizar perfil.");
    }
}

// --- 3. ALTERAR PASSWORD (NOVO) ---
async function changeUserPassword() {
    // Validação frontend
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
        alert("A nova password e a confirmação não coincidem.");
        return;
    }

    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
        alert("Preencha todos os campos da password.");
        return;
    }

    try {
        // Chama o endpoint específico de password
        await $fetch(`${api}/auth/alterar-password`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: {
                oldPassword: passwordForm.oldPassword,
                newPassword: passwordForm.newPassword,
                confirmPassword: passwordForm.confirmPassword
            }
        });

        alert("Password alterada com sucesso!");

        // Limpar campos
        passwordForm.oldPassword = '';
        passwordForm.newPassword = '';
        passwordForm.confirmPassword = '';

    } catch (error) {
        console.error(error);
        alert("Erro ao alterar password: " + (error.response?._data?.message || error.message));
    }
}

onMounted(() => {
    // Pequeno delay para garantir que a store hidratou
    setTimeout(() => fetchProfileData(), 100);
});
</script>

<template>
    <div class="max-w-6xl mx-auto py-8 px-4 font-sans text-slate-800">

        <div v-if="isLoading" class="flex justify-center items-center py-20">
            <div class="text-indigo-600 font-bold text-lg animate-pulse">A carregar o seu perfil...</div>
        </div>

        <div v-else class="flex flex-col md:flex-row gap-6">

            <aside class="w-full md:w-1/4">
                <div class="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden sticky top-4">
                    <div class="p-6 bg-gradient-to-b from-indigo-50 to-white text-center border-b border-gray-100">
                        <div class="w-20 h-20 bg-indigo-600 text-white rounded-full mx-auto flex items-center justify-center text-3xl font-bold mb-3 shadow-md">
                            {{ userProfile?.username?.charAt(0).toUpperCase() || 'U' }}
                        </div>
                        <h2 class="font-bold text-gray-900 text-lg">@{{ userProfile?.username }}</h2>
                        <span class="inline-block bg-indigo-100 text-indigo-700 text-xs px-2 py-1 rounded-full font-semibold mt-1 uppercase">
              {{ userProfile?.role }}
            </span>
                    </div>

                    <nav class="flex flex-col p-2 space-y-1">
                        <button @click="activeTab = 'dados'" :class="['nav-btn', activeTab === 'dados' ? 'active' : '']">
                            👤 Dados Pessoais
                        </button>
                        <button @click="activeTab = 'editar'" :class="['nav-btn', activeTab === 'editar' ? 'active' : '']">
                            ✏️ Editar Perfil
                        </button>
                        <button @click="activeTab = 'publicacoes'" :class="['nav-btn', activeTab === 'publicacoes' ? 'active' : '']">
                            📄 Minhas Publicações
                        </button>
                        <button @click="activeTab = 'tags'" :class="['nav-btn', activeTab === 'tags' ? 'active' : '']">
                            🏷️ Tags Subscritas
                        </button>
                    </nav>
                </div>
            </aside>

            <main class="w-full md:w-3/4 bg-white rounded-xl shadow-sm border border-gray-100 p-6 md:p-8 min-h-[500px]">

                <div v-if="activeTab === 'dados'" class="animate-fade-in space-y-6">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4">Os Meus Dados</h2>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-y-6 gap-x-12">
                        <div><label class="label-view">Nome</label><p class="value-view">{{ userProfile?.name || '-' }}</p></div>
                        <div><label class="label-view">Email</label><p class="value-view">{{ userProfile?.email || '-' }}</p></div>
                        <div><label class="label-view">Username</label><p class="value-view">{{ userProfile?.username }}</p></div>
                        <div><label class="label-view">Função</label><p class="value-view">{{ userProfile?.role }}</p></div>
                    </div>
                </div>

                <div v-if="activeTab === 'editar'" class="animate-fade-in">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Editar Perfil</h2>

                    <form @submit.prevent="updateProfile" class="max-w-md space-y-4 mb-10">
                        <h3 class="text-lg font-semibold text-gray-700">Dados Gerais</h3>
                        <div>
                            <label class="label-form">Nome Completo</label>
                            <input v-model="editForm.name" type="text" class="input-form" required />
                        </div>
                        <div>
                            <label class="label-form">Email</label>
                            <input v-model="editForm.email" type="email" class="input-form" required />
                        </div>
                        <div class="pt-2">
                            <button type="submit" class="bg-indigo-600 text-white px-6 py-2 rounded-lg font-bold hover:bg-indigo-700 transition w-full md:w-auto">
                                Atualizar Dados
                            </button>
                        </div>
                    </form>

                    <hr class="border-gray-200 mb-8">

                    <div class="bg-gray-50 p-6 rounded-xl border border-gray-200 max-w-lg">
                        <h3 class="text-lg font-semibold text-gray-800 mb-4 flex items-center gap-2">
                            🔒 Alterar Palavra-Passe
                        </h3>

                        <form @submit.prevent="changeUserPassword" class="space-y-4">
                            <div>
                                <label class="label-form">Password Atual</label>
                                <input v-model="passwordForm.oldPassword" type="password" placeholder="Password atual" class="input-form bg-white" required />
                            </div>

                            <div class="grid grid-cols-1 sm:grid-cols-2 gap-4">
                                <div>
                                    <label class="label-form">Nova Password</label>
                                    <input v-model="passwordForm.newPassword" type="password" placeholder="Nova password" class="input-form bg-white" required />
                                </div>
                                <div>
                                    <label class="label-form">Confirmar</label>
                                    <input v-model="passwordForm.confirmPassword" type="password" placeholder="Repetir nova" class="input-form bg-white" required />
                                </div>
                            </div>

                            <div class="pt-2 text-right">
                                <button type="submit" class="bg-gray-800 text-white px-6 py-2 rounded-lg font-bold hover:bg-gray-900 transition w-full sm:w-auto">
                                    Mudar Password
                                </button>
                            </div>
                        </form>
                    </div>
                </div>

                <div v-if="activeTab === 'publicacoes'" class="animate-fade-in">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Minhas Publicações</h2>
                    <div v-if="!myPublications.length" class="text-center py-10 text-gray-500">Sem publicações.</div>
                    <div v-else class="space-y-3">
                        <div v-for="pub in myPublications" :key="pub.id" class="p-4 border rounded-lg hover:shadow-md transition">
                            <h3 class="font-bold text-lg">{{ pub.title }}</h3>
                            <p class="text-sm text-gray-500">{{ pub.scientificArea }} - {{ pub.type }}</p>
                            <div class="mt-2 flex gap-2">
                                <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-xs bg-indigo-50 text-indigo-700 px-2 py-1 rounded">Ver</NuxtLink>
                                <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded">Editar</NuxtLink>
                            </div>
                        </div>
                    </div>
                </div>

                <div v-if="activeTab === 'tags'" class="animate-fade-in">
                    <h2 class="text-2xl font-bold text-gray-800 border-b pb-4 mb-6">Tags Subscritas</h2>
                    <div v-if="!myTags.length" class="text-center py-10 text-gray-500">Sem subscrições.</div>
                    <div v-else class="flex flex-wrap gap-2">
                        <span v-for="tag in myTags" :key="tag.id" class="bg-blue-100 text-blue-800 px-3 py-1 rounded-full text-sm font-medium">#{{ tag.name }}</span>
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
.label-view { @apply block text-xs font-semibold text-gray-400 uppercase tracking-wider mb-1; }
.value-view { @apply text-lg font-medium text-gray-800; }
.label-form { @apply block text-sm font-medium text-gray-700 mb-1; }
.input-form { @apply w-full border border-gray-300 rounded-lg px-4 py-2 focus:ring-2 focus:ring-indigo-500 outline-none transition; }
.animate-fade-in { animation: fadeIn 0.3s ease-in-out; }
@keyframes fadeIn { from { opacity: 0; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }
</style>
