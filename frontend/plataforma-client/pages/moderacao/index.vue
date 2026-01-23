<script setup lang="ts">
import {ref, computed, onMounted} from 'vue';
import {useAuthStore} from '~/stores/auth-store.js';
import {
    ShieldAlert, Eye, CheckCircle, AlertTriangle, Loader2, MessageSquare, RotateCcw, FileText, Tag
} from "lucide-vue-next";

const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;
const router = useRouter();

// --- ESTADO ---
const isLoading = ref(true);
const processingId = ref<number | null>(null);
const hiddenItems = ref([]);

// Permissões
const canModerate = computed(() => {
    const role = authStore.user?.role;
    return role === 'ADMINISTRATOR' || role === 'RESPONSIBLE';
});

// --- CRAWLER DIRETO (Backend Updated) ---
async function fetchHiddenContent() {
    if (!canModerate.value) return router.push('/');

    isLoading.value = true;
    hiddenItems.value = [];

    try {
        // 1. Buscar Comentários Ocultos (Endpoint Global no PublicationService)
        const comments = await $fetch(`${api}/publicacoes/comentarios-ocultos-globais`, {
            headers: {Authorization: `Bearer ${authStore.token}`}
        }).catch(() => []);

        // 2. Buscar Publicações Ocultas
        const pubs = await $fetch(`${api}/publicacoes/ocultas`, {
            headers: {Authorization: `Bearer ${authStore.token}`}
        }).catch(() => []);

        // 3. Buscar Tags Ocultas
        const tags = await $fetch(`${api}/tags/ocultas`, {
            headers: {Authorization: `Bearer ${authStore.token}`}
        }).catch(() => []);

        // --- MAPEAR PARA FORMATO VISUAL ---

        const mappedComments = comments.map(c => ({
            id: c.id,
            type: 'COMMENT',
            content: c.content,
            date: c.date,
            author: c.authorUsername || "Desconhecido",
            hideReason: c.hideReason || "N/A",
            pubId: c.publicationId, // IMPORTANTE: O DTO tem de ter isto!
            title: "Comentário Oculto"
        }));

        const mappedPubs = pubs.map(p => ({
            id: p.id,
            type: 'PUBLICATION',
            content: p.description || "(Sem descrição)",
            date: p.uploadDate, // Confirma se o nome do campo é este no DTO
            author: p.uploaderUsername || "Desconhecido",
            hideReason: p.hideReason || "N/A",
            pubId: p.id,
            title: p.title
        }));

        const mappedTags = tags.map(t => ({
            id: t.id,
            type: 'TAG',
            content: t.description || "Tag",
            date: "N/A",
            author: "Sistema",
            hideReason: "Oculta", // Tags geralmente não têm motivo, ou têm?
            pubId: null,
            title: `#${t.name}`
        }));

        // Juntar tudo
        hiddenItems.value = [...mappedComments, ...mappedPubs, ...mappedTags];

    } catch (e) {
        console.error("Erro:", e);
    } finally {
        isLoading.value = false;
    }
}

// --- DESOCULTAR ---
async function unhideItem(item) {
    if (!confirm("Tornar visível?")) return;
    processingId.value = item.id;

    try {
        if (item.type === 'COMMENT') {
            await $fetch(`${api}/publicacoes/${item.pubId}/comentarios/${item.id}/visibilidade`, {
                method: 'PUT',
                headers: {Authorization: `Bearer ${authStore.token}`},
                body: {hidden: false, hideReason: null}
            });
        } else if (item.type === 'PUBLICATION') {
            // Se tiveres endpoint para desocultar pub, usa aqui
            // await $fetch(...)
            alert("Falta implementar desocultar Publicação no backend!");
        } else if (item.type === 'TAG') {
            // Idem para tags
        }

        // Remove da lista
        hiddenItems.value = hiddenItems.value.filter(i => i.id !== item.id);

    } catch (e) {
        alert("Erro ao restaurar.");
    } finally {
        processingId.value = null;
    }
}

onMounted(() => {
    fetchHiddenContent();
});
</script>

