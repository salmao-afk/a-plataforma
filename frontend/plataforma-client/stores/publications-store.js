import { defineStore } from 'pinia';
import { useAuthStore } from './auth-store';

export const usePublicationsStore = defineStore('publications', {
    state: () => ({
        publications: [],
        currentPublication: null, // Para a edição/detalhes
        isLoading: false,
        error: null,
    }),

    getters: {
        // Exemplo: Retorna apenas as publicações de um user específico
        getMyPublications: (state) => (username) => {
            return state.publications.filter(p => p.uploaderUsername === username);
        }
    },

    actions: {
        // 1. FETCH ALL
        async fetchPublications() {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            this.isLoading = true;
            this.error = null;

            try {
                const data = await $fetch(`${config.public.apiBase}/publicacoes`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                this.publications = data;
            } catch (e) {
                this.error = "Erro ao carregar publicações";
                console.error(e);
            } finally {
                this.isLoading = false;
            }
        },

        // 2. FETCH ONE (Por ID)
        async fetchPublicationById(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();

            try {
                const data = await $fetch(`${config.public.apiBase}/publicacoes/${id}`, {
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                this.currentPublication = data;
                return data; // Retorna para uso imediato no componente
            } catch (e) {
                console.error("Erro ao carregar publicação", e);
                throw e;
            }
        },

        // 3. CREATE (Upload)
        async createPublication(formData) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            this.isLoading = true;

            try {
                await $fetch(`${config.public.apiBase}/publicacoes`, {
                    method: 'POST',
                    headers: { Authorization: `Bearer ${authStore.token}` },
                    body: formData
                });
                // Atualiza a lista localmente ou força refresh
                await this.fetchPublications();
            } catch (e) {
                throw e;
            } finally {
                this.isLoading = false;
            }
        },

        // 4. UPDATE (Editar)
        async updatePublication(id, payload) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();
            this.isLoading = true;

            try {
                await $fetch(`${config.public.apiBase}/publicacoes/${id}`, {
                    method: 'PUT',
                    headers: { Authorization: `Bearer ${authStore.token}` },
                    body: payload
                });
                await this.fetchPublications(); // Atualiza a lista
            } catch (e) {
                throw e;
            } finally {
                this.isLoading = false;
            }
        },

        // 5. DELETE
        async deletePublication(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();

            try {
                await $fetch(`${config.public.apiBase}/publicacoes/${id}`, {
                    method: 'DELETE',
                    headers: { Authorization: `Bearer ${authStore.token}` }
                });
                // Remove da lista localmente (mais rápido que fazer fetch de novo)
                this.publications = this.publications.filter(p => p.id !== id);
            } catch (e) {
                throw e;
            }
        },

        // 6. DOWNLOAD (Lógica complexa escondida aqui)
        async downloadPublication(id) {
            const authStore = useAuthStore();
            const config = useRuntimeConfig();

            try {
                const response = await $fetch.raw(`${config.public.apiBase}/publicacoes/${id}/download`, {
                    headers: { Authorization: `Bearer ${authStore.token}` },
                    responseType: 'blob'
                });

                const blob = response._data;
                const header = response.headers.get('content-disposition');
                let filename = `documento_${id}`;
                if (header) {
                    const matches = header.match(/filename="?([^"]+)"?/);
                    if (matches && matches[1]) filename = matches[1];
                }

                const url = window.URL.createObjectURL(blob);
                const link = document.createElement('a');
                link.href = url;
                link.setAttribute('download', filename);
                document.body.appendChild(link);
                link.click();
                document.body.removeChild(link);
                window.URL.revokeObjectURL(url);

            } catch (e) {
                console.error("Erro no download", e);
                throw e;
            }
        }
    }
});