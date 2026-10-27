import { Redirect } from 'expo-router';
import { useState, type ReactNode } from 'react';
import { useTranslation } from 'react-i18next';
import { ScrollView, View } from 'react-native';

import type { TextSize } from '@/domain/settings';
import { themeIds, type ThemeId } from '@/domain/theme/colorSets';
import type { Scheme } from '@/domain/theme/palette';
import {
  AgendaBlock,
  AlertBanner,
  Avatar,
  Badge,
  BottomSheet,
  Button,
  Card,
  Checkbox,
  Chip,
  EarningsSummary,
  EmptyState,
  Fab,
  Icon,
  IconButton,
  LessonRow,
  ListGroup,
  ListRow,
  Money,
  ProgressBar,
  ScreenHeader,
  SectionHeader,
  SegmentedControl,
  SelectField,
  Stepper,
  Switch,
  TabBar,
  TagDot,
  Text,
  TextField,
  TimerRing,
  WeekStrip,
} from '@/ui/components';
import { ThemeProvider } from '@/ui/theme/ThemeProvider';

/** Development builds only, or a test build made with EXPO_PUBLIC_DEV_GALLERY=1. */
function galleryEnabled() {
  return __DEV__ || process.env.EXPO_PUBLIC_DEV_GALLERY === '1';
}

function Section({ name, children }: { name: string; children: ReactNode }) {
  return (
    <View testID={`gallery-${name}`} className="gap-2.5">
      <SectionHeader title={name} />
      {children}
    </View>
  );
}

const tags = ['amber', 'green', 'blue', 'violet', 'orange', 'red', 'teal'] as const;
const week = [
  { weekday: 'Seg', day: 26, dot: 'amber' as const },
  { weekday: 'Ter', day: 27 },
  { weekday: 'Qua', day: 28, dot: 'blue' as const },
  { weekday: 'Qui', day: 29 },
  { weekday: 'Sex', day: 30, dot: 'teal' as const },
  { weekday: 'Sáb', day: 31 },
  { weekday: 'Dom', day: 1 },
];

function PriceField() {
  const [cents, setCents] = useState(12000);
  return <TextField kind="money" label="Valor da aula" cents={cents} onChangeCents={setCents} />;
}

