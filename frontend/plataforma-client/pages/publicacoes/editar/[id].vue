<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';
import { useTagsStore } from '~/stores/tags-store.js';
import { Save, Plus, X, ArrowLeft, FileText, Tag as TagIcon, Loader2 } from 'lucide-vue-next';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const tagStore = useTagsStore();
const config = useRuntimeConfig();
const api = config.public.apiBase;

const id = route.params.id;
const isLoading = ref(true);
const isSaving = ref(false);

// Estado Local da Publicação
const publication = ref({
    title: '',
    authors: [],
    scientificArea: '',
    type: '',
    description: '',
    tags: [] // Lista de Strings (nomes das tags)
});

// --- INICIALIZAÇÃO ---
onMounted(async () => {
    isLoading.value = true;
    try {
        if (!authStore.token) return router.push('/auth/login');

        // 1. Carregar Tags do Sistema
        await tagStore.fetchTags();

        // 2. Carregar a Publicação
        const data = await $fetch(`${api}/publicacoes/${id}`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });

        // 3. Preencher Estado Local
        publication.value = {
            id: data.id,
            title: data.title || "",
            authors: data.authors || [],
            scientificArea: data.scientificArea || "",
            type: data.type || "Article",
            description: data.description || "",
            tags: data.tags || []
        };

        // 4. Limpeza de Segurança (Evita erro 500)
        // Removemos tags que a publicação tem mas que já não existem no sistema de tags
        const validTagNames = new Set(tagStore.tags.map(t => t.name));
        publication.value.tags = publication.value.tags.filter(t => validTagNames.has(t));

    } catch (e) {
        console.error(e);
        alert("Erro ao carregar dados.");
        router.push('/publicacoes');
    } finally {
        isLoading.value = false;
    }
});

// Tags disponíveis para adicionar (exclui as que já tem)
const availableTags = computed(() => {
    const currentTagNames = new Set(publication.value.tags);
    return tagStore.tags.filter(t => !currentTagNames.has(t.name));
});

// --- FUNÇÃO MESTRA: GUARDAR E GERAR HISTÓRICO ---
// Usa sempre o PUT para obrigar o Backend a criar o PublicationHistory
async function saveAllChanges(message = "Alterações guardadas!") {
    isSaving.value = true;

    try {
        // Preparar Payload Limpo
        const payload = {
            title: publication.value.title,
            description: publication.value.description,
            scientificArea: publication.value.scientificArea,
            type: publication.value.type,
            authors: publication.value.authors,
            tags: [...publication.value.tags] // Envia lista de strings atualizada
        };

        // Chama o endpoint PUT geral
        await $fetch(`${api}/publicacoes/${id}`, {
            method: 'PUT',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: payload
        });

        // Sucesso
        // Não precisamos de alertar sempre, depende da ação
        if (message) alert(message);

        // Recarregar dados para garantir sincronia (opcional mas recomendado)
        // Isto garante que temos os dados exatos da BD
        /* const refreshed = await $fetch(`${api}/publicacoes/${id}`, {
             headers: { Authorization: `Bearer ${authStore.token}` }
        });
        publication.value.tags = refreshed.tags || [];
        */

    } catch (e) {
        console.error("Erro ao guardar:", e);
        const msg = e.response?._data ? e.response._data : "Erro interno (500).";
        alert("Erro: " + msg);
    } finally {
        isSaving.value = false;
    }
}

// --- AÇÕES LOCAIS (Chamam o Save imediatamente) ---

// 1. Alterar Descrição (Botão Guardar)
function handleSaveDescription() {
    saveAllChanges("Descrição atualizada e histórico gerado!");
}

// 2. Adicionar Tag
function addTagLocal(tagName) {
    publication.value.tags.push(tagName);
    // Guarda imediatamente para criar o histórico desta ação
    saveAllChanges(null); // null para não mostrar alerta intrusivo
}

// 3. Remover Tag
function removeTagLocal(tagName) {
    if(!confirm(`Remover a tag #${tagName}?`)) return;

    publication.value.tags = publication.value.tags.filter(t => t !== tagName);
    // Guarda imediatamente
    saveAllChanges(null);
}

</script>

