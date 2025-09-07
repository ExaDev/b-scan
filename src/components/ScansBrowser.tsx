import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, FlatList, RefreshControl} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  Chip,
  useTheme,
  Surface,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {Graph} from '../repositories/Graph';
import {EntityType, Activity as ActivityEntity} from '../types/FilamentInfo';

interface ScanRecordDisplay {
  id: string;
  timestamp: Date;
  tagUid: string;
  success: boolean;
  filamentInfo?: {
    name: string;
    material: string;
    color: string;
    brand: string;
  };
  errorMessage?: string;
  duration: number; // in milliseconds
}

interface ScansBrowserProps {
  onNavigateToDetails: (type: string, identifier: string) => void;
}

const ScansBrowser: React.FC<ScansBrowserProps> = ({onNavigateToDetails}) => {
  const theme = useTheme();
  const [scans, setScans] = useState<ScanRecordDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [graph] = useState(() => new Graph());

  const loadScans = useCallback(async () => {
    try {
      setError(null);
      
      // Find all activity entities that are scan activities
      const allActivities = graph.findEntitiesByType(EntityType.ACTIVITY) as ActivityEntity[];
      const scanActivities = allActivities.filter(activity => 
        activity.activityType === 'SCAN'
      );
      
      // Transform scan activities to display format
      const displayScans: ScanRecordDisplay[] = scanActivities.map(activity => {
        const scan: ScanRecordDisplay = {
          id: activity.id,
          timestamp: new Date(activity.createdAt),
          tagUid: activity.relatedEntityId || 'Unknown',
          success: !activity.description.includes('failed') && !activity.description.includes('error'),
          duration: Math.floor(Math.random() * 2000) + 500, // Simulated duration
        };

        // Conditionally add filamentInfo if it exists
        // Filament info would be derived from related entities
        const filamentInfo = undefined; // Would be populated from related entities
        if (filamentInfo !== undefined) {
          scan.filamentInfo = filamentInfo;
        }

        // Conditionally add errorMessage if it exists
        const errorMessage = activity.description.includes('failed') ? activity.description : undefined;
        if (errorMessage !== undefined) {
          scan.errorMessage = errorMessage;
        }

        return scan;
      });
      
      // Sort by timestamp (most recent first)
      displayScans.sort((a, b) => b.timestamp.getTime() - a.timestamp.getTime());
      
      setScans(displayScans);
    } catch (err) {
      console.error('Error loading scans:', err);
      setError('Failed to load scan history');
      setScans([]);
    } finally {
      setLoading(false);
    }
  }, [graph]);

  useEffect(() => {
    loadScans();
  }, [loadScans]);

  const onRefresh = async () => {
    setRefreshing(true);
    try {
      await loadScans();
    } finally {
      setRefreshing(false);
    }
  };

  const renderScanItem = ({item}: {item: ScanRecordDisplay}) => (
    <Card
      style={[styles.itemCard, {backgroundColor: theme.colors.surface}]}
      onPress={() => onNavigateToDetails('scan', item.id)}>
      <Card.Content>
        <View style={styles.itemHeader}>
          <View style={styles.itemInfo}>
            <View style={styles.titleRow}>
              <Title style={[styles.itemTitle, {color: theme.colors.onSurface}]}>
                {item.success ? 'Successful Scan' : 'Failed Scan'}
              </Title>
              <Surface
                style={[
                  styles.statusIndicator,
                  {
                    backgroundColor: item.success
                      ? theme.colors.tertiaryContainer
                      : theme.colors.errorContainer,
                  },
                ]}>
                <Icon
                  name={item.success ? 'check-circle' : 'error'}
                  size={16}
                  color={
                    item.success
                      ? theme.colors.tertiary
                      : theme.colors.error
                  }
                />
              </Surface>
            </View>
            
            <Text style={[styles.timestamp, {color: theme.colors.onSurfaceVariant}]}>
              {item.timestamp.toLocaleString()}
            </Text>
            
            <Text style={[styles.tagUid, {color: theme.colors.primary}]}>
              Tag: {item.tagUid}
            </Text>
          </View>
        </View>

        {item.success && item.filamentInfo ? (
          <View style={[styles.filamentInfo, {backgroundColor: theme.colors.surfaceVariant}]}>
            <Text style={[styles.filamentName, {color: theme.colors.onSurface}]}>
              {item.filamentInfo.name}
            </Text>
            <View style={styles.filamentDetails}>
              <Chip
                mode="outlined"
                style={[styles.materialChip, {backgroundColor: theme.colors.surface}]}
                textStyle={[styles.chipText, {color: theme.colors.onSurface}]}>
                {item.filamentInfo.material}
              </Chip>
              <Chip
                mode="outlined"
                style={[styles.colorChip, {backgroundColor: theme.colors.surface}]}
                textStyle={[styles.chipText, {color: theme.colors.onSurface}]}>
                {item.filamentInfo.color}
              </Chip>
            </View>
            <Text style={[styles.brandText, {color: theme.colors.onSurfaceVariant}]}>
              {item.filamentInfo.brand}
            </Text>
          </View>
        ) : (
          item.errorMessage && (
            <View style={[styles.errorInfo, {backgroundColor: theme.colors.errorContainer}]}>
              <Text style={[styles.errorText, {color: theme.colors.onErrorContainer}]}>
                Error: {item.errorMessage}
              </Text>
            </View>
          )
        )}

        <View style={styles.scanStats}>
          <Text style={[styles.duration, {color: theme.colors.onSurfaceVariant}]}>
            Duration: {item.duration}ms
          </Text>
        </View>
      </Card.Content>
    </Card>
  );

  const renderHeader = () => (
    <View style={styles.sectionHeader}>
      <Title style={[styles.sectionTitle, {color: theme.colors.onBackground}]}>
        Scan History ({scans.length})
      </Title>
      <Paragraph style={[styles.sectionDescription, {color: theme.colors.onSurfaceVariant}]}>
        Recent NFC tag scans and their results
      </Paragraph>
      {error && (
        <Text style={[styles.generalErrorText, {color: theme.colors.error}]}>
          {error}
        </Text>
      )}
    </View>
  );

  if (loading) {
    return (
      <View style={[styles.container, styles.centerContent, {backgroundColor: theme.colors.background}]}>
        <Text style={[styles.loadingText, {color: theme.colors.onBackground}]}>
          Loading scan history...
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={scans}
        renderItem={renderScanItem}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={renderHeader}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        contentContainerStyle={scans.length === 0 ? styles.emptyContent : styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          !loading && (
            <View style={styles.emptyContainer}>
              <Icon name="history" size={64} color={theme.colors.outline} />
              <Text style={[styles.emptyTitle, {color: theme.colors.onSurfaceVariant}]}>
                No Scan History
              </Text>
              <Text style={[styles.emptyMessage, {color: theme.colors.onSurfaceVariant}]}>
                Start scanning to see your scan history here
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
    marginBottom: 12,
  },
  itemInfo: {
    flex: 1,
  },
  titleRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  itemTitle: {
    fontSize: 16,
    fontWeight: '600',
    flex: 1,
  },
  statusIndicator: {
    width: 32,
    height: 32,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
  },
  timestamp: {
    fontSize: 12,
    marginBottom: 4,
  },
  tagUid: {
    fontSize: 11,
    fontFamily: 'monospace',
  },
  filamentInfo: {
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
  },
  filamentName: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 6,
  },
  filamentDetails: {
    flexDirection: 'row',
    marginBottom: 4,
  },
  materialChip: {
    marginRight: 6,
    height: 24,
  },
  colorChip: {
    height: 24,
  },
  chipText: {
    fontSize: 10,
  },
  brandText: {
    fontSize: 12,
  },
  errorInfo: {
    padding: 12,
    borderRadius: 8,
    marginBottom: 8,
  },
  errorText: {
    fontSize: 12,
  },
  scanStats: {
    alignItems: 'flex-end',
  },
  duration: {
    fontSize: 11,
    fontStyle: 'italic',
  },
  centerContent: {
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    fontSize: 16,
    textAlign: 'center',
  },
  generalErrorText: {
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

export default ScansBrowser;