<template>
  <div class="h-full w-full">
    <NuxtLayout>
      <NuxtPage />
    </NuxtLayout>
  </div>
</template>

<style>
/* ESTA PARTE É OBRIGATÓRIA PARA REMOVER AS BORDAS BRANCAS */
html, body, #__nuxt {
  height: 100%;
  width: 100%;
  margin: 0;
  padding: 0;
  background-color: #f9fafb; /* Cinza muito claro */
}
</style>