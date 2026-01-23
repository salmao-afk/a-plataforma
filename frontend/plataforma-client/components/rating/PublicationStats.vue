<script setup>
defineProps({
  average: { type: Number, default: 0 },
  ratingsCount: { type: Number, default: 0 },
  commentsCount: { type: Number, default: 0 }
});
</script>

<template>
  <div class="flex items-center gap-4 text-sm font-medium">

    <div class="flex items-center gap-1.5 bg-yellow-50 text-yellow-800 px-2 py-1 rounded-md border border-yellow-100">
      <svg class="w-4 h-4 text-yellow-500 fill-yellow-500" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24"><path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/></svg>
      <span class="font-bold">{{ average.toFixed(1) }}</span>
      <span class="text-xs text-yellow-600 opacity-80">({{ ratingsCount }} avaliações)</span>
    </div>

    <div class="flex items-center gap-1.5 text-gray-500 hover:text-blue-600 transition">
      <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M7 8h10M7 12h4m1 8l-4-4H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-3l-4 4z" /></svg>
      <span>{{ commentsCount }} comentários</span>
    </div>

  </div>
</template>