export default function ComponentGallery() {
  const { t } = useTranslation();
  const [theme, setTheme] = useState<ThemeId>('quadro');
  const [mode, setMode] = useState<Scheme>('light');
  const [textSize, setTextSize] = useState<TextSize>('normal');
  if (!galleryEnabled()) return <Redirect href="/today" />;

  return (
    <ThemeProvider theme={theme} mode={mode} textSize={textSize}>
      <View testID="gallery-ground" className="flex-1 bg-background">
        <ScreenHeader variant="tab" title={t('dev.title')} />
        <ScrollView contentContainerClassName="gap-6 px-5 pb-6">
          <View className="gap-2.5">
            <SegmentedControl
              label={t('dev.theme')}
              size="sm"
              value={theme}
              onChange={setTheme}
              options={themeIds.map((id) => ({ value: id, label: t(`themes.${id}`) }))}
            />
            <SegmentedControl
              label={t('dev.mode')}
              size="sm"
              value={mode}
              onChange={setMode}
              options={[
                { value: 'light', label: t('dev.light') },
                { value: 'dark', label: t('dev.dark') },
              ]}
            />
            <SegmentedControl
              label={t('dev.textSize')}
              size="sm"
              value={textSize}
              onChange={setTextSize}
              options={[
                { value: 'normal', label: t('dev.normal') },
                { value: 'large', label: t('dev.large') },
              ]}
            />
            <Text testID="gallery-sample-body">Boa tarde, Victor · R$ 1.234,56</Text>
          </View>

          <Section name="Icon">
            <View className="flex-row flex-wrap gap-3">
              {(
                [
                  'plus',
                  'calendar',
                  'users',
                  'pin',
                  'wallet',
                  'clock',
                  'alert',
                  'gear',
                  'play',
                  'star',
                  'search',
                  'archive',
                ] as const
              ).map((name) => (
                <Icon key={name} name={name} />
              ))}
            </View>
          </Section>
          <Section name="Button">
            {(['primary', 'accent', 'outline', 'ghost', 'danger'] as const).map((variant) => (
              <Button key={variant} variant={variant} icon="check">
                Salvar aula
              </Button>
            ))}
            <Button size="sm">Pequeno</Button>
            <Button size="lg" fullWidth>
              Registrar aula · R$ 180,00
            </Button>
            <Button loading>Gerando PDF</Button>
          </Section>
          <Section name="IconButton">
            <View className="flex-row gap-2 rounded-xl bg-header p-2">
              {(['sunken', 'plain', 'onPrimary', 'accent'] as const).map((variant) => (
                <IconButton key={variant} icon="plus" label={variant} variant={variant} />
              ))}
            </View>
          </Section>
          <Section name="Fab">
            <Fab />
          </Section>
          <Section name="SegmentedControl">
            <SegmentedControl
              label="Período"
              options={[
                { value: 'm', label: 'Mês' },
                { value: 'q', label: 'Trimestre' },
                { value: 'y', label: 'Ano' },
              ]}
            />
          </Section>
          <Section name="Chip">
            <View className="flex-row flex-wrap gap-1.5">
              <Chip label="Online" selected />
              <Chip label="Casa do aluno" />
              <Chip label="Salinha Centro" />
            </View>
          </Section>
          <Section name="Switch">
            <View className="flex-row gap-3">
              <Switch label="Ligado" value />
              <Switch label="Desligado" />
              <Switch label="Bloqueado" disabled />
            </View>
          </Section>
          <Section name="Checkbox">
            <Checkbox label="Incluir pagamentos" checked />
            <Checkbox label="Incluir aulas canceladas" />
          </Section>
          <Section name="Stepper">
            <Stepper value={60} step={15} min={15} max={300} format={(v) => `${v} min`} />
            <Stepper value={500} step={500} size="sm" format={(v) => `R$ ${v / 100},00`} />
          </Section>
          <Section name="TextField">
            <TextField
              label="Nome"
              placeholder="Ex.: Ana Souza"
              helper="Como aparece nos relatórios"
            />
            <TextField
              label="Telefone"
              value="(11) 9"
              error="Número incompleto: faltam 8 dígitos"
            />
            <PriceField />
          </Section>
          <Section name="SelectField">
            <SelectField
              label="Aluno"
              value="Ana Souza"
              sub="Matemática"
              avatar={{ initials: 'AS', tag: 'amber' }}
              actionLabel="Trocar"
            />
            <SelectField label="Data" value="Hoje, 26 out" icon="calendar" />
          </Section>
          <Section name="Badge">
            <View className="flex-row flex-wrap gap-1.5">
              {(['neutral', 'success', 'danger', 'accent', 'secondary', 'violet'] as const).map(
                (tone) => (
                  <Badge key={tone} tone={tone}>
                    {tone}
                  </Badge>
                ),
              )}
            </View>
          </Section>
          <Section name="Avatar">
            <View className="flex-row flex-wrap gap-2">
              {tags.map((tag, i) => (
                <Avatar
                  key={tag}
                  initials="AS"
                  tag={tag}
                  size={(['sm', 'md', 'lg'] as const)[i % 3]}
                />
              ))}
            </View>
          </Section>
          <Section name="TagDot">
            <View className="flex-row gap-2">
              {tags.map((tag) => (
                <TagDot key={tag} tag={tag} />
              ))}
            </View>
          </Section>
          <Section name="Money">
            <Money cents={4737} size="display" tone="earnings" />
            <Money cents={24000} size="xl" tone="earnings" />
            <Money cents={18000} size="lg" />
            <Money cents={-56000} size="md" tone="owed" compact />
            <Money cents={12000} size="sm" tone="paid" showSign />
          </Section>
          <Section name="ProgressBar">
            <ProgressBar value={0.63} label="Ganho" />
            <ProgressBar value={0.7} tone="success" track="danger" label="Recebido" />
            <ProgressBar value={0.4} tone="secondary" track="sunken" thin label="Escola" />
          </Section>
          <Section name="Card">
            <Card label="Card">
              <Text>Próxima aula em 25 min</Text>
            </Card>
            <Card elevation="outline" label="Outline">
              <Text>PDF</Text>
            </Card>
          </Section>
          <Section name="EarningsSummary">
            <EarningsSummary
              title="Ganho hoje"
              earnedCents={24000}
              expectedCents={38000}
              expectedLabel="de R$ 380,00 previstos"
              stats={[
                { label: 'Semana', cents: 118000 },
                { label: 'Outubro', cents: 502000 },
                { label: 'Alunos devem', cents: 56000, tone: 'owed' },
              ]}
            />
          </Section>
          <Section name="SectionHeader">
            <SectionHeader title="Próxima aula" aside="em 25 min" asideTone="primary" />
          </Section>
          <Section name="LessonRow">
            <LessonRow
              time="08:00"
              tag="blue"
              title="9º ano B"
              subtitle="Colégio Santa Clara · 50 min"
              status={{ tone: 'success', label: 'Concluída' }}
            />
            <LessonRow
              time="16:30"
              tag="violet"
              title="Turma ENEM"
              subtitle="Redação · 3 alunos"
              trailing="R$ 180"
            />
          </Section>
          <Section name="ListGroup">
            <ListGroup title="Seus dados">
              <ListRow
                title="Backup e planilha"
                subtitle="Automático, toda semana · último ontem"
                icon="cloud"
              />
              <ListRow
                title="Cronômetro ao vivo"
                subtitle="Desligado"
                icon="clock"
                trailing="switch"
              />
              <ListRow
                title="Ana Souza"
                subtitle="Matemática · Online, Casa do Aluno"
                avatar={{ initials: 'AS', tag: 'violet' }}
              />
            </ListGroup>
          </Section>
          <Section name="ListRow">
            <ListGroup>
              <ListRow
                title="Cancelamentos e faltas"
                subtitle="Cobra 50% se cancelar com menos de 24h"
                icon="alert"
                iconTone="danger"
                badge={{ tone: 'success', label: 'Ligado' }}
                trailing="none"
              />
            </ListGroup>
          </Section>
          <Section name="AlertBanner">
            <AlertBanner message="3 alunos com pagamento pendente" value="R$ 560" />
            <AlertBanner tone="secondary" icon="cloud" message="Backup há 15 dias" />
          </Section>
          <Section name="ScreenHeader">
            <ScreenHeader
              variant="hero"
              eyebrow="Segunda, 26 de outubro"
              title="Boa tarde, Victor"
              actions={[
                { icon: 'gear', label: 'Ajustes' },
                { icon: 'plus', label: 'Nova aula', variant: 'accent' },
              ]}
            />
            <ScreenHeader variant="push" title="Detalhe da aula" onBack={() => {}} />
          </Section>
          <Section name="TabBar">
            <TabBar active="schedule" />
          </Section>
          <Section name="WeekStrip">
            <WeekStrip days={week} selected={0} />
          </Section>
          <Section name="AgendaBlock">
            <AgendaBlock tag="green" title="Ana Souza" subtitle="Matemática" state="done" />
            <AgendaBlock
              tag="amber"
              title="Pedro Lima"
              subtitle="Física"
              state="next"
              height={66}
            />
          </Section>
          <Section name="BottomSheet">
            <View className="h-72 overflow-hidden rounded-2xl">
              <BottomSheet
                title="Gerar relatório"
                subtitle="Ana Souza · outubro"
                actions={
                  <>
                    <Button variant="outline">Cancelar</Button>
                    <Button>Gerar PDF</Button>
                  </>
                }
              />
            </View>
          </Section>
          <Section name="EmptyState">
            <EmptyState icon="users" title="Nenhum aluno ainda" body="Seus alunos aparecem aqui." />
          </Section>
          <Section name="TimerRing">
            <View className="items-center rounded-3xl bg-header py-6">
              <TimerRing elapsed="47:12" progress={0.78} caption="R$ 2,00/min" />
            </View>
          </Section>
        </ScrollView>
      </View>
    </ThemeProvider>
  );
}