<template>
    <div class="max-w-5xl mx-auto py-10 px-4 font-sans text-slate-800 mb-20">

        <div class="mb-8 flex items-center gap-4">
            <button @click="router.back()" class="p-2 rounded-full hover:bg-gray-100 transition text-gray-500">
                <ArrowLeft :size="24"/>
            </button>
            <div>
                <h1 class="text-3xl font-extrabold text-slate-900">Gerir Publicação</h1>
                <p class="text-sm text-gray-500">Qualquer alteração aqui gera um registo no histórico.</p>
            </div>
        </div>

        <div v-if="isLoading" class="flex justify-center py-20">
            <Loader2 class="animate-spin text-indigo-600" :size="48"/>
        </div>

        <div v-else class="grid grid-cols-1 lg:grid-cols-3 gap-8">

            <div class="lg:col-span-1 space-y-6">
                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 sticky top-6">
                    <h3 class="text-xs font-bold text-gray-400 uppercase tracking-wider mb-4">Informação Geral</h3>

                    <div class="mb-4">
                        <label class="block text-xs font-bold text-gray-500 uppercase mb-1">Título</label>
                        <p class="text-gray-900 font-bold leading-tight">{{ publication.title }}</p>
                    </div>

                    <div class="mb-4">
                        <label class="block text-xs font-bold text-gray-500 uppercase mb-1">Autores</label>
                        <p class="text-gray-600 text-sm">
                            {{ Array.isArray(publication.authors) ? publication.authors.join(', ') : publication.authors }}
                        </p>
                    </div>

                    <div class="grid grid-cols-2 gap-4 pt-2 border-t border-gray-50">
                        <div><span class="block text-xs text-gray-400">Área</span><span class="font-medium text-sm text-indigo-600">{{ publication.scientificArea }}</span></div>
                        <div><span class="block text-xs text-gray-400">Tipo</span><span class="font-medium text-sm text-gray-700">{{ publication.type }}</span></div>
                    </div>
                </div>
            </div>

            <div class="lg:col-span-2 space-y-8">

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 relative">
                    <div v-if="isSaving" class="absolute inset-0 bg-white/60 z-10 flex items-center justify-center rounded-xl cursor-wait"></div>

                    <div class="flex items-center gap-2 mb-4 pb-2 border-b border-gray-50 text-indigo-700">
                        <FileText :size="20"/>
                        <h2 class="text-lg font-bold">Descrição (IA)</h2>
                    </div>

                    <textarea
                        v-model="publication.description"
                        rows="10"
                        class="w-full border border-gray-300 rounded-lg p-4 text-sm leading-relaxed focus:ring-2 focus:ring-indigo-500 outline-none transition bg-gray-50 focus:bg-white resize-y"
                        placeholder="Resumo da publicação..."
                    ></textarea>

                    <div class="mt-4 flex justify-end">
                        <button
                            @click="handleSaveDescription"
                            :disabled="isSaving"
                            class="flex items-center gap-2 px-6 py-2.5 bg-indigo-600 text-white rounded-lg font-bold hover:bg-indigo-700 shadow-md transition disabled:opacity-50"
                        >
                            <Loader2 v-if="isSaving" class="animate-spin" :size="18"/>
                            <Save v-else :size="18"/>
                            Guardar Descrição
                        </button>
                    </div>
                </div>

                <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-100 relative">
                    <div v-if="isSaving" class="absolute inset-0 bg-white/60 z-10 flex items-center justify-center rounded-xl cursor-wait"></div>

                    <div class="flex items-center gap-2 mb-4 pb-2 border-b border-gray-50 text-indigo-700">
                        <TagIcon :size="20"/>
                        <h2 class="text-lg font-bold">Gerir Tags</h2>
                    </div>

                    <div class="mb-6">
                        <label class="block text-xs font-bold text-gray-500 mb-2 uppercase">Tags Associadas</label>

                        <div v-if="publication.tags.length === 0" class="text-sm text-gray-400 italic p-4 bg-gray-50 rounded-lg text-center">
                            Sem tags associadas.
                        </div>

                        <div class="flex flex-wrap gap-2">
                            <span v-for="tag in publication.tags" :key="tag" class="flex items-center gap-1 bg-indigo-50 text-indigo-700 px-3 py-1.5 rounded-full text-xs font-bold border border-indigo-100 shadow-sm animate-fade-in">
                                #{{ tag }}
                                <button @click="removeTagLocal(tag)" class="ml-1 p-0.5 hover:bg-red-100 hover:text-red-600 rounded-full text-indigo-400 transition" title="Remover"><X :size="14"/></button>
                            </span>
                        </div>
                    </div>

                    <div class="pt-4 border-t border-gray-100">
                        <label class="block text-xs font-bold text-gray-500 mb-2 uppercase">Adicionar Tag Disponível</label>

                        <div v-if="availableTags.length === 0" class="text-sm text-gray-400 italic">
                            Todas as tags já foram adicionadas.
                        </div>

                        <div v-else class="flex flex-wrap gap-2 max-h-48 overflow-y-auto custom-scrollbar p-1">
                            <button
                                v-for="tag in availableTags"
                                :key="tag.id"
                                @click="addTagLocal(tag.name)"
                                class="flex items-center gap-1 bg-white text-gray-600 border border-gray-300 px-3 py-1.5 rounded-full text-xs font-bold hover:border-green-500 hover:text-green-700 hover:bg-green-50 transition shadow-sm"
                            >
                                <Plus :size="14"/> {{ tag.name }}
                            </button>
                        </div>
                    </div>
                </div>

            </div>
        </div>
    </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar { width: 6px; }
.custom-scrollbar::-webkit-scrollbar-track { background: #f9fafb; }
.custom-scrollbar::-webkit-scrollbar-thumb { background: #d1d5db; border-radius: 10px; }
.custom-scrollbar::-webkit-scrollbar-thumb:hover { background: #9ca3af; }
.animate-fade-in { animation: fadeIn 0.3s ease-out; }
@keyframes fadeIn { from { opacity: 0; transform: scale(0.9); } to { opacity: 1; transform: scale(1); } }
</style>
