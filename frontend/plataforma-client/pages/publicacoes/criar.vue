<script setup>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '~/stores/auth-store.js';
import { useTagsStore } from '~/stores/tags-store.js'; // 1. Importar Store de Tags

const router = useRouter();
const authStore = useAuthStore();
const tagStore = useTagsStore(); // 2. Inicializar Store
const config = useRuntimeConfig();

const form = ref({
    title: '',
    scientificArea: '',
    type: 'Tese',
    description: '',
    authors: '',
    tags: [], // 3. Agora é um Array, não uma string vazia
    file: null
});

const isSubmitting = ref(false);

// --- CARREGAR TAGS EXISTENTES ---
onMounted(() => {
    tagStore.fetchTags();
});

// --- FUNÇÃO DE SELEÇÃO DE TAGS ---
const toggleTag = (tagName) => {
    if (form.value.tags.includes(tagName)) {
        // Se já existe, remove
        form.value.tags = form.value.tags.filter(t => t !== tagName);
    } else {
        // Se não existe, adiciona
        form.value.tags.push(tagName);
    }
};

// --- ESTADO DA NOTIFICAÇÃO (TOAST) ---
const toast = ref({
    show: false,
    message: '',
    type: 'success'
});

const showToast = (message, type = 'success') => {
    toast.value = { show: true, message, type };
    setTimeout(() => {
        toast.value.show = false;
    }, 4000);
};

const handleFileUpload = (event) => {
    const file = event.target.files[0];
    if (file) {
        if (file.type !== 'application/pdf') {
            showToast("Por favor, carregue apenas ficheiros PDF.", "error");
            event.target.value = null;
            form.value.file = null;
            return;
        }
        form.value.file = file;
    }
};

const submitPublication = async () => {
    if (!form.value.file) {
        showToast("O upload do ficheiro PDF é obrigatório.", "error");
        return;
    }

    isSubmitting.value = true;

    try {
        const formData = new FormData();
        formData.append('title', form.value.title);
        formData.append('scientificArea', form.value.scientificArea);
        formData.append('type', form.value.type);
        formData.append('description', form.value.description || 'A aguardar resumo IA...'); // Placeholder
        formData.append('authors', form.value.authors);

        // 4. Enviar Tags (Iterar o array e adicionar um a um)
        form.value.tags.forEach(tag => {
            formData.append('tags', tag);
        });

        formData.append('file', form.value.file);

        await $fetch(`${config.public.apiBase}/publicacoes`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: formData
        });

        showToast("Publicação submetida! A IA está a processar o resumo...", "success");

        setTimeout(() => {
            router.push('/publicacoes');
        }, 2000);

    } catch (error) {
        console.error("Erro:", error);
        showToast(error.data?.message || "Erro ao criar publicação.", "error");
    } finally {
        isSubmitting.value = false;
    }
};
</script>

