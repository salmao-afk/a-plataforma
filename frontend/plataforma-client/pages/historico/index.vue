<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useAuthStore } from '~/stores/auth-store.js';
import {
    Search, Filter, Clock, ChevronDown, ChevronUp,
    FileText, MessageSquare, Star, AlertCircle, ListFilter, User, RotateCcw, Tag, Users, History, Shield, EyeOff, Loader2
} from "lucide-vue-next";

const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;

// --- ESTADO ---
const filterUsername = ref("");
const filterText = ref("");
const filterType = ref("ALL");

const isLoading = ref(true);
const errorMsg = ref("");
const allActivities = ref([]);
const expandedIds = ref(new Set());
const loadingHistoryIds = ref(new Set());

// Identificar se é Admin
const isAdmin = computed(() => authStore.user?.role === 'ADMINISTRATOR');

// --- AUXILIARES DE DATA ---
function parseBackendDate(dateInput) {
    if (!dateInput) return new Date();

    // Se o Java enviar array [2023, 1, 23]
    if (Array.isArray(dateInput)) {
        return new Date(dateInput[0], dateInput[1] - 1, dateInput[2], dateInput[3]||0, dateInput[4]||0);
    }

    // Se o Java enviar String "2023-01-23" (LocalDate.toString())
    if (typeof dateInput === 'string') {
        const d = new Date(dateInput);
        if (!isNaN(d.getTime())) return d;
    }

    return new Date();
}

function formatDisplayDate(dateInput) {
    const date = parseBackendDate(dateInput);
    if (isNaN(date.getTime())) return "Data n/a";

    const day = String(date.getDate()).padStart(2, '0');
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const year = date.getFullYear();

    return `${day}/${month}/${year}`;
}

// --- EXPANDIR COM LAZY LOADING ---
async function toggleExpand(item) {
    const id = item.id;

    if (expandedIds.value.has(id)) {
        expandedIds.value.delete(id);
        return;
    }
    expandedIds.value.add(id);

    // Se for Publicação e não tiver histórico carregado
    if (item.type === 'PUBLICATION' && (!item.history || item.history.length === 0)) {
        loadingHistoryIds.value.add(id);

        try {
            const historyData = await $fetch(`${api}/publicacoes/${item.originalId}/historico`, {
                headers: { Authorization: `Bearer ${authStore.token}` }
            });

            // Atualizar lista
            if (Array.isArray(allActivities.value)) {
                for (const group of allActivities.value) {
                    if (group.publicacoes) {
                        const pub = group.publicacoes.find(p => p.id === item.originalId);
                        if (pub) {
                            pub.history = historyData;
                        }
                    }
                }
            }
        } catch (e) {
            console.warn("Sem histórico ou erro ao carregar.", e);
        } finally {
            loadingHistoryIds.value.delete(id);
        }
    }
}

// --- CARREGAR HISTÓRICO GLOBAL ---
async function loadHistory() {
    isLoading.value = true;
    errorMsg.value = "";
    allActivities.value = [];

    try {
        if (isAdmin.value) {
            const users = await $fetch(`${api}/utilizadores`, { headers: { Authorization: `Bearer ${authStore.token}` } });
            const promises = users.map(u =>
                $fetch(`${api}/utilizadores/${u.username}/atividade`, { headers: { Authorization: `Bearer ${authStore.token}` } })
                    .then(res => ({ ...res, username: u.username })).catch(() => null)
            );
            const results = await Promise.all(promises);
            allActivities.value = results.filter(r => r !== null);
        } else {
            const myActivity = await $fetch(`${api}/utilizadores/me/atividade`, { headers: { Authorization: `Bearer ${authStore.token}` } });
            if (myActivity) allActivities.value = [{ ...myActivity, username: authStore.user.username }];
        }
    } catch (e) {
        console.error(e);
        errorMsg.value = "Erro ao carregar o histórico.";
    } finally {
        isLoading.value = false;
    }
}

