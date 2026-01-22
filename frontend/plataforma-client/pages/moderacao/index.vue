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

// --- API: PROCURAR CONTEÚDO OCULTO ---
async function fetchHiddenContent() {
    if (!canModerate.value) {
        router.push('/');
        return;
    }

    isLoading.value = true;
    hiddenComments.value = [];

    try {
        loadingProgress.value = "A obter lista de publicações...";

        // 1. Obter TODAS as publicações (usa o endpoint de listagem geral)
        // Nota: O endpoint pode exigir paginação no futuro, mas assumimos que devolve tudo ou a 1ª página
        const publications = await $fetch(`${api}/publicacoes`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        if (!Array.isArray(publications)) throw new Error("Formato de publicações inválido");

        loadingProgress.value = `A analisar ${publications.length} publicações para encontrar conteúdo oculto...`;

        // 2. Para cada publicação, obter os comentários
        const promises = publications.map(async (pub) => {
            try {
                const comments = await $fetch(`${api}/publicacoes/${pub.id}/comentarios`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });

                if (!Array.isArray(comments)) return [];

                // 3. Filtrar APENAS os ocultos (hidden === true)
                return comments
                    .filter(c => c.hidden === true)
                    .map(c => ({
                        ...c,
                        pubTitle: pub.title, // Adiciona o título da publicação para contexto
                        pubId: pub.id
                    }));
            } catch (e) {
                // Ignora erros individuais (ex: publicação apagada entretanto)
                return [];
            }
        });

        // Espera por todos os pedidos
        const results = await Promise.all(promises);

        // Junta tudo numa única lista plana
        hiddenComments.value = results.flat();

    } catch (e) {
        console.error("Erro no crawler de moderação:", e);
    } finally {
        isLoading.value = false;
    }
}

// --- AÇÃO: DESOCULTAR (Tornar Visível) ---
async function unhideComment(comment) {
    if (!confirm("Tem a certeza? O comentário ficará visível para todos.")) return;

    processingId.value = comment.id;

    try {
        // Endpoint: PUT /publicacoes/{pubId}/comentarios/{commentId}/visibilidade
        await $fetch(`${api}/publicacoes/${comment.pubId}/comentarios/${comment.id}/visibilidade`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: {
                hidden: false, // Define como visível
                hideReason: null
            }
        });

        // Remove da lista localmente para dar feedback imediato
        hiddenComments.value = hiddenComments.value.filter(c => c.id !== comment.id);

    } catch (e) {
        alert("Erro ao alterar a visibilidade. Tente novamente.");
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
                    <div class="p-3 bg-red-100 text-red-600 rounded-lg shadow-sm">
                        <ShieldAlert :size="32" />
                    </div>
                    <div>
                        <h1 class="text-3xl font-extrabold text-slate-900">Painel de Moderação</h1>
                        <p class="text-slate-500 text-sm">Gestão de conteúdo oculto na plataforma.</p>
                    </div>
                </div>
            </div>

            <div v-if="!canModerate" class="bg-red-50 p-6 rounded-xl border border-red-200 text-center text-red-700 font-bold">
                <AlertTriangle class="mx-auto mb-2" :size="32"/>
                Acesso negado. Apenas Moderadores.
            </div>

            <div v-else>
                <div v-if="isLoading" class="text-center py-20 bg-white rounded-xl shadow-sm border border-gray-100">
                    <Loader2 class="animate-spin mx-auto text-indigo-600 mb-4" :size="40"/>
                    <h3 class="text-lg font-bold text-gray-700">A procurar conteúdo oculto...</h3>
                    <p class="text-sm text-gray-400 mt-1">{{ loadingProgress }}</p>
                </div>

                <div v-else-if="hiddenComments.length === 0" class="text-center py-24 bg-white rounded-xl shadow-sm border border-dashed border-gray-300">
                    <CheckCircle class="mx-auto text-green-500 mb-4" :size="48"/>
                    <h3 class="text-xl font-bold text-gray-800">Tudo Limpo!</h3>
                    <p class="text-gray-500 mt-2">Não existem comentários ocultos no momento.</p>
                    <button @click="fetchHiddenContent" class="mt-6 text-indigo-600 font-bold hover:bg-indigo-50 px-4 py-2 rounded transition flex items-center gap-2 mx-auto">
                        <RotateCcw :size="16"/> Verificar Novamente
                    </button>
                </div>

                <div v-else class="space-y-6">
                    <div class="flex justify-between items-center px-1 border-b pb-2">
                        <h2 class="text-lg font-bold text-gray-700 flex items-center gap-2">
                            <MessageSquare :size="20" class="text-gray-400"/>
                            Comentários Ocultos
                            <span class="bg-red-600 text-white text-xs px-2 py-0.5 rounded-full font-bold">{{ hiddenComments.length }}</span>
                        </h2>
                        <button @click="fetchHiddenContent" class="text-sm text-indigo-600 hover:underline flex items-center gap-1">
                            <RotateCcw :size="14"/> Atualizar Lista
                        </button>
                    </div>

                    <div v-for="item in hiddenComments" :key="item.id"
                         class="bg-white rounded-xl border-l-4 border-red-500 shadow-sm p-6 relative overflow-hidden group hover:shadow-md transition">

                        <div class="flex flex-col md:flex-row justify-between items-start mb-4 gap-4">
                            <div>
                                <div class="text-xs font-bold text-gray-400 uppercase mb-1 flex items-center gap-1">
                                    <FileText :size="12"/> Publicação: <span class="text-indigo-600">{{ item.pubTitle }}</span>
                                </div>
                                <div class="flex items-center gap-2 text-sm">
                                    <span class="font-bold text-gray-800">Autor: @{{ item.authorUsername || 'Desconhecido' }}</span>
                                    <span class="text-gray-300">|</span>
                                    <span class="text-gray-500">{{ item.date }}</span>
                                </div>
                            </div>

                            <button
                                @click="unhideComment(item)"
                                :disabled="processingId === item.id"
                                class="flex items-center gap-2 px-4 py-2 bg-white border border-gray-300 text-gray-700 font-bold rounded-lg hover:bg-green-50 hover:text-green-700 hover:border-green-200 transition disabled:opacity-50 text-sm shadow-sm"
                            >
                                <Loader2 v-if="processingId === item.id" class="animate-spin" :size="16"/>
                                <Eye v-else :size="18"/>
                                Tornar Visível
                            </button>
                        </div>

                        <div class="bg-gray-50 p-4 rounded-lg border border-gray-200 text-gray-700 italic mb-4 relative">
                            <div class="absolute -top-2 left-4 bg-red-100 text-red-700 text-[10px] font-bold px-2 py-0.5 rounded uppercase tracking-wide border border-red-200">
                                Conteúdo Ocultado
                            </div>
                            "{{ item.content }}"
                        </div>

                        <div class="bg-red-50 p-3 rounded border border-red-100 text-sm text-red-800 flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-6">
                            <span class="flex items-center gap-2">
                                <AlertTriangle :size="16"/>
                                <strong>Motivo:</strong> {{ item.hideReason || "Motivo não especificado" }}
                            </span>
                            <span v-if="item.hiddenBy" class="text-red-600/80 text-xs">
                                (Moderado por: @{{ item.hiddenBy }})
                            </span>
                        </div>

                    </div>
                </div>
            </div>

        </div>
    </div>
</template>
