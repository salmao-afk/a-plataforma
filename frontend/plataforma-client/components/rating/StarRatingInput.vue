<script setup>
import { ref } from 'vue';

const props = defineProps({
  modelValue: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
});

const emit = defineEmits(['update:modelValue', 'rate']);
const hoverValue = ref(0);

const isFilled = (star) => {
  if (props.readonly) return star <= props.modelValue;
  return star <= (hoverValue.value || props.modelValue);
};

const handleRate = (star) => {
  if (props.readonly) return;
  emit('update:modelValue', star);
  emit('rate', star);
};
</script>

<template>
  <div class="flex items-center gap-1">
    <button
        v-for="star in 5"
        :key="star"
        type="button"
        @click="handleRate(star)"
        @mouseenter="!readonly ? hoverValue = star : null"
        @mouseleave="!readonly ? hoverValue = 0 : null"
        :class="['transition-transform duration-200', readonly ? 'cursor-default' : 'cursor-pointer hover:scale-110']"
        :disabled="readonly"
    >
      <svg
          class="w-8 h-8 transition-colors duration-200"
          :class="isFilled(star) ? 'text-yellow-400 fill-yellow-400 drop-shadow-sm' : 'text-gray-200 fill-gray-100'"
          xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.5"
      >
        <path stroke-linecap="round" stroke-linejoin="round" d="M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z" />
      </svg>
    </button>
  </div>
</template>