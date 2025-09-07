import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, FlatList, RefreshControl} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  useTheme,
  Surface,
} from 'react-native-paper';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import {Graph} from '../repositories/Graph';
import {EntityType, Identifier as IdentifierEntity} from '../types/FilamentInfo';

interface TagInfoDisplay {
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
  const [tags, setTags] = useState<TagInfoDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [graph] = useState(() => new Graph());

  const loadTags = useCallback(async () => {
    try {
      setError(null);
      
      // Find all identifier entities from the graph
      const identifierEntities = graph.findEntitiesByType(EntityType.IDENTIFIER) as IdentifierEntity[];
      
      // Transform identifier entities to display format
      const displayTags: TagInfoDisplay[] = identifierEntities.map(entity => {
        const tag: TagInfoDisplay = {
          id: entity.id,
          uid: entity.value || entity.id,
          type: entity.identifierType === 'RFID' ? 'MIFARE Classic 1K' : 
                entity.identifierType === 'QR_CODE' ? 'QR Code' : 
                entity.identifierType === 'BARCODE' ? 'Barcode' : 'Unknown',
          lastScanned: new Date(entity.updatedAt || entity.createdAt),
          scanCount: 1, // Default scan count
        };

        // Conditionally add associatedSpool if it exists
        // Associated spool information would be derived from relationships
        const associatedSpool = undefined; // Would be populated from relationships
        if (associatedSpool !== undefined) {
          tag.associatedSpool = associatedSpool;
        }

        return tag;
      });
      
      setTags(displayTags);
    } catch (err) {
      console.error('Error loading tags:', err);
      setError('Failed to load tags');
      setTags([]);
    } finally {
      setLoading(false);
    }
  }, [graph]);

  useEffect(() => {
    loadTags();
  }, [loadTags]);

  const onRefresh = async () => {
    setRefreshing(true);
    try {
      await loadTags();
    } finally {
      setRefreshing(false);
    }
  };

  const renderTagItem = ({item}: {item: TagInfoDisplay}) => (
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
            <MaterialIcon name="nfc" size={24} color={theme.colors.primary} />
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
      {error && (
        <Text style={[styles.errorText, {color: theme.colors.error}]}>
          {error}
        </Text>
      )}
    </View>
  );

  if (loading) {
    return (
      <View style={[styles.container, styles.centerContent, {backgroundColor: theme.colors.background}]}>
        <Text style={[styles.loadingText, {color: theme.colors.onBackground}]}>
          Loading tags...
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={tags}
        renderItem={renderTagItem}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={renderHeader}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        contentContainerStyle={tags.length === 0 ? styles.emptyContent : styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          !loading && (
            <View style={styles.emptyContainer}>
              <MaterialIcon name="nfc" size={64} color={theme.colors.outline} />
              <Text style={[styles.emptyTitle, {color: theme.colors.onSurfaceVariant}]}>
                No Tags Found
              </Text>
              <Text style={[styles.emptyMessage, {color: theme.colors.onSurfaceVariant}]}>
                Scan some NFC tags to see them here
              </Text>
            </View>
          )
        }
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
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 16,
    textAlign: 'center',
  },
  errorText: {
    fontSize: 12,
    marginTop: 4,
    textAlign: 'center',
  },
  emptyContent: {
    flex: 1,
    paddingBottom: 80,
  },
  emptyContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    paddingHorizontal: 32,
    paddingVertical: 64,
  },
  emptyTitle: {
    fontSize: 18,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
    textAlign: 'center',
  },
  emptyMessage: {
    fontSize: 14,
    textAlign: 'center',
    lineHeight: 20,
  },
});

export default TagsBrowser;