import { Stack, useLocalSearchParams, useRouter } from 'expo-router';
import { useTranslation } from 'react-i18next';
import { View } from 'react-native';

import { resolvePrice } from '@/domain/pricing';
import { usePlaces, useStudent } from '@/state/catalog';
import { initials, placeName, rateText, useLanguage } from '@/ui/catalog';
import { Avatar } from '@/ui/components/Avatar';
import { Button } from '@/ui/components/Button';
import { Card } from '@/ui/components/Card';
import { ListGroup } from '@/ui/components/ListGroup';
import { ListRow } from '@/ui/components/ListRow';
import { Screen } from '@/ui/components/Screen';
import { Text } from '@/ui/components/Text';

export default function StudentDetail() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { t } = useTranslation();
  const language = useLanguage();
  const router = useRouter();
  const { data: student } = useStudent(id);
  const { data: places = [] } = usePlaces();
  if (!student) return <Screen testID="student-detail">{null}</Screen>;
  const placeById = new Map(places.map((p) => [p.id, p]));

  return (
    <Screen testID="student-detail">
      <Stack.Screen
        options={{
          title: t('students.detail'),
          headerRight: () => (
            <Button
              variant="ghost"
              size="sm"
              onPress={() => router.push(`/catalog/student/edit?id=${student.id}`)}
            >
              {t('catalog.edit')}
            </Button>
          ),
        }}
      />
      <Card>
        <View className="flex-row items-center gap-3">
          <Avatar initials={initials(student.name)} tag={student.color} size="lg" />
          <View className="min-w-0 flex-1">
            <Text variant="title-lg">{student.name}</Text>
            <Text variant="meta" className="text-ink-muted">
              {[student.subject, student.phone].filter(Boolean).join(' · ')}
            </Text>
          </View>
        </View>
      </Card>
      <Card label={t('students.rate')}>
        <Text variant="caption" className="text-ink-muted">
          {t('students.rate')}
        </Text>
        <Text variant="body-strong">
          {t(student.rateType === 'hour' ? 'students.perHour' : 'students.perLesson')}
        </Text>
        {student.notes ? (
          <Text variant="body" className="text-ink-muted">
            {student.notes}
          </Text>
        ) : null}
      </Card>
      <ListGroup title={t('students.places')}>
        {student.links.length === 0 ? (
          <ListRow title={t('students.noPlace')} trailing="none" />
        ) : (
          student.links.map((link) => {
            const place = placeById.get(link.placeId);
            if (!place) return null;
            const price = resolvePrice(place, { student, link });
            return (
              <ListRow
                key={link.placeId}
                title={placeName(place, t)}
                icon={link.isDefault ? 'star' : 'pin'}
                iconTone={link.isDefault ? 'accent' : 'secondary'}
                subtitle={
                  price.kind === 'set'
                    ? rateText(price.cents, price.rateType, t, language)
                    : t(price.kind === 'salary' ? 'groups.coveredBySalary' : 'places.noPrice')
                }
                onPress={() => router.push(`/catalog/place/${place.id}`)}
              />
            );
          })
        )}
      </ListGroup>
    </Screen>
  );
}
