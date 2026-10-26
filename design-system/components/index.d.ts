import type * as React from 'react';

export type IconName = 'plus' | 'minus' | 'check' | 'close' | 'chevron-left' | 'chevron-right' | 'home' | 'calendar' | 'users' | 'pin' | 'wallet' | 'clock' | 'alert' | 'file' | 'cloud' | 'gear' | 'calendar-check' | 'no-show' | 'play';
export type Tag = 'amber' | 'green' | 'blue' | 'violet' | 'orange' | 'red' | 'teal';
export type Currency = 'BRL' | 'USD';

export interface IconProps { name: IconName; size?: number; strokeWidth?: number; label?: string }
export declare function Icon(props: IconProps): React.ReactElement;

export interface ButtonProps { variant?: 'primary' | 'accent' | 'outline' | 'ghost' | 'danger'; size?: 'sm' | 'md' | 'lg'; icon?: IconName; fullWidth?: boolean; loading?: boolean; disabled?: boolean; onPress?: () => void; children: React.ReactNode }
export declare function Button(props: ButtonProps): React.ReactElement;

export interface IconButtonProps { icon: IconName; label: string; variant?: 'sunken' | 'plain' | 'onPrimary' | 'accent'; disabled?: boolean; onPress?: () => void }
export declare function IconButton(props: IconButtonProps): React.ReactElement;

export interface FabProps { icon?: IconName; label?: string; onPress?: () => void }
export declare function Fab(props: FabProps): React.ReactElement;

export interface SegmentedControlProps<T extends string = string> { options: { value: T; label: string }[]; value?: T; onChange?: (value: T) => void; label: string; size?: 'sm' | 'md' }
export declare function SegmentedControl(props: SegmentedControlProps): React.ReactElement;

export interface ChipProps { label: string; selected?: boolean; onPress?: (selected: boolean) => void }
export declare function Chip(props: ChipProps): React.ReactElement;

export interface SwitchProps { value?: boolean; onChange?: (value: boolean) => void; label: string; disabled?: boolean }
export declare function Switch(props: SwitchProps): React.ReactElement;

export interface CheckboxProps { label: string; checked?: boolean; onChange?: (checked: boolean) => void }
export declare function Checkbox(props: CheckboxProps): React.ReactElement;

export interface StepperProps { value?: number; step?: number; min?: number; max?: number; format?: (value: number) => string; onChange?: (value: number) => void; decrementLabel?: string; incrementLabel?: string; size?: 'sm' | 'md' }
export declare function Stepper(props: StepperProps): React.ReactElement;

export interface TextFieldProps { label: string; value?: string; placeholder?: string; helper?: string; error?: string; inputMode?: string; tone?: 'default' | 'onPrimary'; onChangeText?: (text: string) => void }
export declare function TextField(props: TextFieldProps): React.ReactElement;

export interface SelectFieldProps { label?: string; value: string; sub?: string; avatar?: { initials: string; tag: Tag }; icon?: IconName; actionLabel?: string; onPress?: () => void }
export declare function SelectField(props: SelectFieldProps): React.ReactElement;

export interface BadgeProps { tone?: 'neutral' | 'success' | 'danger' | 'accent' | 'secondary' | 'violet'; icon?: IconName; children: React.ReactNode }
export declare function Badge(props: BadgeProps): React.ReactElement;

export interface AvatarProps { initials: string; tag?: Tag; size?: 'sm' | 'md' | 'lg' }
export declare function Avatar(props: AvatarProps): React.ReactElement;

export interface TagDotProps { tag?: Tag; size?: 'sm' | 'md' }
export declare function TagDot(props: TagDotProps): React.ReactElement;

export interface MoneyProps { cents: number; currency?: Currency; locale?: string; size?: 'display' | 'xl' | 'lg' | 'md' | 'sm'; tone?: 'default' | 'earnings' | 'owed' | 'paid' | 'onHeader'; compact?: boolean; showSign?: boolean }
export declare function Money(props: MoneyProps): React.ReactElement;

export interface ProgressBarProps { value: number; tone?: 'accent' | 'success' | 'primary' | 'secondary'; track?: 'accent' | 'danger' | 'sunken'; thin?: boolean; label: string }
export declare function ProgressBar(props: ProgressBarProps): React.ReactElement;

export interface CardProps { elevation?: 'flat' | 'raised' | 'outline'; label?: string; children: React.ReactNode }
export declare function Card(props: CardProps): React.ReactElement;