<template>
    <div class="min-h-screen bg-gray-50 p-6 lg:p-10 font-sans text-slate-800">
        <div class="max-w-5xl mx-auto">
            <div class="flex items-center justify-between mb-8">
                <div class="flex items-center gap-3">
                    <div class="p-3 bg-red-100 text-red-600 rounded-lg">
                        <ShieldAlert :size="32"/>
                    </div>
                    <div>
                        <h1 class="text-3xl font-extrabold text-slate-900">Moderação</h1>
                    </div>
                </div>
                <button @click="fetchHiddenContent"
                        class="text-indigo-600 font-bold hover:bg-indigo-50 px-4 py-2 rounded transition flex items-center gap-2">
                    <RotateCcw :size="16"/>
                    Atualizar
                </button>
            </div>

            <div v-if="!canModerate" class="bg-red-50 p-6 rounded text-red-700 font-bold text-center">Acesso Negado.
            </div>

            <div v-else>
                <div v-if="isLoading" class="text-center py-20 bg-white rounded-xl shadow-sm border border-gray-100">
                    <Loader2 class="animate-spin mx-auto text-indigo-600 mb-4" :size="32"/>
                    <p class="text-gray-600 font-medium">A carregar...</p></div>

                <div v-else-if="hiddenItems.length === 0"
                     class="text-center py-24 bg-white rounded-xl border border-dashed border-gray-300">
                    <CheckCircle class="mx-auto text-green-500 mb-4" :size="48"/>
                    <h3 class="text-xl font-bold text-gray-800">Tudo Limpo!</h3></div>

                <div v-else class="space-y-4">
                    <div class="flex items-center gap-2 text-gray-700 font-bold px-1 pb-2 border-b border-gray-200">
                        <span>Itens Ocultos ({{ hiddenItems.length }})</span></div>

                    <div v-for="item in hiddenItems" :key="item.type + item.id"
                         class="bg-white rounded-xl border-l-4 border-red-500 shadow-sm p-6 relative group hover:shadow-md transition">

                        <div class="flex flex-col md:flex-row justify-between items-start mb-4 gap-4">
                            <div>
                                <div class="flex items-center gap-2 mb-1">
                                    <span v-if="item.type === 'COMMENT'"
                                          class="bg-blue-100 text-blue-700 text-xs px-2 py-0.5 rounded font-bold uppercase flex items-center gap-1"><MessageSquare
                                        :size="12"/> Comentário</span>
                                    <span v-else-if="item.type === 'PUBLICATION'"
                                          class="bg-purple-100 text-purple-700 text-xs px-2 py-0.5 rounded font-bold uppercase flex items-center gap-1"><FileText
                                        :size="12"/> Publicação</span>
                                    <span v-else
                                          class="bg-gray-100 text-gray-700 text-xs px-2 py-0.5 rounded font-bold uppercase flex items-center gap-1"><Tag
                                        :size="12"/> Tag</span>
                                    <span class="text-gray-400 text-xs">• {{ item.date }}</span>
                                </div>
                                <div class="text-sm text-gray-800 font-bold">{{ item.title }}</div>
                                <div class="text-xs text-gray-500">Autor: <span class="text-indigo-600">@{{
                                        item.author
                                    }}</span></div>
                            </div>
                            <button @click="unhideItem(item)" :disabled="processingId === item.id"
                                    class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 text-gray-700 font-bold rounded-lg hover:bg-green-50 hover:text-green-700 transition disabled:opacity-50 text-xs shadow-sm">
                                <Loader2 v-if="processingId === item.id" class="animate-spin" :size="14"/>
                                <Eye v-else :size="16"/>
                                Restaurar
                            </button>
                        </div>

                        <div class="bg-gray-50 p-4 rounded border border-gray-200 text-gray-700 italic mb-4">
                            "{{ item.content }}"
                        </div>
                        <div
                            class="flex flex-wrap gap-4 text-xs text-gray-500 bg-white p-2 rounded border border-gray-100">
                            <span class="flex items-center gap-1 text-red-600"><strong>Motivo:</strong> {{
                                    item.hideReason
                                }}</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
