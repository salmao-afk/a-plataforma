<script setup>
defineProps({
  average: { type: Number, default: 0 }
});
</script>

<template>
  <div class="flex items-center text-sm font-medium">

    <div v-if="average > 0" class="flex items-center gap-1.5 bg-yellow-50 text-yellow-800 px-3 py-1 rounded-full border border-yellow-200 shadow-sm">
      <svg class="w-4 h-4 text-yellow-500 fill-yellow-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      <span class="font-bold text-gray-900">{{ average.toFixed(1) }}</span>
    </div>

    <div v-else class="text-xs text-gray-400 bg-gray-50 px-2 py-1 rounded border border-gray-200">
      Sem classificação
    </div>

  </div>
</template>