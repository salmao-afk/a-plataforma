// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
    compatibilityDate: '2025-07-15',

    // 1. Close the devtools object here
    devtools: { enabled: true },

    // 2. These are now correctly placed at the root level
    modules: ['@pinia/nuxt',
    '@nuxtjs/tailwindcss'],

    runtimeConfig: {
        public: {
            // TEM DE SER 'apiBase' PARA BATER CERTO COM O SEU LOGIN.VUE
            apiBase: 'http://localhost:8080/plataforma/api'
        }
    },


    postcss: {
        plugins: {
            autoprefixer: {},
        },
    },
})