<template>
    <div class="max-w-3xl mx-auto py-10 px-4 relative font-sans">

        <div class="mb-8 text-center md:text-left">
            <h1 class="text-4xl font-extrabold text-gray-900 tracking-tight">Nova Publicação</h1>
            <p class="text-lg text-gray-500 mt-2">Partilhe conhecimento com a comunidade académica.</p>
        </div>

        <form @submit.prevent="submitPublication" class="bg-white p-8 rounded-2xl shadow-xl border border-gray-100 space-y-8 relative overflow-hidden">

            <div class="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500"></div>

            <div class="group">
                <label class="block text-sm font-bold text-gray-700 mb-2 group-focus-within:text-blue-600 transition">Título da Publicação <span class="text-red-500">*</span></label>
                <input v-model="form.title" type="text" required class="input-field" placeholder="Ex: Análise de Algoritmos Genéticos..." />
            </div>

            <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div class="group">
                    <label class="block text-sm font-bold text-gray-700 mb-2 group-focus-within:text-blue-600 transition">Área Científica <span class="text-red-500">*</span></label>
                    <input v-model="form.scientificArea" type="text" required class="input-field" placeholder="Ex: Engenharia Informática" />
                </div>

                <div class="group">
                    <label class="block text-sm font-bold text-gray-700 mb-2 group-focus-within:text-blue-600 transition">Tipo da Publicação <span class="text-red-500">*</span></label>
                    <div class="relative">
                        <select v-model="form.type" class="input-field appearance-none cursor-pointer">
                            <option value="Tese">Tese</option>
                            <option value="Artigo">Artigo</option>
                            <option value="Relatorio">Relatório</option>
                            <option value="Outro">Outro</option>
                        </select>
                        <div class="absolute inset-y-0 right-0 flex items-center px-3 pointer-events-none text-gray-500">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" /></svg>
                        </div>
                    </div>
                </div>
            </div>

            <div class="group">
                <label class="block text-sm font-bold text-gray-700 mb-2 group-focus-within:text-blue-600 transition">Autores <span class="text-red-500">*</span></label>
                <input v-model="form.authors" type="text" required class="input-field" placeholder="Separe os nomes por vírgulas..." />
            </div>

            <div class="group">
                <label class="block text-sm font-bold text-gray-700 mb-2">
                    Associar Tags <span class="text-gray-400 font-normal text-xs ml-1">(Selecione da lista)</span>
                </label>

                <div v-if="tagStore.isLoading" class="p-4 bg-gray-50 rounded-xl text-center text-gray-400 text-sm animate-pulse">
                    A carregar tags disponíveis...
                </div>

                <div v-else-if="!tagStore.tags.length" class="p-4 bg-gray-50 rounded-xl text-center text-gray-400 text-sm italic">
                    Não existem tags disponíveis no sistema.
                </div>

                <div v-else class="bg-gray-50 p-4 rounded-xl border border-gray-200">
                    <div class="flex flex-wrap gap-2 max-h-40 overflow-y-auto pr-2 custom-scrollbar">
                        <button
                            type="button"
                            v-for="tag in tagStore.tags"
                            :key="tag.id"
                            @click="toggleTag(tag.name)"
                            class="px-3 py-1.5 rounded-lg text-sm font-medium transition-all duration-200 border"
                            :class="form.tags.includes(tag.name)
                  ? 'bg-blue-600 text-white border-blue-600 shadow-md transform scale-105'
                  : 'bg-white text-gray-600 border-gray-200 hover:border-blue-300 hover:text-blue-600'"
                        >
                            #{{ tag.name }}
                        </button>
                    </div>

                    <div v-if="form.tags.length > 0" class="mt-3 pt-3 border-t border-gray-200">
                        <p class="text-xs text-gray-500 mb-1">Selecionadas:</p>
                        <div class="flex flex-wrap gap-1">
              <span v-for="t in form.tags" :key="t" class="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded font-bold">
                {{ t }}
              </span>
                        </div>
                    </div>
                </div>
            </div>

            <div class="relative p-1 rounded-xl bg-gradient-to-r from-blue-100 via-purple-100 to-pink-100">
                <div class="bg-white rounded-lg p-5">
                    <div class="flex justify-between items-center mb-3">
                        <label class="block text-sm font-bold text-gray-800">Resumo</label>
                        <div class="flex items-center gap-1.5 bg-gradient-to-r from-blue-600 to-purple-600 text-white px-3 py-1 rounded-full text-xs font-bold shadow-sm">
                            <svg class="w-3 h-3 animate-pulse" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z" /></svg>
                            IA Autopilot
                        </div>
                    </div>
                    <div class="text-sm text-gray-500 bg-gray-50 p-3 rounded-lg border border-gray-100 italic">
                        O resumo será gerado automaticamente pela Inteligência Artificial após o upload do ficheiro.
                    </div>
                </div>
            </div>

            <div>
                <label class="block text-sm font-bold text-gray-700 mb-2">Ficheiro PDF <span class="text-red-500">*</span></label>

                <label
                    class="flex flex-col items-center justify-center w-full h-40 border-2 border-dashed border-gray-300 rounded-2xl cursor-pointer hover:bg-blue-50 hover:border-blue-300 transition-all duration-300 group"
                    :class="{'bg-green-50 border-green-400': form.file}"
                >
                    <div v-if="form.file" class="flex flex-col items-center animate-fade-in-up">
                        <div class="w-12 h-12 bg-white text-green-500 rounded-full shadow-md flex items-center justify-center mb-2">
                            <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                        </div>
                        <p class="font-bold text-gray-800 text-sm">{{ form.file.name }}</p>
                        <p class="text-xs text-gray-500 mt-1">Clique para substituir</p>
                    </div>

                    <div v-else class="flex flex-col items-center text-gray-400 group-hover:text-blue-500 transition">
                        <div class="p-3 bg-gray-100 rounded-full mb-3 group-hover:bg-white group-hover:shadow-md transition">
                            <svg class="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" /></svg>
                        </div>
                        <p class="font-semibold text-sm">Clique ou arraste o PDF aqui</p>
                    </div>

                    <input type="file" @change="handleFileUpload" accept="application/pdf" class="hidden" />
                </label>
            </div>

            <div class="pt-4">
                <button
                    type="submit"
                    :disabled="isSubmitting"
                    class="w-full bg-gray-900 text-white py-4 rounded-xl font-bold hover:bg-black transition transform hover:-translate-y-1 hover:shadow-lg disabled:opacity-70 disabled:cursor-wait disabled:transform-none flex justify-center items-center gap-3 text-lg"
                >
                    <span v-if="isSubmitting" class="animate-spin h-5 w-5 border-2 border-white border-t-transparent rounded-full"></span>
                    {{ isSubmitting ? 'A Processar...' : 'Publicar Publicação' }}
                </button>
            </div>

        </form>

        <Transition name="toast">
            <div
                v-if="toast.show"
                class="fixed top-24 right-5 z-50 flex items-center gap-3 px-6 py-4 rounded-xl shadow-2xl backdrop-blur-md border border-white/20 min-w-[300px]"
                :class="toast.type === 'success' ? 'bg-emerald-500/90 text-white' : 'bg-rose-500/90 text-white'"
            >
                <div v-if="toast.type === 'success'" class="bg-white/20 p-2 rounded-full">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M5 13l4 4L19 7" /></svg>
                </div>
                <div v-else class="bg-white/20 p-2 rounded-full">
                    <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" /></svg>
                </div>
                <div>
                    <h4 class="font-bold text-sm">{{ toast.type === 'success' ? 'Sucesso!' : 'Atenção' }}</h4>
                    <p class="text-sm opacity-90">{{ toast.message }}</p>
                </div>
            </div>
        </Transition>

    </div>
</template>

<style scoped>
.input-field {
    @apply w-full bg-gray-50 border border-gray-200 rounded-xl px-5 py-3 outline-none focus:ring-2 focus:ring-blue-500 focus:bg-white focus:border-transparent transition duration-200 text-gray-700 font-medium placeholder-gray-400;
}

/* Scrollbar personalizada para a lista de tags */
.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-track {
    background: #f1f1f1;
    border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 4px;
}
.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #9ca3af;
}

.toast-enter-active,
.toast-leave-active {
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}
.toast-enter-from,
.toast-leave-to {
    opacity: 0;
    transform: translateX(100px) scale(0.9);
}

.animate-fade-in-up {
    animation: fadeInUp 0.3s ease-out;
}
@keyframes fadeInUp {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
