import React from 'react';
import {View, StyleSheet, FlatList} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  useTheme,
  Surface,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';

interface TagInfo {
  id: string;
  uid: string;
  type: string;
  lastScanned: Date;
  scanCount: number;
  associatedSpool?: {
    name: string;
    material: string;
    color: string;
  };
}

interface TagsBrowserProps {
  onNavigateToDetails: (type: string, identifier: string) => void;
}

const TagsBrowser: React.FC<TagsBrowserProps> = ({onNavigateToDetails}) => {
  const theme = useTheme();

  const tags: TagInfo[] = [
    {
      id: '1',
      uid: '04:A3:22:B2:C4:80',
      type: 'MIFARE Classic 1K',
      lastScanned: new Date(),
      scanCount: 15,
      associatedSpool: {
        name: 'PLA Basic',
        material: 'PLA',
        color: 'Orange',
      },
    },
    {
      id: '2',
      uid: '04:B1:55:F3:D2:90',
      type: 'MIFARE Classic 1K',
      lastScanned: new Date(Date.now() - 86400000),
      scanCount: 8,
      associatedSpool: {
        name: 'PETG Tough',
        material: 'PETG',
        color: 'Black',
      },
    },
    {
      id: '3',
      uid: '04:C2:88:A1:E5:70',
      type: 'MIFARE Classic 1K',
      lastScanned: new Date(Date.now() - 259200000),
      scanCount: 3,
    },
  ];

  const renderTagItem = ({item}: {item: TagInfo}) => (
    <Card
      style={[styles.itemCard, {backgroundColor: theme.colors.surface}]}
      onPress={() => onNavigateToDetails('tag', item.uid)}>
      <Card.Content>
        <View style={styles.itemHeader}>
          <View style={styles.itemInfo}>
            <Title style={[styles.itemTitle, {color: theme.colors.onSurface}]}>
              NFC Tag
            </Title>
            <Text style={[styles.uid, {color: theme.colors.primary}]}>
              {item.uid}
            </Text>
            <Paragraph style={[styles.tagType, {color: theme.colors.onSurfaceVariant}]}>
              {item.type}
            </Paragraph>
          </View>
          <Surface style={[styles.iconContainer, {backgroundColor: theme.colors.primaryContainer}]}>
            <Icon name="nfc" size={24} color={theme.colors.primary} />
          </Surface>
        </View>

        {item.associatedSpool && (
          <View style={[styles.spoolInfo, {backgroundColor: theme.colors.surfaceVariant}]}>
            <Text style={[styles.spoolLabel, {color: theme.colors.onSurfaceVariant}]}>
              Associated Spool:
            </Text>
            <Text style={[styles.spoolName, {color: theme.colors.onSurface}]}>
              {item.associatedSpool.name} - {item.associatedSpool.color}
            </Text>
            <Text style={[styles.spoolMaterial, {color: theme.colors.onSurfaceVariant}]}>
              {item.associatedSpool.material}
            </Text>
          </View>
        )}

        <View style={styles.statsContainer}>
          <View style={styles.stat}>
            <Text style={[styles.statValue, {color: theme.colors.primary}]}>
              {item.scanCount}
            </Text>
            <Text style={[styles.statLabel, {color: theme.colors.onSurfaceVariant}]}>
              Scans
            </Text>
          </View>
          <View style={styles.stat}>
            <Text style={[styles.statValue, {color: theme.colors.primary}]}>
              {item.lastScanned.toLocaleDateString()}
            </Text>
            <Text style={[styles.statLabel, {color: theme.colors.onSurfaceVariant}]}>
              Last Scan
            </Text>
          </View>
        </View>
      </Card.Content>
    </Card>
  );

  const renderHeader = () => (
    <View style={styles.sectionHeader}>
      <Title style={[styles.sectionTitle, {color: theme.colors.onBackground}]}>
        NFC Tags ({tags.length})
      </Title>
      <Paragraph style={[styles.sectionDescription, {color: theme.colors.onSurfaceVariant}]}>
        Discovered NFC tags and their associated data
      </Paragraph>
    </View>
  );

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={tags}
        renderItem={renderTagItem}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={renderHeader}
        contentContainerStyle={styles.listContent}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  listContent: {
    paddingBottom: 80,
  },
  sectionHeader: {
    paddingHorizontal: 16,
    paddingVertical: 16,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  sectionDescription: {
    fontSize: 14,
    marginTop: 4,
  },
  itemCard: {
    marginHorizontal: 16,
    marginVertical: 4,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 12,
  },
  itemInfo: {
    flex: 1,
  },
  itemTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 4,
  },
  uid: {
    fontSize: 12,
    fontFamily: 'monospace',
    marginBottom: 2,
  },
  tagType: {
    fontSize: 11,
  },
  iconContainer: {
    width: 48,
    height: 48,
    borderRadius: 24,
    alignItems: 'center',
    justifyContent: 'center',
  },
  spoolInfo: {
    padding: 12,
    borderRadius: 8,
    marginBottom: 12,
  },
  spoolLabel: {
    fontSize: 12,
    marginBottom: 4,
  },
  spoolName: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 2,
  },
  spoolMaterial: {
    fontSize: 12,
  },
  statsContainer: {
    flexDirection: 'row',
    justifyContent: 'space-around',
  },
  stat: {
    alignItems: 'center',
  },
  statValue: {
    fontSize: 16,
    fontWeight: 'bold',
  },
  statLabel: {
    fontSize: 12,
    marginTop: 2,
  },
});

export default TagsBrowser;