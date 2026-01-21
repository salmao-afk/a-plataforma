import { defineStore } from 'pinia';
import { useAuthStore } from './auth-store';

export const useTagsStore = defineStore('tags', {
    state: () => ({
        tags: [],
        currentTag: null, // Para a página de detalhes
        isLoading: false,
    }),

    actions: {
        async fetchTags() {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            this.isLoading = true;
            try {
                const data = await $fetch(`${config.public.apiBase}/tags`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                this.tags = data;
            } catch (e) {
                console.error("Erro ao carregar tags", e);
            } finally {
                this.isLoading = false;
            }
        },

        // NOVO: Buscar uma tag específica
        async fetchTagById(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            try {
                const data = await $fetch(`${config.public.apiBase}/tags/${id}`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                this.currentTag = data;
                return data;
            } catch (e) {
                throw e;
            }
        },

        async createTag(payload) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            await $fetch(`${config.public.apiBase}/tags`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${authStore.token}` },
                body: payload
            });
            await this.fetchTags();
        },

        async updateTag(id, payload) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            await $fetch(`${config.public.apiBase}/tags/${id}`, {
                method: 'PUT',
                headers: { Authorization: `Bearer ${authStore.token}` },
                body: payload
            });
            await this.fetchTags();
        },

        // CORRIGIDO: Delete seguro
        async deleteTag(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();

            // Adicionado responseType: 'text' porque o backend retorna uma String simples
            await $fetch(`${config.public.apiBase}/tags/${id}`, {
                method: 'DELETE',
                headers: { Authorization: `Bearer ${authStore.token}` },
                responseType: 'text'
            });

            this.tags = this.tags.filter(t => t.id !== id);
        },

        async subscribeTag(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            await $fetch(`${config.public.apiBase}/tags/${id}/subscrever`, {
                method: 'POST',
                headers: { Authorization: `Bearer ${authStore.token}` }
            });
        },

        async unsubscribeTag(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            await $fetch(`${config.public.apiBase}/tags/${id}/dessubscrever`, {
                method: 'DELETE',
                headers: { Authorization: `Bearer ${authStore.token}` }
            });
        }
    }
});