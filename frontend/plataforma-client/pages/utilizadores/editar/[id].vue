<script setup lang="ts">
import { reactive, ref, onMounted } from "vue";
import { useAuthStore } from "~/stores/auth-store";
import { ArrowLeft, Save, Shield } from "lucide-vue-next";

// --- VALIDAÇÃO DE ROTA ---
definePageMeta({
    validate: async (route) => {
        const reservedWords = ['index', 'create', 'editar'];
        return !reservedWords.includes(route.params.id as string);
    }
});

const route = useRoute();
const router = useRouter();
const config = useRuntimeConfig();
const authStore = useAuthStore();
const api = config.public.apiBase;

// URL: /utilizadores/editar/joaosilva
const userId = route.params.id as string;

const formData = reactive({
    name: "",
    email: "",
    role: ""
});
let originalRole = "";

const feedback = ref("");
const isError = ref(false);
const isLoading = ref(true);

// --- MAPA DE ROLES (Para o Select) ---
const roleOptions = [
    { value: 'COLLABORATOR', label: 'Colaborador' },
    { value: 'RESPONSIBLE', label: 'Responsável' },
    { value: 'ADMINISTRATOR', label: 'Administrador' }
];

// --- CARREGAR DADOS ---
onMounted(async () => {
    isLoading.value = true;
    try {
        const user = await $fetch(`${api}/utilizadores/${userId}`, {
            headers: { Authorization: `Bearer ${authStore.token}` }
        });
        formData.name = user.name;
        formData.email = user.email;

        // Garante que a role vem preenchida
        formData.role = user.role || "COLLABORATOR";
        originalRole = user.role;
    } catch (e) {
        feedback.value = "Erro ao carregar dados do utilizador.";
        isError.value = true;
    } finally {
        isLoading.value = false;
    }
});

// --- GUARDAR ---
async function handleSubmit() {
    feedback.value = "";
    isError.value = false;

    try {
        // 1. Atualizar Perfil (PUT)
        await $fetch(`${api}/utilizadores/${userId}`, {
            method: "PUT",
            headers: { Authorization: `Bearer ${authStore.token}` },
            body: { name: formData.name, email: formData.email }
        });

        // 2. Atualizar Role (PATCH) - SÓ SE MUDOU
        if (formData.role !== originalRole) {
            await $fetch(`${api}/utilizadores/${userId}/role`, {
                method: "PATCH",
                headers: { Authorization: `Bearer ${authStore.token}` },
                body: { role: formData.role } // Envia 'ADMINISTRATOR', etc.
            });
        }

        feedback.value = "Utilizador atualizado com sucesso!";

        // Redireciona para a lista de utilizadores
        setTimeout(() => router.push(`/utilizadores`), 1000);

    } catch (e: any) {
        isError.value = true;
        feedback.value = e.response?._data?.message || e.response?._data || "Erro ao atualizar.";
        console.error("Erro update:", e);
    }
}
</script>

<template>
    <div class="min-h-screen bg-gray-50 p-6 flex justify-center items-start pt-10 font-sans text-slate-800">
        <div class="w-full max-w-lg bg-white rounded-xl shadow-sm p-8 border border-gray-200">

            <div class="flex items-center gap-4 mb-6">
                <NuxtLink to="/utilizadores" class="p-2 hover:bg-gray-100 rounded-full text-gray-500 transition">
                    <ArrowLeft :size="24"/>
                </NuxtLink>
                <div>
                    <h1 class="text-2xl font-bold text-gray-900">Editar Utilizador</h1>
                    <p class="text-sm text-gray-500">@{{ userId }}</p>
                </div>
            </div>

            <div v-if="feedback" :class="`p-3 rounded-lg mb-6 text-sm font-bold text-center ${isError ? 'bg-red-100 text-red-700 border border-red-200' : 'bg-green-100 text-green-700 border border-green-200'}`">
                {{ feedback }}
            </div>

            <div v-if="isLoading" class="text-center py-10 text-gray-500 animate-pulse">
                A carregar dados...
            </div>

            <form v-else @submit.prevent="handleSubmit" class="space-y-5">

                <div>
                    <label class="block text-xs font-bold text-gray-500 uppercase tracking-wide mb-1">Username</label>
                    <input :value="userId" disabled class="w-full p-2.5 border border-gray-200 rounded-lg bg-gray-50 text-gray-500 cursor-not-allowed font-medium" />
                </div>

                <div>
                    <label class="block text-xs font-bold text-gray-700 uppercase tracking-wide mb-1">Nome Completo</label>
                    <input v-model="formData.name" type="text" required class="w-full p-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition" />
                </div>

                <div>
                    <label class="block text-xs font-bold text-gray-700 uppercase tracking-wide mb-1">Email</label>
                    <input v-model="formData.email" type="email" required class="w-full p-2.5 border border-gray-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none transition" />
                </div>

                <div>
                    <label class="block text-xs font-bold text-gray-700 uppercase tracking-wide mb-1 flex items-center gap-1">
                        Função <Shield :size="14" class="text-indigo-500"/>
                    </label>
                    <div class="relative">
                        <select v-model="formData.role" class="w-full p-2.5 border border-gray-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none appearance-none transition font-medium text-gray-700">
                            <option v-for="option in roleOptions" :key="option.value" :value="option.value">
                                {{ option.label }}
                            </option>
                        </select>
                        <div class="absolute inset-y-0 right-0 flex items-center px-2 pointer-events-none text-gray-500">
                            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                        </div>
                    </div>
                </div>

                <div class="pt-4 flex gap-3">
                    <button type="submit" class="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-bold py-3 rounded-lg flex justify-center items-center gap-2 transition shadow-md">
                        <Save :size="18"/> Guardar Alterações
                    </button>
                    <NuxtLink to="/utilizadores" class="px-4 py-3 border border-gray-300 rounded-lg font-bold text-gray-600 hover:bg-gray-50 transition">
                        Cancelar
                    </NuxtLink>
                </div>

            </form>
        </div>
    </div>
</template>
