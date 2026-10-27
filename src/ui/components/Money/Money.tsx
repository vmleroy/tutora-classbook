import { useTranslation } from 'react-i18next';

import type { Currency } from '@/domain/money';
import type { Language } from '@/domain/settings';
import { Text } from '@/ui/components/Text';
import { formatMoney } from '@/ui/format';

export interface MoneyProps {
  cents: number;
  currency?: Currency;
  locale?: string;
  size?: 'display' | 'xl' | 'lg' | 'md' | 'sm';
  tone?: 'default' | 'earnings' | 'owed' | 'paid' | 'onHeader';
  compact?: boolean;
  showSign?: boolean;
}

const variant = {
  display: 'money-display',
  xl: 'money-xl',
  lg: 'money-lg',
  md: 'money-md',
  sm: 'row-title',
} as const;

const toneClass = {
  default: 'text-ink',
  earnings: 'text-earnings',
  owed: 'text-danger-ink',
  paid: 'text-success-ink',
  onHeader: 'text-earnings-on-header',
} as const;

/** Integer cents in the app language, always in tabular figures. */
export function Money({
  cents,
  currency = 'BRL',
  locale,
  size = 'md',
  tone = 'default',
  compact,
  showSign,
}: MoneyProps) {
  const { i18n } = useTranslation();
  const language = (locale ?? i18n.language) === 'en' ? 'en' : ('pt-BR' satisfies Language);
  const text = formatMoney(cents, currency, language, compact);
  return (
    <Text variant={variant[size]} tabular className={toneClass[tone]}>
      {showSign && cents > 0 ? `+ ${text}` : text}
    </Text>
  );
}
