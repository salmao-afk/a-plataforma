<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { useTagsStore } from '~/stores/tags-store.js';
import { Eye, EyeOff, Trash2, Filter } from 'lucide-vue-next';

const config = useRuntimeConfig();
const authStore = useAuthStore();
const tagStore = useTagsStore();
const api = config.public.apiBase;

// --- ESTADO ---
const searchQuery = ref("");
const showOnlySubscribed = ref(false);
const mySubscribedTags = ref([]);

// Filtro de Visibilidade
const showHiddenTags = ref(false);
const recentlyHiddenIds = ref(new Set());

// --- MODAIS E ESTADO DE AÇÃO ---
const isDeleteModalOpen = ref(false);
const tagToDelete = ref(null);

const isVisibilityModalOpen = ref(false);
const tagToToggle = ref(null);
const hideReason = ref(""); // Guarda o motivo escrito

const toast = reactive({ show: false, message: '', type: 'success' });

// Permissões
const canManageTags = computed(() => {
    return authStore.user?.role === 'ADMINISTRATOR' || authStore.user?.role === 'RESPONSIBLE';
});

// --- FETCH INICIAL ---
async function fetchMySubscriptions() {
    try {
        const response = await $fetch(`${api}/utilizadores/me/tags-subscritas`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        mySubscribedTags.value = response || [];
        if(authStore.user) authStore.user.subscribedTags = response;
    } catch (e) {
        console.error("Erro subs:", e);
    }
}

// --- FILTRO ---
const filteredTags = computed(() => {
    let tags = tagStore.tags;

    if (!showHiddenTags.value) {
        tags = tags.filter(t => {
            if (t.visible !== false) return true;
            if (recentlyHiddenIds.value.has(t.id)) return true;
            return false;
        });
    }

    if (showOnlySubscribed.value) {
        const subIds = new Set(mySubscribedTags.value.map(t => t.id));
        tags = tags.filter(t => subIds.has(t.id));
    }

    if (searchQuery.value) {
        const q = searchQuery.value.toLowerCase();
        tags = tags.filter(t =>
            t.name.toLowerCase().includes(q) || t.description?.toLowerCase().includes(q)
        );
    }

    return tags;
});

function isSubscribed(tagId) {
    return mySubscribedTags.value.some(t => t.id === tagId);
}

function showToast(message, type = 'success') {
    toast.message = message; toast.type = type; toast.show = true;
    setTimeout(() => { toast.show = false; }, 3000);
}

// --- 1. PEDIR PARA ALTERAR VISIBILIDADE (Abre Modal) ---
function requestToggleVisibility(tag) {
    tagToToggle.value = tag;
    hideReason.value = ""; // Limpar motivo anterior
    isVisibilityModalOpen.value = true;
}

// --- 2. CONFIRMAR VISIBILIDADE (Chamada API) ---
async function confirmToggleVisibility() {
    if (!tagToToggle.value) return;
    const tag = tagToToggle.value;

    const previousState = tag.visible;
    const tagInStore = tagStore.tags.find(t => t.id === tag.id);

    // Se visible for undefined, assume true. Inverte o valor.
    const currentVisibility = tagInStore ? (tagInStore.visible !== false) : (tag.visible !== false);
    const newVisibility = !currentVisibility;

    // Atualização Otimista
    if (tagInStore) tagInStore.visible = newVisibility;

    // Gestão do Badge Visual ("Vanishing Act")
    if (newVisibility === false) {
        recentlyHiddenIds.value.add(tag.id);
    } else {
        recentlyHiddenIds.value.delete(tag.id);
    }

    // Preparar o motivo (só envia se estivermos a ocultar)
    const reasonToSend = (newVisibility === false) ? (hideReason.value || "Sem motivo especificado") : null;

    try {
        await $fetch(`${api}/tags/${tag.id}/alterar-visibilidade`, {
            method: 'PATCH', // Mantém PATCH conforme o teu backend @PATCH
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: {
                hideReason: reasonToSend
            }
        });

        const status = newVisibility ? "visível" : "oculta";
        showToast(`Tag ${status}. (Atualize a página para limpar)`);
        isVisibilityModalOpen.value = false;
        tagToToggle.value = null;

    } catch (e) {
        console.error(e);
        // Rollback
        if (tagInStore) tagInStore.visible = previousState;

        if (!previousState) recentlyHiddenIds.value.add(tag.id);
        else recentlyHiddenIds.value.delete(tag.id);

        showToast("Erro ao alterar visibilidade.", "error");
    }
}

// Ações Delete
function confirmDelete(tag) { tagToDelete.value = tag; isDeleteModalOpen.value = true; }

async function handleDelete() {
    if (!tagToDelete.value) return;

    try {
        await tagStore.deleteTag(tagToDelete.value.id);
        showToast("Tag eliminada com sucesso.");
        tagStore.tags = tagStore.tags.filter(t => t.id !== tagToDelete.value.id);
        mySubscribedTags.value = mySubscribedTags.value.filter(t => t.id !== tagToDelete.value.id);
        isDeleteModalOpen.value = false;
    } catch (e) {
        console.error("Erro ao apagar:", e);
        if (e.response && e.response.status === 500) {
            showToast("Não é possível apagar tags que já estejam associadas.", "error");
        } else {
            showToast("Ocorreu um erro ao apagar.", "error");
        }
        isDeleteModalOpen.value = false;
    }
}

async function handleSubscribe(tag) {
    try {
        await tagStore.subscribeTag(tag.id);
        mySubscribedTags.value.push(tag);
        showToast(`Subscrito a #${tag.name}`);
    } catch(e) { showToast("Erro ao subscrever.", "error"); }
}

async function handleUnsubscribe(tag) {
    try {
        await tagStore.unsubscribeTag(tag.id);
        mySubscribedTags.value = mySubscribedTags.value.filter(t => t.id !== tag.id);
        showToast("Subscrição removida.");
    } catch(e) { showToast("Erro ao remover.", "error"); }
}

onMounted(() => {
    if (authStore.token) {
        tagStore.fetchTags();
        fetchMySubscriptions();
    }
});
</script>

<template>
    <div class="max-w-6xl mx-auto space-y-6 relative p-6 font-sans text-slate-800">

        <div class="flex flex-col md:flex-row justify-between items-center gap-4 mb-2">
            <div>
                <h1 class="text-3xl font-extrabold text-slate-900 mb-1">Tags</h1>
                <p class="text-slate-500 text-sm">Explore tópicos e personalize o seu feed.</p>
            </div>

            <NuxtLink v-if="canManageTags" to="/tags/criar" class="bg-indigo-600 text-white px-5 py-2.5 rounded-lg font-bold hover:bg-indigo-700 transition shadow-md flex items-center gap-2 text-sm">
                <span>+</span> Criar Tag
            </NuxtLink>
        </div>

        <div class="bg-white p-2 rounded-xl shadow-sm border border-gray-200 flex flex-col md:flex-row gap-3 items-center">
            <div class="flex bg-gray-100 p-1 rounded-lg w-full md:w-auto flex-shrink-0">
                <button @click="showOnlySubscribed = false" :class="`flex-1 md:flex-none px-6 py-2 rounded-md text-sm font-bold transition ${!showOnlySubscribed ? 'bg-white text-indigo-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`">Todas</button>
                <button @click="showOnlySubscribed = true" :class="`flex-1 md:flex-none px-6 py-2 rounded-md text-sm font-bold transition ${showOnlySubscribed ? 'bg-white text-indigo-600 shadow-sm' : 'text-gray-500 hover:text-gray-700'}`">As Minhas</button>
            </div>
            <div class="relative flex-grow w-full">
                <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">🔍</div>
                <input v-model="searchQuery" type="text" placeholder="Filtrar por nome..." class="w-full pl-10 pr-4 py-2.5 border-none bg-transparent focus:ring-0 outline-none text-gray-700 placeholder-gray-400" />
            </div>

            <div v-if="canManageTags" class="border-l border-gray-200 pl-3 ml-1 hidden md:block">
                <button
                    @click="showHiddenTags = !showHiddenTags"
                    :class="`flex items-center gap-2 px-3 py-2 rounded-lg text-xs font-bold transition ${showHiddenTags ? 'bg-amber-100 text-amber-700 border border-amber-200' : 'bg-gray-50 text-gray-500 hover:bg-gray-100'}`"
                    title="Alternar visualização de tags ocultas"
                >
                    <Filter :size="16"/>
                    {{ showHiddenTags ? 'Ocultas: ON' : 'Ocultas: OFF' }}
                </button>
            </div>
        </div>

        <div v-if="filteredTags.length === 0" class="text-center py-20 bg-white rounded-xl border border-dashed border-gray-300">
            <div class="text-4xl mb-3">🏷️</div>
            <p class="text-gray-500 font-medium" v-if="showOnlySubscribed">Ainda não subscreveu nenhuma tag.</p>
            <p class="text-gray-500 font-medium" v-else>Nenhuma tag encontrada.</p>
            <button v-if="canManageTags && !showHiddenTags" @click="showHiddenTags = true" class="mt-4 text-indigo-600 text-sm font-bold hover:underline">
                Verificar tags ocultas?
            </button>
        </div>

        <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <div v-for="tag in filteredTags" :key="tag.id"
                 :class="`bg-white p-6 rounded-xl shadow-sm border transition flex flex-col justify-between h-full group relative overflow-hidden ${tag.visible === false ? 'border-red-200 bg-gray-50 opacity-80' : 'border-gray-200 hover:shadow-md'}`">

                <div v-if="tag.visible === false" class="absolute top-2 right-2 bg-red-100 text-red-600 text-xs font-bold px-2 py-1 rounded border border-red-200 z-10 pointer-events-none">
                    OCULTA
                </div>

                <div v-if="isSubscribed(tag.id)" class="absolute top-0 left-0 right-0 h-1 bg-green-500"></div>

                <div>
                    <div class="flex items-center justify-between mb-2">
                        <NuxtLink :to="`/tags/${tag.id}`" class="text-lg font-bold text-slate-800 hover:text-indigo-600 transition truncate pr-2">
                            #{{ tag.name }}
                        </NuxtLink>
                    </div>
                    <p class="text-gray-500 text-sm leading-relaxed mb-4 line-clamp-2">
                        {{ tag.description || 'Sem descrição.' }}
                    </p>
                </div>

                <div class="pt-4 border-t border-gray-100 flex justify-between items-center mt-auto">

                    <div>
                        <button v-if="isSubscribed(tag.id)" @click="handleUnsubscribe(tag)" class="group flex items-center gap-1 text-xs font-bold px-3 py-1.5 rounded-md transition bg-green-50 text-green-700 border border-green-200 hover:bg-red-50 hover:text-red-600 hover:border-red-200">
                            <span class="group-hover:hidden flex items-center gap-1">✅ Subscrito</span>
                            <span class="hidden group-hover:flex items-center gap-1">❌ Remover</span>
                        </button>
                        <button v-else @click="handleSubscribe(tag)" class="flex items-center gap-1 text-xs font-bold px-3 py-1.5 rounded-md transition bg-indigo-600 text-white hover:bg-indigo-700 shadow-sm border border-transparent">
                            🔔 Subscrever
                        </button>
                    </div>

                    <div v-if="canManageTags" class="flex gap-2 opacity-0 group-hover:opacity-100 transition duration-200">

                        <button
                            @click="requestToggleVisibility(tag)"
                            :class="`p-1 rounded ${tag.visible === false ? 'text-green-600 bg-green-50 hover:bg-green-100' : 'text-gray-400 hover:text-gray-600 bg-gray-50 hover:bg-gray-100'}`"
                            :title="tag.visible === false ? 'Tornar Visível' : 'Ocultar Tag'"
                        >
                            <Eye v-if="tag.visible === false" :size="18" />
                            <EyeOff v-else :size="18" />
                        </button>

                        <NuxtLink :to="`/tags/editar/${tag.id}`" class="text-gray-400 hover:text-blue-600 p-1 bg-gray-50 rounded" title="Editar">✏️</NuxtLink>

                        <button @click="confirmDelete(tag)" class="text-gray-400 hover:text-red-600 p-1 bg-gray-50 rounded" title="Apagar">
                            <Trash2 :size="18"/>
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div v-if="toast.show" class="fixed bottom-6 right-6 z-50 animate-bounce-in">
            <div :class="toast.type === 'error' ? 'bg-red-500' : 'bg-green-600'" class="text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-3">
                <span>{{ toast.type === 'error' ? '⚠️' : '✅' }}</span>
                <p class="font-bold text-sm">{{ toast.message }}</p>
            </div>
        </div>

        <div v-if="isDeleteModalOpen" class="fixed inset-0 bg-black/50 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div class="bg-white rounded-xl shadow-xl p-6 text-center max-w-sm">
                <h3 class="text-lg font-bold text-gray-900 mb-2">Apagar Tag?</h3>
                <p class="text-gray-500 mb-6 text-sm">Tem a certeza que deseja remover "<b>{{ tagToDelete?.name }}</b>"?</p>
                <div class="flex justify-center gap-3">
                    <button @click="isDeleteModalOpen=false" class="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 font-medium">Cancelar</button>
                    <button @click="handleDelete" class="px-4 py-2 bg-red-600 text-white rounded-lg font-bold shadow hover:bg-red-700">Sim, Apagar</button>
                </div>
            </div>
        </div>

        <div v-if="isVisibilityModalOpen" class="fixed inset-0 bg-black/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
            <div class="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm w-full">

                <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4"
                     :class="tagToToggle?.visible === false ? 'bg-green-100 text-green-600' : 'bg-amber-100 text-amber-600'">
                    <Eye v-if="tagToToggle?.visible === false" :size="32"/>
                    <EyeOff v-else :size="32"/>
                </div>

                <h3 class="text-xl font-bold text-gray-900 mb-2">Alterar Visibilidade?</h3>

                <div v-if="tagToToggle?.visible !== false" class="text-left mb-4">
                    <p class="text-gray-500 mb-3 text-center text-sm">A tag deixará de estar visível para os utilizadores.</p>
                    <label class="block text-xs font-bold text-gray-700 mb-1 ml-1">Motivo (Obrigatório):</label>
                    <textarea
                        v-model="hideReason"
                        class="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-amber-500 outline-none transition bg-gray-50"
                        placeholder="Ex: Tópico duplicado, ofensivo..."
                        rows="3"
                    ></textarea>
                </div>

                <div v-else class="mb-6">
                    <p class="text-gray-500 text-sm">Deseja tornar a tag <strong>#{{ tagToToggle?.name }}</strong> visível novamente?</p>
                </div>

                <div class="flex flex-col gap-3">
                    <button @click="confirmToggleVisibility"
                            :disabled="tagToToggle?.visible !== false && !hideReason"
                            :class="`w-full py-3 text-white rounded-xl font-bold shadow-lg transition flex items-center justify-center gap-2 ${tagToToggle?.visible === false ? 'bg-green-600 hover:bg-green-700' : 'bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed'}`">
                        {{ tagToToggle?.visible === false ? 'Sim, Mostrar' : 'Sim, Ocultar' }}
                    </button>
                    <button @click="isVisibilityModalOpen=false" class="w-full py-3 bg-white text-gray-700 border border-gray-200 rounded-xl font-medium hover:bg-gray-50 transition">Cancelar</button>
                </div>
            </div>
        </div>

    </div>
</template>

<style scoped>
.animate-bounce-in { animation: bounceIn 0.5s; }
@keyframes bounceIn {
    0% { transform: scale(0.1); opacity: 0; }
    60% { transform: scale(1.2); opacity: 1; }
    100% { transform: scale(1); }
}
</style>
