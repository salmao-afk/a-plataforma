<script setup lang="ts">
import { reactive, ref, onMounted, computed } from "vue";
import { useAuthStore } from "~/stores/auth-store.js";
// Adicionei AlertTriangle e X para o novo modal
import {
    Search, Plus, Edit, Trash2, Shield,
    CheckCircle, XCircle, ChevronLeft, ChevronRight, X, AlertTriangle
} from "lucide-vue-next";

const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();

// --- ESTADO GERAL ---
const users = ref([]);
const isLoading = ref(false);

// --- PAGINAÇÃO & PESQUISA ---
const currentPage = ref(1);
const itemsPerPage = ref(10);
const searchQuery = ref("");

// --- MODAL DE CRIAR/EDITAR ---
const showModal = ref(false);
const isEditing = ref(false);
const editingUserId = ref(null);

// --- MODAL DE SUSPENSÃO (NOVO) ---
const showSuspendModal = ref(false);
const userToSuspend = ref(null);
const suspendMotive = ref("");

// --- DADOS DOS FORMULÁRIOS ---
const userFormData = reactive({
    username: "",
    password: "",
    name: "",
    email: "",
    role: "COLLABORATOR"
});

const feedbackMessage = ref("");
const isError = ref(false);

// --- COMPUTED PROPERTIES ---
const filteredUsers = computed(() => {
    if (!searchQuery.value) return users.value;
    const query = searchQuery.value.toLowerCase();
    return users.value.filter(user =>
        user.username?.toLowerCase().includes(query) ||
        user.name?.toLowerCase().includes(query) ||
        user.email?.toLowerCase().includes(query)
    );
});

const totalPages = computed(() =>
    Math.ceil(filteredUsers.value.length / itemsPerPage.value)
);

const paginatedUsers = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage.value;
    const end = start + itemsPerPage.value;
    return filteredUsers.value.slice(start, end);
});

// --- FUNÇÕES DE API ---

// 1. CARREGAR UTILIZADORES
async function loadUsers() {
    isLoading.value = true;
    try {
        const response = await $fetch(`${api}/utilizadores`, {
            method: "GET",
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        users.value = response.map(user => ({
            ...user,
            active: user.active !== undefined ? user.active : true
        }));
    } catch (e) {
        console.error("Erro:", e);
        showFeedback("Erro ao carregar utilizadores", true);
    } finally {
        isLoading.value = false;
    }
}

// 2. CRIAR UTILIZADOR
async function createUser() {
    feedbackMessage.value = "";
    try {
        await $fetch(`${api}/utilizadores`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${authStore.token}`
            },
            body: userFormData
        });

        showFeedback(`Utilizador "${userFormData.username}" criado!`, false);
        setTimeout(() => {
            closeCreateModal();
            loadUsers();
        }, 1500);
    } catch (e: any) {
        handleApiError(e, "Erro ao criar utilizador");
    }
}

// 3. ATUALIZAR UTILIZADOR
async function updateUser() {
    try {
        const body: any = {
            name: userFormData.name,
            email: userFormData.email,
            role: userFormData.role // Assumindo que podes mudar role aqui
        };
        if (userFormData.password) body.password = userFormData.password;

        await $fetch(`${api}/utilizadores/${editingUserId.value}`, {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${authStore.token}`
            },
            body
        });

        showFeedback("Utilizador atualizado!", false);
        setTimeout(() => {
            closeCreateModal();
            loadUsers();
        }, 1500);
    } catch (e: any) {
        handleApiError(e, "Erro ao atualizar");
    }
}

// 4. GERIR ESTADO (BOTÃO DA TABELA)
function handleStatusClick(user) {
    if (user.active) {
        // Se está ATIVO -> Abrir Modal de Suspensão
        userToSuspend.value = user;
        suspendMotive.value = ""; // Limpar motivo anterior
        showSuspendModal.value = true;
    } else {
        // Se está SUSPENSO -> Ativar diretamente
        activateUser(user);
    }
}

