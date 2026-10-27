import { View } from 'react-native';

import { cn, tv } from '@/ui/cn';

export type Tag = 'amber' | 'green' | 'blue' | 'violet' | 'orange' | 'red' | 'teal';

export interface TagDotProps {
  tag?: Tag;
  size?: 'sm' | 'md';
}

export const tagFill: Record<Tag, string> = {
  amber: 'bg-tag-amber',
  green: 'bg-tag-green',
  blue: 'bg-tag-blue',
  violet: 'bg-tag-violet',
  orange: 'bg-tag-orange',
  red: 'bg-tag-red',
  teal: 'bg-tag-teal',
};

export const tagSoft: Record<Tag, string> = {
  amber: 'bg-tag-amber-soft',
  green: 'bg-tag-green-soft',
  blue: 'bg-tag-blue-soft',
  violet: 'bg-tag-violet-soft',
  orange: 'bg-tag-orange-soft',
  red: 'bg-tag-red-soft',
  teal: 'bg-tag-teal-soft',
};

export const tagInk: Record<Tag, string> = {
  amber: 'text-tag-amber-ink',
  green: 'text-tag-green-ink',
  blue: 'text-tag-blue-ink',
  violet: 'text-tag-violet-ink',
  orange: 'text-tag-orange-ink',
  red: 'text-tag-red-ink',
  teal: 'text-tag-teal-ink',
};

const dot = tv({
  base: 'rounded-full',
  variants: { size: { md: 'h-2.5 w-2.5', sm: 'h-2 w-2' } },
});

/** A student's or place's color mark; never the only signal, so hidden from screen readers. */
export function TagDot({ tag = 'amber', size = 'md' }: TagDotProps) {
  return (
    <View
      testID={`tag-dot-${tag}`}
      importantForAccessibility="no-hide-descendants"
      accessibilityElementsHidden
      className={cn(dot({ size }), tagFill[tag])}
    />
  );
}
