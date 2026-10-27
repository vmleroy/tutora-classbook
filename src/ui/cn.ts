import { clsx, type ClassValue } from 'clsx';
import { extendTailwindMerge } from 'tailwind-merge';
import { createTV } from 'tailwind-variants';

import { colorSets, radius, size, spacing, typeStyles } from '@/ui/theme/tokens';

// tailwind-merge has to know our token names: otherwise `text-body` (a size) and
// `text-ink` (a color) look like one group and the first is dropped.
const twMergeConfig = {
  extend: {
    theme: {
      colors: [
        ...Object.keys(colorSets['quadro-light']),
        'btn-primary',
        'on-btn-primary',
        'tab-active',
        'tab-active-soft',
      ],
      spacing: [...Object.keys(spacing), ...Object.keys(size)],
      borderRadius: Object.keys(radius),
    },
    classGroups: {
      'font-size': [{ text: Object.keys(typeStyles) }],
      'font-family': [{ font: ['regular', 'semibold', 'bold', 'black'] }],
    },
  },
};

const twMerge = extendTailwindMerge(twMergeConfig);

/** Joins class names like clsx, then lets the later of two conflicting token classes win. */
export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/** tailwind-variants with the same token-aware merging. */
export const tv = createTV({ twMergeConfig });
