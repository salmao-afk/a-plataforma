<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import { usePublicationsStore } from '~/stores/publications-store.js';
import { useTagsStore } from '~/stores/tags-store.js';
import PublicationStats from '~/components/rating/PublicationStats.vue';
import { Eye, EyeOff } from 'lucide-vue-next';

const config = useRuntimeConfig();
const authStore = useAuthStore();
const pubStore = usePublicationsStore();
const tagStore = useTagsStore();
const api = config.public.apiBase;

// --- ESTADO ---
const searchQuery = ref("");
const showMine = ref(false);
const sortOption = ref('newest');
const selectedUser = ref('');
const recentlyHiddenIds = ref(new Set());

// --- NOVO: Motivo da Ocultação ---
const hideReason = ref("");

// Modais
const isVisibilityModalOpen = ref(false);
const publicationToToggle = ref(null);
const isRemoveTagModalOpen = ref(false);
const tagToRemoveData = ref(null);

// Permissões
const canManageTags = computed(() => {
    const role = authStore.user?.role;
    return role === 'ADMINISTRATOR' || role === 'RESPONSIBLE';
});

const uniqueUsers = computed(() => {
    if (!pubStore.publications) return [];
    const users = pubStore.publications.map(p => p.uploaderUsername);
    return [...new Set(users)].sort();
});

// --- FILTRO INTELIGENTE ---
const filteredPublications = computed(() => {
    let pubs = pubStore.publications;

    pubs = pubs.filter(p => {
        if (p.visible === false) {
            return recentlyHiddenIds.value.has(p.id);
        }
        return true;
    });

    if (selectedUser.value && !showMine.value) {
        pubs = pubs.filter(p => p.uploaderUsername === selectedUser.value);
    }

    if (searchQuery.value) {
        const q = searchQuery.value.toLowerCase();
        pubs = pubs.filter(p => {
            const authors = Array.isArray(p.authors) ? p.authors.join(' ') : (p.authors || '');
            const tags = Array.isArray(p.tags) ? p.tags.join(' ') : '';
            return p.title?.toLowerCase().includes(q) ||
                p.scientificArea?.toLowerCase().includes(q) ||
                authors.toLowerCase().includes(q) ||
                tags.toLowerCase().includes(q);
        });
    }

    switch (sortOption.value) {
        case 'rating_desc': pubs.sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0)); break;
        case 'rating_asc': pubs.sort((a, b) => (a.averageRating || 0) - (b.averageRating || 0)); break;
        case 'newest': default: pubs.sort((a, b) => b.id - a.id); break;
    }

    return pubs;
});