// 4.1 ATIVAR (Direto)
async function activateUser(user) {
    try {
        await $fetch(`${api}/utilizadores/${user.username}/estado`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${authStore.token}`
            },
            body: { active: true, statusReason: null }
        });

        // Atualização Otimista (Instantânea na UI)
        user.active = true;
        showFeedback(`Utilizador ${user.username} ativado!`, false);
    } catch (e: any) {
        handleApiError(e, "Erro ao ativar utilizador");
    }
}

// 4.2 SUSPENDER (Chamado pelo Modal)
async function confirmSuspend() {
    if (!suspendMotive.value || suspendMotive.value.trim() === "") {
        alert("O motivo é obrigatório!");
        return;
    }

    try {
        await $fetch(`${api}/utilizadores/${userToSuspend.value.username}/estado`, {
            method: "PATCH",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${authStore.token}`
            },
            body: {
                active: false,
                statusReason: suspendMotive.value
            }
        });

        // Atualização Otimista
        const userIndex = users.value.findIndex(u => u.username === userToSuspend.value.username);
        if (userIndex !== -1) {
            users.value[userIndex].active = false;
        }

        showFeedback("Utilizador suspenso com sucesso!", false);
        showSuspendModal.value = false;
        userToSuspend.value = null;

    } catch (e: any) {
        handleApiError(e, "Erro ao suspender");
    }
}

