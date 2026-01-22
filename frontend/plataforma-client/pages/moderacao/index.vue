<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import {
    ShieldAlert, Eye, CheckCircle, AlertTriangle, Loader2, FileText, MessageSquare, RotateCcw
} from "lucide-vue-next";

const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;
const router = useRouter();

// --- ESTADO ---
const isLoading = ref(true);
const processingId = ref<number | null>(null);
const hiddenComments = ref([]);
const loadingProgress = ref("");

// Permissões
const canModerate = computed(() => {
    const role = authStore.user?.role;
    return role === 'ADMINISTRATOR' || role === 'RESPONSIBLE';
});

// --- CRAWLER DE DADOS (Frontend Only) ---
async function fetchHiddenContent() {
    if (!canModerate.value) {
        router.push('/');
        return;
    }

    isLoading.value = true;
    hiddenComments.value = [];

    try {
        loadingProgress.value = "A obter lista de publicações...";

        // 1. Buscar todas as publicações
        const publications = await $fetch(`${api}/publicacoes`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        if (!Array.isArray(publications)) throw new Error("Erro ao listar publicações");

        loadingProgress.value = `A verificar ${publications.length} publicações...`;

        // 2. Buscar comentários de cada uma
        const promises = publications.map(async (pub) => {
            try {
                const comments = await $fetch(`${api}/publicacoes/${pub.id}/comentarios`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });

                if (!Array.isArray(comments)) return [];

                // 3. FILTRAR O QUE ESTÁ NO BACKEND (hidden = true)
                return comments
                    .filter(c => c.hidden === true) // O teu DTO envia boolean, isto deve funcionar
                    .map(c => ({
                        id: c.id,
                        content: c.content,
                        date: c.date,
                        // DADOS QUE FALTAM NO TEU DTO (Preenchemos com valores padrão)
                        author: "Desconhecido (Limitação Backend)",
                        hideReason: "Não registado no DTO",
                        hiddenBy: "N/A",
                        // Contexto da publicação
                        pubTitle: pub.title,
                        pubId: pub.id
                    }));
            } catch (e) {
                return [];
            }
        });

        const results = await Promise.all(promises);
        hiddenComments.value = results.flat();

        console.log("Resultados Ocultos Encontrados:", hiddenComments.value);

    } catch (e) {
        console.error("Erro moderação:", e);
    } finally {
        isLoading.value = false;
    }
}

// --- DESOCULTAR ---
async function unhideComment(comment) {
    if (!confirm("Tornar visível?")) return;
    processingId.value = comment.id;

    try {
        await $fetch(`${api}/publicacoes/${comment.pubId}/comentarios/${comment.id}/visibilidade`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: {
                hidden: false,
                hideReason: null
            }
        });
        // Remove da lista local
        hiddenComments.value = hiddenComments.value.filter(c => c.id !== comment.id);
    } catch (e) {
        alert("Erro ao alterar visibilidade.");
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
                        <ShieldAlert :size="32" />
                    </div>
                    <div>
                        <h1 class="text-3xl font-extrabold text-slate-900">Moderação</h1>
                        <p class="text-slate-500 text-sm">Gestão de conteúdo oculto.</p>
                    </div>
                </div>
                <button @click="fetchHiddenContent" class="text-indigo-600 font-bold hover:bg-indigo-50 px-4 py-2 rounded transition flex items-center gap-2">
                    <RotateCcw :size="16"/> Atualizar
                </button>
            </div>

            <div v-if="!canModerate" class="bg-red-50 p-6 rounded text-red-700 font-bold text-center">
                Acesso Negado.
            </div>

            <div v-else>
                <div v-if="isLoading" class="text-center py-20 bg-white rounded-xl shadow-sm border border-gray-100">
                    <Loader2 class="animate-spin mx-auto text-indigo-600 mb-4" :size="32"/>
                    <p class="text-gray-600">{{ loadingProgress }}</p>
                </div>

                <div v-else-if="hiddenComments.length === 0" class="text-center py-20 bg-white rounded-xl border border-dashed border-gray-300">
                    <CheckCircle class="mx-auto text-green-500 mb-4" :size="48"/>
                    <h3 class="text-xl font-bold text-gray-800">Tudo Limpo!</h3>
                    <p class="text-gray-500 mt-2">Nenhum comentário oculto encontrado.</p>
                    <p class="text-xs text-gray-400 mt-4">(Nota: Se tem a certeza que existem, verifique se o endpoint 'getComments' do backend devolve dados com hidden=true)</p>
                </div>

                <div v-else class="space-y-4">
                    <div class="flex items-center gap-2 text-gray-700 font-bold px-1">
                        <MessageSquare :size="20"/> Comentários Ocultos ({{ hiddenComments.length }})
                    </div>

                    <div v-for="item in hiddenComments" :key="item.id"
                         class="bg-white rounded-xl border-l-4 border-red-500 shadow-sm p-6 relative">

                        <div class="flex flex-col md:flex-row justify-between items-start mb-4 gap-4">
                            <div>
                                <div class="text-xs font-bold text-gray-400 uppercase mb-1">
                                    Na publicação: <span class="text-indigo-600">{{ item.pubTitle }}</span>
                                </div>
                                <div class="text-sm text-gray-500">
                                    Data: {{ item.date }}
                                </div>
                            </div>

                            <button
                                @click="unhideComment(item)"
                                :disabled="processingId === item.id"
                                class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 text-gray-700 font-bold rounded-lg hover:bg-green-50 hover:text-green-700 transition disabled:opacity-50 text-sm"
                            >
                                <Eye v-if="processingId !== item.id" :size="18"/>
                                <Loader2 v-else class="animate-spin" :size="18"/>
                                Tornar Visível
                            </button>
                        </div>

                        <div class="bg-gray-50 p-4 rounded border border-gray-200 text-gray-700 italic mb-4">
                            "{{ item.content }}"
                        </div>

                        <div class="text-xs text-gray-400 flex gap-4">
                            <span>Autor: {{ item.author }}</span>
                            <span>Motivo: {{ item.hideReason }}</span>
                        </div>

                    </div>
                </div>
            </div>
        </div>
    </div>
</template>
