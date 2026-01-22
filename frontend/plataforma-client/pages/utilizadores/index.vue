<script setup lang="ts">
import { ref, reactive, computed, onMounted } from "vue";
import { useAuthStore } from "~/stores/auth-store";
import {
    Search, Plus, Edit, Trash2, Shield, Eye, Save,
    CheckCircle, XCircle, ChevronLeft, ChevronRight, AlertTriangle, X
} from "lucide-vue-next";

const config = useRuntimeConfig();
const api = config.public.apiBase;
const authStore = useAuthStore();
const router = useRouter();

// --- ESTADO GERAL ---
const users = ref([]);
const isLoading = ref(false);
const currentPage = ref(1);
const itemsPerPage = ref(10);
const searchQuery = ref("");
const feedbackMessage = ref("");
const isError = ref(false);

// --- MODAL SUSPENSÃO ---
const showSuspendModal = ref(false);
const userToSuspend = ref(null);
const suspendMotive = ref("");

// --- MODAL CRIAR ---
const showCreateModal = ref(false);
const createFormData = reactive({
    username: "",
    password: "",
    name: "",
    email: "",
    role: "COLLABORATOR"
});

// --- ROLES (TRADUÇÃO) ---
const roleLabels = {
    ADMINISTRATOR: 'Administrador',
    RESPONSIBLE: 'Responsável',
    COLLABORATOR: 'Colaborador'
};

const roleClasses = {
    ADMINISTRATOR: 'bg-purple-100 text-purple-800',
    RESPONSIBLE: 'bg-blue-100 text-blue-800',
    COLLABORATOR: 'bg-gray-100 text-gray-800'
};

// --- COMPUTED ---
const filteredUsers = computed(() => {
    if (!searchQuery.value) return users.value;
    const query = searchQuery.value.toLowerCase();
    return users.value.filter(u =>
        u.username?.toLowerCase().includes(query) ||
        u.name?.toLowerCase().includes(query) ||
        u.email?.toLowerCase().includes(query)
    );
});

const totalPages = computed(() => Math.ceil(filteredUsers.value.length / itemsPerPage.value));

const paginatedUsers = computed(() => {
    const start = (currentPage.value - 1) * itemsPerPage.value;
    return filteredUsers.value.slice(start, start + itemsPerPage.value);
});