const fetchPublications = async () => {
    if (!authStore.token) return;
    pubStore.isLoading = true;
    try {
        const endpoint = showMine.value ? `${api}/publicacoes/me` : `${api}/publicacoes`;
        const pubs = await $fetch(endpoint, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        const pubsWithRatings = await Promise.all(pubs.map(async (pub) => {
            try {
                const res = await $fetch(`${api}/publicacoes/${pub.id}/rating-medio`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                pub.averageRating = res.average || 0;
            } catch (e) { pub.averageRating = 0; }
            return pub;
        }));

        pubStore.publications = pubsWithRatings;
    } catch (e) { console.error(e); } finally { pubStore.isLoading = false; }
};

const downloadPublication = (id) => {
    pubStore.downloadPublication(id).catch(() => alert("Erro ao baixar."));
};

// --- ALTERAR VISIBILIDADE ---
const requestToggleVisibility = (pub) => {
    publicationToToggle.value = pub;
    hideReason.value = ""; // Resetar o motivo ao abrir o modal
    isVisibilityModalOpen.value = true;
};

const confirmToggleVisibility = async () => {
    if (!publicationToToggle.value) return;
    const pub = publicationToToggle.value;

    // Se estivermos a ocultar (estava visível), usamos o texto do input.
    // Se estivermos a mostrar, enviamos null ou string vazia.
    const reasonToSend = (pub.visible !== false) ? (hideReason.value || "Sem motivo especificado") : null;

    try {
        await $fetch(`${api}/publicacoes/${pub.id}/alterar-visibilidade`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: {
                hideReason: reasonToSend
            }
        });

        // Atualização Otimista
        const isVisible = pub.visible !== false;
        pub.visible = !isVisible;

        if (pub.visible === false) {
            recentlyHiddenIds.value.add(pub.id);
        } else {
            recentlyHiddenIds.value.delete(pub.id);
        }

        isVisibilityModalOpen.value = false;
        publicationToToggle.value = null;
    } catch (e) {
        console.error(e);
        alert("Erro ao alterar visibilidade: " + (e.response?._data || "Erro desconhecido"));
    }
};

// Remover Tag
const requestRemoveTag = (pub, tag) => { tagToRemoveData.value = { pub, tag }; isRemoveTagModalOpen.value = true; };
const confirmRemoveTag = async () => {
    if (!tagToRemoveData.value) return;
    const { pub, tag } = tagToRemoveData.value;
    const tagObject = tagStore.tags.find(t => t.name === tag);
    if (!tagObject) { alert("Tag não encontrada."); isRemoveTagModalOpen.value = false; return; }

    try {
        await $fetch(`${api}/publicacoes/${pub.id}/tags/${tagObject.id}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        pub.tags = pub.tags.filter(t => t !== tag);
        isRemoveTagModalOpen.value = false;
        tagToRemoveData.value = null;
    } catch (e) { alert("Erro ao remover tag."); }
};

const typeTranslations = { 'Thesis': 'Tese', 'Article': 'Artigo', 'Report': 'Relatório', 'Other': 'Outro', 'thesis': 'Tese', 'article': 'Artigo', 'report': 'Relatório', 'other': 'Outro' };

watch(showMine, () => { if(showMine.value) selectedUser.value = ''; fetchPublications(); });

onMounted(() => {
    if (authStore.token) {
        fetchPublications();
        tagStore.fetchTags();
    }
});
</script>

<template>
    <div class="max-w-6xl mx-auto space-y-6 mb-20 font-sans">

        <div v-if="pubStore.isLoading" class="flex justify-center py-10">
            <div class="animate-spin rounded-full h-10 w-10 border-b-2 border-indigo-600"></div>
        </div>

        <div v-if="!authStore.token" class="flex flex-col items-center justify-center py-20 bg-white rounded-xl shadow-sm border border-gray-100 text-center px-4">
            <h2 class="text-2xl font-bold text-gray-800 mb-2">Acesso Reservado</h2>
            <NuxtLink to="/auth/login" class="px-8 py-3 bg-indigo-600 text-white rounded-xl font-bold hover:bg-indigo-700 transition">Entrar</NuxtLink>
        </div>

        <div v-else class="space-y-6">
            <div class="flex flex-col md:flex-row justify-between items-center bg-white p-6 rounded-xl shadow-sm border border-gray-100 gap-4">
                <div>
                    <h1 class="text-2xl font-extrabold text-gray-900">Publicações</h1>
                    <p class="text-sm text-gray-500">Total: {{ filteredPublications.length }} documentos</p>
                </div>
                <div class="flex flex-wrap items-center gap-4">
                    <div class="flex items-center bg-indigo-50 px-4 py-2 rounded-lg border border-indigo-100 cursor-pointer">
                        <input type="checkbox" id="filterMine" v-model="showMine" class="w-5 h-5 text-indigo-600 rounded cursor-pointer">
                        <label for="filterMine" class="ml-2 text-sm font-bold text-indigo-800 cursor-pointer select-none">Minhas</label>
                    </div>
                    <NuxtLink to="/publicacoes/criar" class="bg-gray-900 text-white px-6 py-2.5 rounded-lg font-bold hover:bg-black transition flex items-center gap-2">
                        <span>+</span> Nova
                    </NuxtLink>
                </div>
            </div>

            <div class="bg-white p-4 rounded-xl shadow-sm border border-gray-100 grid grid-cols-1 md:grid-cols-12 gap-4">
                <div class="md:col-span-6 relative">
                    <div class="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-gray-400">🔍</div>
                    <input v-model="searchQuery" type="text" placeholder="Pesquisar..." class="w-full pl-10 px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none transition" />
                </div>
                <div class="md:col-span-3">
                    <select v-model="selectedUser" :disabled="showMine" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none bg-white disabled:bg-gray-100">
                        <option value="">Todos os Utilizadores</option>
                        <option v-for="user in uniqueUsers" :key="user" :value="user">@{{ user }}</option>
                    </select>
                </div>
                <div class="md:col-span-3">
                    <select v-model="sortOption" class="w-full px-4 py-2 border border-gray-200 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none bg-white">
                        <option value="newest">📅 Mais Recentes</option>
                        <option value="rating_desc">⭐ Rating Descendente</option>
                        <option value="rating_asc">⭐ Rating Ascendente</option>
                    </select>
                </div>
            </div>

            <div v-if="filteredPublications.length === 0 && !pubStore.isLoading" class="text-center py-16 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
                <p class="text-gray-400 text-lg">Nenhuma publicação encontrada.</p>
            </div>

            <div v-else class="grid grid-cols-1 gap-4">
                <div v-for="pub in filteredPublications" :key="pub.id"
                     :class="`bg-white p-6 rounded-xl shadow-sm border transition flex flex-col md:flex-row justify-between items-start gap-4 relative overflow-hidden ${pub.visible === false ? 'border-red-200 bg-gray-50 opacity-75' : 'border-gray-100 hover:shadow-md'}`">

                    <div v-if="pub.visible === false" class="absolute top-0 left-0 right-0 bg-red-100 text-red-600 text-xs font-bold px-4 py-1 text-center border-b border-red-200 z-10">
                        ESTA PUBLICAÇÃO ESTÁ OCULTA (Desaparecerá ao atualizar)
                    </div>

                    <div class="flex gap-5 w-full mt-4 md:mt-0">
                        <div class="shrink-0 hidden sm:block pt-1">
                            <div class="h-14 w-14 bg-indigo-50 text-indigo-600 rounded-xl flex items-center justify-center shadow-sm">
                                <svg class="w-7 h-7" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M12 6.253v13m0-13C10.832 5.477 9.246 5 7.5 5S4.168 5.477 3 6.253v13C4.168 18.477 5.754 18 7.5 18s3.332.477 4.5 1.253m0-13C13.168 5.477 14.754 5 16.5 5c1.747 0 3.332.477 4.5 1.253v13C19.832 18.477 18.247 18 16.5 18c-1.746 0-3.332.477-4.5 1.253"/></svg>
                            </div>
                        </div>
                        <div class="flex-grow">
                            <div class="flex flex-wrap gap-2 mb-2">
                                <span class="bg-blue-50 text-blue-700 px-2 py-0.5 rounded text-xs font-bold uppercase">{{pub.scientificArea }}</span>
                                <span class="bg-gray-100 text-gray-600 px-2 py-0.5 rounded text-xs font-bold uppercase">{{ typeTranslations[pub.type] || pub.type }}</span>
                            </div>
                            <NuxtLink :to="`/publicacoes/${pub.id}`" class="text-xl font-bold text-gray-900 hover:text-indigo-600 transition block mb-1">{{ pub.title }}</NuxtLink>
                            <p class="text-sm text-gray-600 mb-3"><span class="font-semibold text-gray-800">Autores:</span> {{ Array.isArray(pub.authors) ? pub.authors.join(', ') : (pub.authors || 'N/A') }}</p>
                            <div v-if="pub.tags && pub.tags.length > 0" class="flex flex-wrap gap-2 mb-3">
                                <div v-for="tag in pub.tags" :key="tag" class="flex items-center gap-1 px-2 py-1 bg-indigo-50 text-indigo-700 text-xs rounded-md border border-indigo-100 font-medium group/tag">
                                    <span>#{{ tag }}</span>
                                    <button v-if="canManageTags" @click.stop="requestRemoveTag(pub, tag)" class="ml-1 text-indigo-400 hover:text-red-600 hover:bg-red-100 rounded-full p-0.5 transition"><svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/></svg></button>
                                </div>
                            </div>
                            <div class="pt-3 border-t border-gray-50 flex items-center justify-between">
                                <PublicationStats :average="pub.averageRating || 0"/>
                                <div class="text-xs text-gray-400 font-medium">@{{ pub.uploaderUsername }}</div>
                            </div>
                        </div>
                    </div>

                    <div class="flex flex-col gap-2 shrink-0 border-l border-gray-50 pl-4 ml-2 mt-4 md:mt-0">
                        <button @click="downloadPublication(pub.id)" class="text-gray-400 hover:text-indigo-600 hover:bg-indigo-50 p-2 rounded-lg transition" title="Download">
                            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 16v1a3 3 0 003 3h10a3 3 0 003-3v-1m-4-4l-4-4m0 0l-4 4m4-4v12"/></svg>
                        </button>
                        <template v-if="authStore.user?.role === 'ADMINISTRATOR' || authStore.user?.role === 'RESPONSIBLE' || authStore.user?.username === pub.uploaderUsername">
                            <NuxtLink :to="`/publicacoes/editar/${pub.id}`" class="text-gray-400 hover:text-orange-500 hover:bg-orange-50 p-2 rounded-lg transition" title="Editar">
                                <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"/></svg>
                            </NuxtLink>
                            <button @click="requestToggleVisibility(pub)" :class="`p-2 rounded-lg transition ${pub.visible === false ? 'text-green-600 hover:bg-green-50' : 'text-gray-400 hover:text-amber-500 hover:bg-amber-50'}`" :title="pub.visible === false ? 'Tornar Visível' : 'Ocultar Publicação'">
                                <Eye v-if="pub.visible === false" :size="20"/>
                                <EyeOff v-else :size="20"/>
                            </button>
                        </template>
                    </div>
                </div>
            </div>

            <div v-if="isVisibilityModalOpen" class="fixed inset-0 bg-gray-900/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                <div class="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm w-full">

                    <div class="w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4" :class="publicationToToggle?.visible === false ? 'bg-green-100 text-green-600' : 'bg-amber-100 text-amber-600'">
                        <Eye v-if="publicationToToggle?.visible === false" :size="32"/> <EyeOff v-else :size="32"/>
                    </div>

                    <h3 class="text-xl font-bold text-gray-900 mb-2">Alterar Visibilidade?</h3>

                    <div v-if="publicationToToggle?.visible !== false" class="text-left mb-4">
                        <p class="text-gray-500 mb-3 text-center text-sm">A publicação deixará de ser visível para outros utilizadores.</p>
                        <label class="block text-xs font-bold text-gray-700 mb-1 ml-1">Motivo da Ocultação (Obrigatório):</label>
                        <textarea
                            v-model="hideReason"
                            class="w-full border border-gray-300 rounded-lg p-3 text-sm focus:ring-2 focus:ring-amber-500 outline-none transition bg-gray-50"
                            placeholder="Ex: Conteúdo duplicado, inapropriado..."
                            rows="3"
                        ></textarea>
                    </div>

                    <div v-else class="mb-6">
                        <p class="text-gray-500">Deseja tornar esta publicação <strong>visível</strong> novamente?</p>
                    </div>

                    <div class="flex flex-col gap-3">
                        <button @click="confirmToggleVisibility"
                                :disabled="publicationToToggle?.visible !== false && !hideReason"
                                :class="`w-full py-3 text-white rounded-xl font-bold shadow-lg transition flex items-center justify-center gap-2 ${publicationToToggle?.visible === false ? 'bg-green-600 hover:bg-green-700' : 'bg-amber-500 hover:bg-amber-600 disabled:opacity-50 disabled:cursor-not-allowed'}`">
                            {{ publicationToToggle?.visible === false ? 'Sim, Mostrar' : 'Sim, Ocultar' }}
                        </button>
                        <button @click="isVisibilityModalOpen=false" class="w-full py-3 bg-white text-gray-700 border border-gray-200 rounded-xl font-medium hover:bg-gray-50 transition">Cancelar</button>
                    </div>
                </div>
            </div>

            <div v-if="isRemoveTagModalOpen" class="fixed inset-0 bg-gray-900/60 z-50 flex items-center justify-center p-4 backdrop-blur-sm">
                <div class="bg-white rounded-2xl shadow-2xl p-8 text-center max-w-sm">
                    <h3 class="text-xl font-bold text-gray-900 mb-2">Desassociar Tag?</h3>
                    <p class="text-gray-500 mb-6">Remover tag <span class="font-bold text-indigo-600">#{{ tagToRemoveData?.tag }}</span>?</p>
                    <div class="flex flex-col gap-3">
                        <button @click="confirmRemoveTag" class="w-full py-3 bg-indigo-600 text-white rounded-xl font-bold shadow-lg">Sim, Remover</button>
                        <button @click="isRemoveTagModalOpen=false" class="w-full py-3 bg-white text-gray-700 border border-gray-200 rounded-xl font-medium">Cancelar</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