// 5. REMOVER UTILIZADOR
async function deleteUser(username, displayName) {
    if (!confirm(`Remover "${displayName}" permanentemente?`)) return;

    try {
        await $fetch(`${api}/utilizadores/${username}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        // Remove da lista localmente
        users.value = users.value.filter(u => u.username !== username);
        showFeedback("Utilizador removido.", false);
    } catch (e) {
        handleApiError(e, "Erro ao remover");
    }
}

// --- UTILITÁRIOS ---

function openCreateModal() {
    isEditing.value = false;
    editingUserId.value = null;
    resetForm();
    showModal.value = true;
}

function openEditModal(user) {
    isEditing.value = true;
    editingUserId.value = user.username;
    userFormData.username = user.username;
    userFormData.name = user.name;
    userFormData.email = user.email;
    userFormData.role = user.role;
    userFormData.password = "";
    showModal.value = true;
}

function closeCreateModal() {
    showModal.value = false;
    resetForm();
}

function resetForm() {
    userFormData.username = "";
    userFormData.password = "";
    userFormData.name = "";
    userFormData.email = "";
    userFormData.role = "COLLABORATOR";
    feedbackMessage.value = "";
    isError.value = false;
}

function submitForm() {
    isEditing.value ? updateUser() : createUser();
}

function changePage(page) {
    if (page >= 1 && page <= totalPages.value) currentPage.value = page;
}

function showFeedback(msg, error = false) {
    feedbackMessage.value = msg;
    isError.value = error;
    setTimeout(() => feedbackMessage.value = "", 3000);
}

function handleApiError(e, defaultMsg) {
    isError.value = true;
    console.error(e);
    // Tenta ler a mensagem de erro do backend ou usa a default
    const serverMsg = e.response?._data?.message || e.response?._data || defaultMsg;
    feedbackMessage.value = serverMsg;
}

onMounted(() => {
    if (authStore.user?.role === 'ADMINISTRATOR') {
        loadUsers();
    }
});
</script>

<template>
    <!-- MENSAGEM DE ACESSO NEGADO -->
    <div v-if="!authStore.user || authStore.user.role !== 'ADMINISTRATOR'"
         class="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-red-600 font-bold">
        <Shield :size="64" class="mb-4 text-red-500" />
        <h1 class="text-2xl">ACESSO NEGADO</h1>
        <p class="text-gray-600 font-normal mt-2">Esta página é restrita a Administradores.</p>
        <NuxtLink to="/" class="mt-6 px-4 py-2 bg-indigo-600 text-white rounded-md hover:bg-indigo-700 font-normal text-sm">Voltar ao Início</NuxtLink>
    </div>

    <!-- CONTEÚDO PRINCIPAL -->
    <div v-else class="min-h-screen bg-gray-50 p-6 lg:p-10 font-sans text-slate-800">
        <div class="max-w-7xl mx-auto">

            <!-- Header -->
            <div class="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4">
                <div>
                    <h1 class="text-3xl font-extrabold text-slate-900 tracking-tight">Gestão de Utilizadores</h1>
                    <p class="text-slate-500 text-sm mt-1">Gerir acessos, roles e estados da plataforma.</p>
                </div>
                <button @click="openCreateModal"
                        class="inline-flex items-center gap-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-lg text-sm font-bold shadow-sm transition-all">
                    <Plus :size="18" /> Novo Utilizador
                </button>
            </div>

            <!-- Feedback Global -->
            <div v-if="feedbackMessage" class="mb-6 p-4 rounded-lg text-sm font-bold text-center shadow-sm animate-fade-in-down"
                 :class="isError ? 'bg-red-100 text-red-800 border border-red-200' : 'bg-green-100 text-green-800 border border-green-200'">
                {{ feedbackMessage }}
            </div>

            <!-- Barra de Pesquisa -->
            <div class="bg-white p-4 rounded-t-xl border border-gray-200 border-b-0 flex flex-col sm:flex-row gap-4 justify-between items-center">
                <div class="relative w-full sm:w-96">
                    <Search class="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400" :size="18" />
                    <input
                        v-model="searchQuery"
                        type="text"
                        placeholder="Pesquisar utilizador..."
                        class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-indigo-500 outline-none transition-all"
                    />
                </div>
                <div class="text-xs font-bold text-gray-500 uppercase tracking-wider">
                    Total: {{ filteredUsers.length }} registos
                </div>
            </div>

            <!-- Tabela -->
            <div class="bg-white border border-gray-200 rounded-b-xl shadow-sm overflow-hidden">
                <div class="overflow-x-auto">
                    <table class="min-w-full divide-y divide-gray-200">
                        <thead class="bg-gray-50">
                        <tr>
                            <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Utilizador</th>
                            <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Role</th>
                            <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase tracking-wider">Estado</th>
                            <th class="px-6 py-3 text-right text-xs font-bold text-gray-500 uppercase tracking-wider">Ações</th>
                        </tr>
                        </thead>
                        <tbody class="bg-white divide-y divide-gray-200 text-sm">
                        <tr v-if="isLoading">
                            <td colspan="4" class="px-6 py-10 text-center text-gray-500">A carregar...</td>
                        </tr>
                        <tr v-else-if="paginatedUsers.length === 0">
                            <td colspan="4" class="px-6 py-10 text-center text-gray-500 italic">Nenhum utilizador encontrado.</td>
                        </tr>
                        <tr v-for="user in paginatedUsers" :key="user.username" class="hover:bg-gray-50 transition-colors">
                            <!-- Info -->
                            <td class="px-6 py-4 whitespace-nowrap">
                                <div class="flex items-center">
                                    <div class="flex-shrink-0 h-10 w-10 bg-indigo-100 rounded-full flex items-center justify-center text-indigo-700 font-bold text-lg border border-indigo-200">
                                        {{ user.name?.charAt(0).toUpperCase() }}
                                    </div>
                                    <div class="ml-4">
                                        <div class="text-sm font-bold text-gray-900">{{ user.name }}</div>
                                        <div class="text-xs text-gray-500">@{{ user.username }}</div>
                                    </div>
                                </div>
                            </td>
                            <!-- Role -->
                            <td class="px-6 py-4 whitespace-nowrap">
                                    <span :class="['px-2 py-1 inline-flex text-xs leading-5 font-bold rounded-full uppercase tracking-wide',
                                        user.role === 'ADMINISTRATOR' ? 'bg-purple-100 text-purple-800' :
                                        user.role === 'RESPONSIBLE' ? 'bg-blue-100 text-blue-800' : 'bg-gray-100 text-gray-800']">
                                        {{ user.role }}
                                    </span>
                            </td>
                            <!-- Status -->
                            <td class="px-6 py-4 whitespace-nowrap">
                                    <span :class="['px-2 py-1 inline-flex text-xs leading-5 font-bold rounded-full flex items-center gap-1 w-fit',
                                        user.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800']">
                                        <component :is="user.active ? CheckCircle : XCircle" :size="14" />
                                        {{ user.active ? 'Ativo' : 'Suspenso' }}
                                    </span>
                            </td>
                            <!-- Ações -->
                            <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                <div class="flex justify-end gap-2">
                                    <!-- Botão Status (Inteligente) -->
                                    <button @click="handleStatusClick(user)"
                                            class="p-2 rounded-lg transition-colors border border-transparent hover:border-gray-200 hover:bg-white hover:shadow-sm"
                                            :title="user.active ? 'Suspender' : 'Ativar'">
                                        <component :is="user.active ? XCircle : CheckCircle"
                                                   :class="user.active ? 'text-amber-600' : 'text-green-600'" :size="18" />
                                    </button>

                                    <button @click="openEditModal(user)" class="p-2 text-indigo-600 hover:bg-indigo-50 rounded-lg transition-colors border border-transparent hover:border-indigo-100" title="Editar">
                                        <Edit :size="18" />
                                    </button>

                                    <button @click="deleteUser(user.username, user.name)" class="p-2 text-red-600 hover:bg-red-50 rounded-lg transition-colors border border-transparent hover:border-red-100" title="Remover">
                                        <Trash2 :size="18" />
                                    </button>
                                </div>
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                <!-- Paginação -->
                <div class="bg-gray-50 px-4 py-3 flex items-center justify-between border-t border-gray-200">
                    <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1" class="px-4 py-2 border border-gray-300 text-sm font-medium rounded-md bg-white hover:bg-gray-50 disabled:opacity-50 transition-all flex items-center gap-1">
                        <ChevronLeft :size="16" /> Anterior
                    </button>
                    <span class="text-sm text-gray-700 font-medium">Página {{ currentPage }} de {{ totalPages }}</span>
                    <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages" class="px-4 py-2 border border-gray-300 text-sm font-medium rounded-md bg-white hover:bg-gray-50 disabled:opacity-50 transition-all flex items-center gap-1">
                        Próximo <ChevronRight :size="16" />
                    </button>
                </div>
            </div>
        </div>

        <!-- MODAL DE SUSPENSÃO -->
        <div v-if="showSuspendModal" class="fixed z-50 inset-0 overflow-y-auto">
            <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
                <div class="fixed inset-0 bg-gray-900 bg-opacity-60 transition-opacity backdrop-blur-sm" @click="showSuspendModal = false"></div>

                <div class="inline-block align-middle bg-white rounded-2xl text-left overflow-hidden shadow-2xl transform transition-all sm:my-8 sm:max-w-lg w-full border border-gray-100">
                    <div class="bg-white px-6 pt-6 pb-6">
                        <div class="sm:flex sm:items-start">
                            <div class="mx-auto flex-shrink-0 flex items-center justify-center h-12 w-12 rounded-full bg-red-100 sm:mx-0 sm:h-10 sm:w-10">
                                <AlertTriangle class="h-6 w-6 text-red-600" />
                            </div>
                            <div class="mt-3 text-center sm:mt-0 sm:ml-4 sm:text-left w-full">
                                <h3 class="text-lg leading-6 font-bold text-gray-900">
                                    Suspender Utilizador
                                </h3>
                                <div class="mt-2">
                                    <p class="text-sm text-gray-500">
                                        Estás prestes a suspender o acesso de <strong class="text-gray-800">{{ userToSuspend?.name }}</strong> (@{{ userToSuspend?.username }}).
                                        <br>Esta ação impede o utilizador de fazer login.
                                    </p>

                                    <div class="mt-4">
                                        <label class="block text-xs font-bold text-gray-700 uppercase tracking-wide mb-2">
                                            Motivo da Suspensão <span class="text-red-500">*</span>
                                        </label>
                                        <textarea
                                            v-model="suspendMotive"
                                            rows="3"
                                            class="w-full p-3 border border-gray-300 rounded-lg shadow-sm focus:ring-2 focus:ring-red-500 focus:border-red-500 sm:text-sm resize-none"
                                            placeholder="Ex: Violação dos termos de uso, comportamento inadequado..."
                                        ></textarea>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                    <div class="bg-gray-50 px-6 py-4 sm:flex sm:flex-row-reverse gap-2">
                        <button @click="confirmSuspend" type="button" class="w-full inline-flex justify-center rounded-lg border border-transparent shadow-sm px-4 py-2 bg-red-600 text-base font-medium text-white hover:bg-red-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-red-500 sm:w-auto sm:text-sm">
                            Confirmar Suspensão
                        </button>
                        <button @click="showSuspendModal = false" type="button" class="mt-3 w-full inline-flex justify-center rounded-lg border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm">
                            Cancelar
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <!-- MODAL DE CRIAR/EDITAR  -->
        <div v-if="showModal" class="fixed z-50 inset-0 overflow-y-auto">
            <div class="flex items-center justify-center min-h-screen px-4 pt-4 pb-20 text-center sm:block sm:p-0">
                <div class="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" @click="closeCreateModal"></div>
                <span class="hidden sm:inline-block sm:align-middle sm:h-screen">&#8203;</span>
                <div class="inline-block align-middle bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:max-w-lg w-full">
                    <div class="bg-white px-4 pt-5 pb-4 sm:p-6 sm:pb-4">
                        <div class="flex justify-between items-start mb-4">
                            <h3 class="text-lg leading-6 font-bold text-gray-900">
                                {{ isEditing ? 'Editar Utilizador' : 'Criar Novo Utilizador' }}
                            </h3>
                            <button @click="closeCreateModal" class="text-gray-400 hover:text-gray-500 transition-colors">
                                <X :size="24" />
                            </button>
                        </div>
                        <form @submit.prevent="submitForm" class="space-y-4">
                            <div><label class="block text-sm font-bold text-gray-700">Username</label><input v-model="userFormData.username" type="text" required :disabled="isEditing" :class="['mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm border p-2', isEditing ? 'bg-gray-100 cursor-not-allowed' : '']" /></div>
                            <div><label class="block text-sm font-bold text-gray-700">Nome</label><input v-model="userFormData.name" type="text" required class="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm border p-2" /></div>
                            <div><label class="block text-sm font-bold text-gray-700">Email</label><input v-model="userFormData.email" type="email" required class="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm border p-2" /></div>
                            <div>
                                <label class="block text-sm font-bold text-gray-700">Role</label>
                                <select v-model="userFormData.role"
                                        class="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm border py-2 pl-3 pr-10 bg-white focus:outline-none focus:ring-indigo-500 focus:border-indigo-500">
                                    <option value="COLLABORATOR">Colaborador</option>
                                    <option value="RESPONSIBLE">Responsável</option>
                                    <option value="ADMINISTRATOR">Administrador</option>
                                </select>
                            </div>
                            <div v-if="!isEditing">
                                <label class="block text-sm font-bold text-gray-700">Password </label>
                                <input v-model="userFormData.password" type="password" :required="!isEditing" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm sm:text-sm border p-2" />
                            </div>
                            <div class="mt-6 sm:flex sm:flex-row-reverse gap-3">
                                <button type="submit"
                                        class="w-full inline-flex justify-center rounded-md border border-transparent shadow-sm px-4 py-2 bg-indigo-600 text-base font-medium text-white hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:w-auto sm:text-sm">
                                    {{ isEditing ? 'Guardar' : 'Criar' }}
                                </button>

                                <button type="button" @click="closeCreateModal"
                                        class="mt-3 w-full inline-flex justify-center rounded-md border border-gray-300 shadow-sm px-4 py-2 bg-white text-base font-medium text-gray-700 hover:bg-gray-50 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500 sm:mt-0 sm:w-auto sm:text-sm">
                                    Cancelar
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