// --- API ---
async function loadUsers() {
    isLoading.value = true;
    try {
        const response = await $fetch(`${api}/utilizadores`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        // Proteção contra resposta nula
        users.value = response || [];
    } catch (e) {
        console.error(e);
        showFeedback("Erro ao carregar utilizadores", true);
    } finally {
        isLoading.value = false;
    }
}

async function createUser() {
    try {
        await $fetch(`${api}/utilizadores`, {
            method: "POST",
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: createFormData
        });

        showFeedback(`Utilizador "${createFormData.username}" criado com sucesso!`);
        showCreateModal.value = false;
        resetCreateForm();
        loadUsers();
    } catch (e: any) {
        const msg = e.response?._data || "Erro ao criar utilizador.";
        alert(msg);
    }
}

function handleStatusClick(user) {
    if (user.active) {
        userToSuspend.value = user;
        suspendMotive.value = "";
        showSuspendModal.value = true;
    } else {
        activateUser(user);
    }
}

async function activateUser(user) {
    try {
        await $fetch(`${api}/utilizadores/${user.username}/estado`, {
            method: "PATCH",
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: { active: true, statusReason: null }
        });

        // Atualização Otimista
        user.active = true;
        showFeedback(`Utilizador ${user.username} ativado!`);
    } catch (e) {
        showFeedback("Erro ao ativar", true);
    }
}

async function confirmSuspend() {
    if (!suspendMotive.value || suspendMotive.value.trim() === "") {
        alert("O motivo é obrigatório!");
        return;
    }
    const username = userToSuspend.value.username;

    try {
        await $fetch(`${api}/utilizadores/${username}/estado`, {
            method: "PATCH",
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: { active: false, statusReason: suspendMotive.value }
        });

        showFeedback("Utilizador suspenso com sucesso!");
        showSuspendModal.value = false;

        // Atualização Local
        const idx = users.value.findIndex(u => u.username === username);
        if(idx !== -1) users.value[idx].active = false;

    } catch (e) {
        showFeedback("Erro ao suspender", true);
    }
}

async function deleteUser(username) {
    if (!confirm(`Remover ${username} permanentemente?`)) return;
    try {
        await $fetch(`${api}/utilizadores/${username}`, {
            method: "DELETE",
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        users.value = users.value.filter(u => u.username !== username);
        showFeedback("Utilizador removido.");
    } catch (e) {
        showFeedback("Erro ao remover", true);
    }
}

// --- AUXILIARES ---
function openCreateModal() {
    resetCreateForm();
    showCreateModal.value = true;
}

function resetCreateForm() {
    createFormData.username = "";
    createFormData.password = "";
    createFormData.name = "";
    createFormData.email = "";
    createFormData.role = "COLLABORATOR";
}

function showFeedback(msg, error = false) {
    feedbackMessage.value = msg;
    isError.value = error;
    setTimeout(() => feedbackMessage.value = "", 3000);
}

function navigateToEdit(username) { router.push(`/utilizadores/editar/${username}`); }
function navigateToDetails(username) { router.push(`/utilizadores/${username}`); }

onMounted(() => {
    if (authStore.user?.role === 'ADMINISTRATOR') loadUsers();
});
</script>

<template>
    <div v-if="authStore.user?.role !== 'ADMINISTRATOR'" class="text-center mt-20 text-red-600 font-bold">
        <Shield :size="48" class="mx-auto mb-2"/> Acesso Negado
    </div>

    <div v-else class="min-h-screen bg-gray-50 p-6 lg:p-10 font-sans text-slate-800">
        <div class="max-w-7xl mx-auto">

            <div class="flex justify-between items-center mb-8">
                <div>
                    <h1 class="text-3xl font-extrabold text-slate-900">Gestão de Utilizadores</h1>
                    <p class="text-slate-500 text-sm">Gerir acessos e estados da plataforma.</p>
                </div>
                <button @click="openCreateModal" class="bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-lg flex items-center gap-2 font-bold text-sm shadow-sm transition-all">
                    <Plus :size="18"/> Novo Utilizador
                </button>
            </div>

            <div v-if="feedbackMessage" :class="`p-4 rounded-lg mb-6 text-center font-bold text-sm ${isError ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`">
                {{ feedbackMessage }}
            </div>

            <div class="bg-white p-4 rounded-t-xl border border-gray-200 flex justify-between items-center">
                <div class="relative w-96">
                    <Search class="absolute left-3 top-2.5 text-gray-400" :size="18"/>
                    <input v-model="searchQuery" type="text" placeholder="Pesquisar..." class="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none text-sm"/>
                </div>
                <span class="text-xs font-bold text-gray-500 uppercase">Total: {{ filteredUsers.length }}</span>
            </div>

            <div class="bg-white border border-t-0 border-gray-200 rounded-b-xl shadow-sm overflow-hidden">
                <table class="min-w-full divide-y divide-gray-200">
                    <thead class="bg-gray-50">
                    <tr>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase">Utilizador</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase">Role</th>
                        <th class="px-6 py-3 text-left text-xs font-bold text-gray-500 uppercase">Estado</th>
                        <th class="px-6 py-3 text-right text-xs font-bold text-gray-500 uppercase">Ações</th>
                    </tr>
                    </thead>
                    <tbody class="divide-y divide-gray-200 text-sm">
                    <tr v-for="user in paginatedUsers" :key="user.username" class="hover:bg-gray-50 transition-colors">
                        <td class="px-6 py-4">
                            <div class="font-bold text-gray-900">{{ user.name }}</div>
                            <div class="text-xs text-gray-500">@{{ user.username }}</div>
                        </td>
                        <td class="px-6 py-4">
                            <span :class="`px-2 py-1 text-xs font-bold rounded-full uppercase tracking-wide ${roleClasses[user.role] || 'bg-gray-100 text-gray-800'}`">
                                {{ roleLabels[user.role] || user.role }}
                            </span>
                        </td>
                        <td class="px-6 py-4">
                            <div v-if="user.active !== undefined">
                                <span :class="`flex items-center gap-1 w-fit px-2 py-1 text-xs font-bold rounded-full ${user.active ? 'bg-green-100 text-green-800' : 'bg-red-100 text-red-800'}`">
                                    <component :is="user.active ? CheckCircle : XCircle" :size="14"/>
                                    {{ user.active ? 'Ativo' : 'Suspenso' }}
                                </span>
                            </div>
                            <div v-else>
                                <span class="px-2 py-1 text-xs font-bold rounded-full bg-gray-100 text-gray-500">Indefinido</span>
                            </div>
                        </td>
                        <td class="px-6 py-4 text-right flex justify-end gap-2">
                            <button @click="navigateToDetails(user.username)" class="p-2 text-gray-500 hover:bg-gray-100 rounded-lg" title="Ver Detalhes"><Eye :size="18"/></button>
                            <button @click="handleStatusClick(user)" class="p-2 text-amber-600 hover:bg-amber-50 rounded-lg" title="Alterar Estado"><component :is="user.active ? XCircle : CheckCircle" :size="18"/></button>
                            <button @click="navigateToEdit(user.username)" class="p-2 text-indigo-600 hover:bg-indigo-50 rounded-lg" title="Editar"><Edit :size="18"/></button>
                            <button @click="deleteUser(user.username)" class="p-2 text-red-600 hover:bg-red-50 rounded-lg" title="Remover"><Trash2 :size="18"/></button>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <div class="px-4 py-3 border-t flex justify-between items-center">
                    <button @click="currentPage--" :disabled="currentPage===1" class="disabled:opacity-50"><ChevronLeft/></button>
                    <span>{{ currentPage }} / {{ totalPages }}</span>
                    <button @click="currentPage++" :disabled="currentPage>=totalPages" class="disabled:opacity-50"><ChevronRight/></button>
                </div>
            </div>
        </div>

        <div v-if="showSuspendModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm">
            <div class="bg-white p-6 rounded-lg w-full max-w-md shadow-xl">
                <div class="flex items-center gap-3 mb-4 text-red-600 font-bold text-lg">
                    <AlertTriangle/> Suspender Utilizador
                </div>
                <p class="text-sm text-gray-600 mb-4">Motivo para suspender <b>{{ userToSuspend?.username }}</b>:</p>
                <textarea v-model="suspendMotive" class="w-full border rounded p-2 mb-4" rows="3"></textarea>
                <div class="flex justify-end gap-2">
                    <button @click="showSuspendModal=false" class="px-4 py-2 border rounded">Cancelar</button>
                    <button @click="confirmSuspend" class="px-4 py-2 bg-red-600 text-white rounded font-bold">Confirmar</button>
                </div>
            </div>
        </div>

        <div v-if="showCreateModal" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
            <div class="bg-white rounded-xl shadow-xl w-full max-w-lg overflow-hidden">
                <div class="bg-gray-50 px-6 py-4 border-b flex justify-between items-center">
                    <h3 class="text-lg font-bold text-gray-900">Novo Utilizador</h3>
                    <button @click="showCreateModal=false" class="text-gray-400 hover:text-gray-600"><X :size="20"/></button>
                </div>

                <form @submit.prevent="createUser" class="p-6 space-y-4">
                    <div>
                        <label class="block text-sm font-bold text-gray-700 mb-1">Username</label>
                        <input v-model="createFormData.username" type="text" required class="w-full p-2 border rounded-md focus:ring-2 focus:ring-indigo-500 outline-none"/>
                    </div>
                    <div>
                        <label class="block text-sm font-bold text-gray-700 mb-1">Nome</label>
                        <input v-model="createFormData.name" type="text" required class="w-full p-2 border rounded-md focus:ring-2 focus:ring-indigo-500 outline-none"/>
                    </div>
                    <div>
                        <label class="block text-sm font-bold text-gray-700 mb-1">Email</label>
                        <input v-model="createFormData.email" type="email" required class="w-full p-2 border rounded-md focus:ring-2 focus:ring-indigo-500 outline-none"/>
                    </div>
                    <div>
                        <label class="block text-sm font-bold text-gray-700 mb-1">Password</label>
                        <input v-model="createFormData.password" type="password" required class="w-full p-2 border rounded-md focus:ring-2 focus:ring-indigo-500 outline-none"/>
                    </div>
                    <div>
                        <label class="block text-sm font-bold text-gray-700 mb-1">Role</label>
                        <select v-model="createFormData.role" class="w-full p-2 border rounded-md bg-white focus:ring-2 focus:ring-indigo-500 outline-none">
                            <option value="COLLABORATOR">Colaborador</option>
                            <option value="RESPONSIBLE">Responsável</option>
                            <option value="ADMINISTRATOR">Administrador</option>
                        </select>
                    </div>

                    <div class="pt-4 flex justify-end gap-3">
                        <button type="button" @click="showCreateModal=false" class="px-4 py-2 border rounded-lg text-gray-700 hover:bg-gray-50">Cancelar</button>
                        <button type="submit" class="px-4 py-2 bg-indigo-600 text-white rounded-lg font-bold hover:bg-indigo-700 flex items-center gap-2">
                            <Save :size="18"/> Criar
                        </button>
                    </div>
                </form>
            </div>
        </div>
    </div>
</template>