export interface EarningsSummaryProps { title: string; earnedCents: number; expectedCents?: number; expectedLabel?: string; stats?: { label: string; cents: number; tone?: MoneyProps['tone'] }[] }
export declare function EarningsSummary(props: EarningsSummaryProps): React.ReactElement;

export interface SectionHeaderProps { title: string; aside?: string; asideTone?: 'muted' | 'primary' }
export declare function SectionHeader(props: SectionHeaderProps): React.ReactElement;

export interface LessonRowProps { time: string; tag: Tag; title: string; subtitle?: string; status?: { tone: BadgeProps['tone']; label: string }; trailing?: string; onPress?: () => void }
export declare function LessonRow(props: LessonRowProps): React.ReactElement;

export interface ListGroupProps { title?: string; children: React.ReactNode }
export declare function ListGroup(props: ListGroupProps): React.ReactElement;

export interface ListRowProps { title: string; subtitle?: string; icon?: IconName; iconTone?: 'secondary' | 'danger' | 'success' | 'accent' | 'primary'; trailing?: 'chevron' | 'switch' | 'none'; switchValue?: boolean; badge?: { tone: BadgeProps['tone']; label: string }; onPress?: () => void }
export declare function ListRow(props: ListRowProps): React.ReactElement;

export interface AlertBannerProps { tone?: 'danger' | 'accent' | 'success' | 'secondary'; icon?: IconName; message: string; value?: string; onPress?: () => void }
export declare function AlertBanner(props: AlertBannerProps): React.ReactElement;

export interface ScreenHeaderProps { variant?: 'hero' | 'tab' | 'push' | 'modal'; title: string; eyebrow?: string; actions?: { icon: IconName; label: string; variant?: IconButtonProps['variant']; onPress?: () => void }[]; action?: React.ReactNode; onBack?: () => void }
export declare function ScreenHeader(props: ScreenHeaderProps): React.ReactElement;

export interface TabBarProps { active?: 'today' | 'schedule' | 'students' | 'places' | 'earnings' }
export declare function TabBar(props: TabBarProps): React.ReactElement;

export interface WeekStripProps { days: { weekday: string; day: number; dot?: Tag; a11y?: string }[]; selected?: number; label?: string; onSelect?: (index: number) => void }
export declare function WeekStrip(props: WeekStripProps): React.ReactElement;

export interface AgendaBlockProps { tag: Tag; title: string; subtitle?: string; state?: 'default' | 'done' | 'next'; height?: number; onPress?: () => void }
export declare function AgendaBlock(props: AgendaBlockProps): React.ReactElement;

export interface BottomSheetProps { title: string; subtitle?: string; actions?: React.ReactNode; children?: React.ReactNode }
export declare function BottomSheet(props: BottomSheetProps): React.ReactElement;

export interface EmptyStateProps { icon?: IconName; title: string; body?: string; action?: React.ReactNode }
export declare function EmptyState(props: EmptyStateProps): React.ReactElement;

export interface TimerRingProps { elapsed: string; progress: number; caption?: string; label?: string }
export declare function TimerRing(props: TimerRingProps): React.ReactElement;

export declare function formatMoney(cents: number, currency?: Currency, locale?: string, compact?: boolean): string;

declare global { interface Window { Tutora: { Icon: typeof Icon; Button: typeof Button; IconButton: typeof IconButton; Fab: typeof Fab; SegmentedControl: typeof SegmentedControl; Chip: typeof Chip; Switch: typeof Switch; Checkbox: typeof Checkbox; Stepper: typeof Stepper; TextField: typeof TextField; SelectField: typeof SelectField; Badge: typeof Badge; Avatar: typeof Avatar; TagDot: typeof TagDot; Money: typeof Money; ProgressBar: typeof ProgressBar; Card: typeof Card; EarningsSummary: typeof EarningsSummary; SectionHeader: typeof SectionHeader; LessonRow: typeof LessonRow; ListGroup: typeof ListGroup; ListRow: typeof ListRow; AlertBanner: typeof AlertBanner; ScreenHeader: typeof ScreenHeader; TabBar: typeof TabBar; WeekStrip: typeof WeekStrip; AgendaBlock: typeof AgendaBlock; BottomSheet: typeof BottomSheet; EmptyState: typeof EmptyState; TimerRing: typeof TimerRing; formatMoney: typeof formatMoney } } }