// --- TIMELINE COMPUTADA ---
const timeline = computed(() => {
    let items = [];
    const activities = allActivities.value || [];

    for (const activity of activities) {
        const author = activity.username || 'Desconhecido';

        if (activity.publicacoes?.length) {
            items.push(...activity.publicacoes.map(p => ({
                id: `pub-${p.id}`,
                originalId: p.id,
                author: author,
                type: 'PUBLICATION',
                rawDate: p.uploadDate,
                title: p.title || "Publicação",
                details: p.description,
                tags: p.tags || [],
                pubAuthors: p.authors || [],
                history: p.history || [],
                isHidden: p.visible === false
            })));
        }

        if (activity.comentarios?.length) {
            items.push(...activity.comentarios.map(c => ({
                id: `com-${c.id}`,
                originalId: c.id,
                author: author,
                type: 'COMMENT',
                rawDate: c.date,
                title: c.hidden ? "Comentário (Oculto)" : "Comentário",
                details: c.content,
                isHidden: c.hidden,
            })));
        }

        if (activity.ratings?.length) {
            items.push(...activity.ratings.map(r => ({
                id: `rate-${r.id || Math.random()}`,
                originalId: r.id,
                author: author,
                type: 'RATING',
                rawDate: r.date || r.createdAt,
                title: `Avaliação: ${r.value}/5`,
                details: `Na publicação: "${r.publicationTitle || '...'}"`,
                isHidden: false
            })));
        }
    }

    if (filterType.value !== 'ALL') items = items.filter(i => i.type === filterType.value);
    if (filterUsername.value.trim()) items = items.filter(i => i.author.toLowerCase().includes(filterUsername.value.toLowerCase()));
    if (filterText.value.trim()) {
        const term = filterText.value.toLowerCase();
        items = items.filter(i =>
            (i.title?.toLowerCase().includes(term)) ||
            (i.details?.toLowerCase().includes(term)) ||
            (i.tags && i.tags.some(t => t.toLowerCase().includes(term)))
        );
    }

    return items.sort((a, b) => parseBackendDate(b.rawDate).getTime() - parseBackendDate(a.rawDate).getTime());
});

onMounted(() => loadHistory());
</script>

<template>
    <div class="min-h-screen bg-gray-50 p-6 lg:p-10 font-sans text-slate-800">
        <div class="max-w-7xl mx-auto">

            <div class="flex flex-col md:flex-row justify-between items-start md:items-end mb-8 gap-4">
                <div>
                    <h1 class="text-3xl font-extrabold text-slate-900 mb-2 flex items-center gap-2">
                        <span v-if="isAdmin">Histórico Global</span>
                        <span v-else>O Meu Histórico</span>
                        <Shield v-if="isAdmin" class="text-indigo-600" :size="28"/>
                    </h1>
                    <p class="text-slate-500">
                        <span v-if="isAdmin">Monitorização de <span class="font-bold text-indigo-600">{{ allActivities.length }}</span> utilizadores.</span>
                        <span v-else>A sua atividade na plataforma.</span>
                    </p>
                </div>
                <button @click="loadHistory" class="text-indigo-600 font-bold hover:bg-indigo-50 px-4 py-2 rounded-lg transition text-sm flex items-center gap-2">
                    <RotateCcw :size="16"/> Atualizar
                </button>
            </div>

            <div class="bg-white p-6 rounded-xl shadow-sm border border-gray-200 mb-8">
                <div class="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-12 gap-4 items-end">
                    <div v-if="isAdmin" class="xl:col-span-3">
                        <label class="block text-sm font-bold text-gray-700 mb-1">Utilizador</label>
                        <div class="relative"><User class="absolute left-3 top-3 text-gray-400" :size="20"/><input v-model="filterUsername" type="text" placeholder="Ex: admin..." class="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none transition"/></div>
                    </div>
                    <div :class="isAdmin ? 'xl:col-span-5' : 'xl:col-span-8'">
                        <label class="block text-sm font-bold text-gray-700 mb-1">Conteúdo</label>
                        <div class="relative"><Search class="absolute left-3 top-3 text-gray-400" :size="20"/><input v-model="filterText" type="text" placeholder="Título, descrição, tags..." class="w-full pl-10 pr-4 py-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none transition"/></div>
                    </div>
                    <div class="xl:col-span-4">
                        <label class="block text-sm font-bold text-gray-700 mb-1">Tipo de Atividade</label>
                        <div class="relative"><ListFilter class="absolute left-3 top-3 text-gray-400" :size="18"/><select v-model="filterType" class="w-full pl-10 pr-10 py-2.5 border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none appearance-none cursor-pointer"><option value="ALL">Mostrar Tudo</option><option value="PUBLICATION">Publicações</option><option value="COMMENT">Comentários</option><option value="RATING">Avaliações</option></select><div class="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-500"><ChevronDown :size="16"/></div></div>
                    </div>
                </div>
            </div>

            <div v-if="isLoading" class="text-center py-20"><div class="text-indigo-600 font-bold text-lg animate-pulse mb-2">A carregar histórico...</div></div>
            <div v-else-if="errorMsg" class="text-center py-12 bg-red-50 rounded-xl border border-red-100 text-red-600 font-bold"><AlertCircle class="mx-auto mb-2" :size="32"/>{{ errorMsg }}</div>
            <div v-else-if="timeline.length === 0" class="text-center py-16 bg-white rounded-xl border border-dashed text-gray-500"><p class="text-lg">Nenhum registo encontrado.</p></div>

            <div v-else class="space-y-4">
                <div v-for="item in timeline" :key="item.id"
                     :class="`bg-white border rounded-lg overflow-hidden hover:shadow-md transition group ${item.isHidden ? 'border-red-300 bg-red-50' : 'border-gray-200'}`">

                    <div @click="toggleExpand(item)" class="p-4 flex items-center gap-4 cursor-pointer hover:bg-opacity-50 transition select-none">

                        <div v-if="item.isHidden" class="p-3 rounded-full flex-shrink-0 bg-red-100 text-red-600"><EyeOff :size="20"/></div>
                        <div v-else :class="`p-3 rounded-full flex-shrink-0 ${item.type === 'PUBLICATION' ? 'bg-blue-100 text-blue-600' : item.type === 'COMMENT' ? 'bg-green-100 text-green-600' : 'bg-amber-100 text-amber-600'}`">
                            <FileText v-if="item.type === 'PUBLICATION'" :size="20"/>
                            <MessageSquare v-else-if="item.type === 'COMMENT'" :size="20"/>
                            <Star v-else :size="20"/>
                        </div>

                        <div class="flex-grow min-w-0">
                            <div class="flex flex-col md:flex-row md:items-center gap-1 md:gap-3 mb-1">
                                <span class="bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded text-xs font-bold border border-indigo-100 self-start">@{{ item.author }}</span>
                                <span class="text-xs text-gray-400 flex items-center gap-1 whitespace-nowrap"><Clock :size="14"/> {{ formatDisplayDate(item.rawDate) }}</span>
                                <span v-if="item.isHidden" class="bg-red-200 text-red-800 px-2 py-0.5 rounded text-xs font-bold border border-red-300 ml-2">OCULTO</span>
                            </div>
                            <h4 :class="`font-bold text-sm md:text-base truncate ${item.isHidden ? 'text-red-700' : 'text-gray-800'}`">{{ item.title }}</h4>
                        </div>

                        <div class="text-gray-300 group-hover:text-indigo-500 transition">
                            <component :is="expandedIds.has(item.id) ? ChevronUp : ChevronDown" :size="24"/>
                        </div>
                    </div>

                    <div v-if="expandedIds.has(item.id)" class="bg-white/50 border-t border-gray-200 p-6 animate-fade-in-down">

                        <div v-if="item.isHidden" class="mb-4 p-3 bg-red-100 border border-red-200 rounded text-red-800 text-sm flex items-start gap-2">
                            <AlertCircle class="mt-0.5 flex-shrink-0" :size="16"/>
                            <div><strong>Item Oculto:</strong> Conteúdo não visível publicamente.</div>
                        </div>

                        <div class="flex items-center gap-2 mb-3">
                            <span class="text-xs font-bold text-gray-400 uppercase">Conteúdo Atual</span>
                            <div class="h-px bg-gray-200 flex-grow"></div>
                        </div>

                        <div class="text-sm text-gray-700 leading-relaxed bg-white p-4 rounded border border-gray-200 whitespace-pre-wrap font-mono mb-4">
                            {{ item.details || "Sem conteúdo textual disponível." }}
                        </div>

                        <div v-if="item.type === 'PUBLICATION'">
                            <div class="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs mb-6">
                                <div v-if="item.tags?.length" class="bg-white p-3 rounded border">
                                    <div class="font-bold text-gray-500 mb-1 flex items-center gap-1"><Tag :size="12"/> Tags Atuais</div>
                                    <div class="flex flex-wrap gap-1"><span v-for="t in item.tags" :key="t" class="bg-blue-50 text-blue-600 px-2 py-0.5 rounded border border-blue-100">{{ t }}</span></div>
                                </div>
                                <div v-if="item.pubAuthors?.length" class="bg-white p-3 rounded border">
                                    <div class="font-bold text-gray-500 mb-1 flex items-center gap-1"><Users :size="12"/> Autores</div>
                                    <p>{{ item.pubAuthors.join(', ') }}</p>
                                </div>
                            </div>

                            <div class="mt-8 bg-gray-50 rounded-xl p-4 border border-gray-200">
                                <div class="flex items-center gap-2 mb-4">
                                    <History :size="16" class="text-indigo-600"/>
                                    <span class="font-bold text-gray-700 text-sm uppercase tracking-wide">Histórico de Versões</span>
                                </div>

                                <div v-if="loadingHistoryIds.has(item.id)" class="text-center py-6 text-gray-500 flex flex-col items-center gap-2">
                                    <Loader2 class="animate-spin text-indigo-500" :size="24"/>
                                    <span class="text-xs">A carregar edições antigas...</span>
                                </div>

                                <div v-else-if="!item.history || item.history.length === 0" class="text-center py-4 text-gray-400 text-xs italic bg-white rounded border border-dashed border-gray-300">
                                    Esta publicação nunca foi editada.
                                </div>

                                <div v-else class="space-y-4">
                                    <div v-for="(ver, idx) in item.history" :key="idx"
                                         class="bg-white rounded-lg border-l-4 border-amber-400 shadow-sm p-3 relative text-sm hover:shadow-md transition">

                                        <div class="flex justify-between items-start border-b border-gray-100 pb-2 mb-2">
                                            <div>
                                                <div class="font-bold text-gray-800 flex items-center gap-2">
                                                    <User :size="12" class="text-gray-400"/>
                                                    {{ ver.modifierName || 'Sistema' }}
                                                </div>
                                                <div class="text-xs text-gray-400 flex items-center gap-1 mt-0.5">
                                                    <Clock :size="10"/> {{ formatDisplayDate(ver.date) }}
                                                </div>
                                            </div>
                                            <span class="bg-amber-50 text-amber-700 text-[10px] font-bold px-2 py-0.5 rounded uppercase">Versão Arquivada</span>
                                        </div>

                                        <div class="space-y-2 text-xs">
                                            <div v-if="ver.oldTitle">
                                                <span class="font-bold text-gray-500 block mb-0.5">Título Anterior:</span>
                                                <span class="text-gray-700 bg-gray-50 px-2 py-1 rounded block">{{ ver.oldTitle }}</span>
                                            </div>

                                            <div v-if="ver.description">
                                                <span class="font-bold text-gray-500 block mb-0.5">Descrição Anterior:</span>
                                                <div class="text-gray-600 bg-gray-50 p-2 rounded border border-gray-100 font-mono text-[11px] leading-relaxed max-h-24 overflow-y-auto">
                                                    {{ ver.description }}
                                                </div>
                                            </div>

                                            <div v-if="ver.tagsSnapshots">
                                                <span class="font-bold text-gray-500 block mb-0.5">Tags na altura:</span>
                                                <span class="text-indigo-600 bg-indigo-50 px-2 py-1 rounded block truncate">
                                                    {{ ver.tagsSnapshots }}
                                                </span>
                                            </div>
                                        </div>
                                    </div>
                                </div>
                            </div>

                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.animate-fade-in-down { animation: fadeInDown 0.3s ease-out; }
@keyframes fadeInDown { from { opacity: 0; transform: translateY(-10px); } to { opacity: 1; transform: translateY(0); } }
</style>
