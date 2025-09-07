import React from 'react';
import {View, StyleSheet, FlatList} from 'react-native';
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

interface ScanRecord {
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

  const scans: ScanRecord[] = [
    {
      id: '1',
      timestamp: new Date(),
      tagUid: '04:A3:22:B2:C4:80',
      success: true,
      filamentInfo: {
        name: 'PLA Basic',
        material: 'PLA',
        color: 'Orange',
        brand: 'Bambu Lab',
      },
      duration: 1250,
    },
    {
      id: '2',
      timestamp: new Date(Date.now() - 3600000),
      tagUid: '04:B1:55:F3:D2:90',
      success: true,
      filamentInfo: {
        name: 'PETG Tough',
        material: 'PETG',
        color: 'Black',
        brand: 'Bambu Lab',
      },
      duration: 980,
    },
    {
      id: '3',
      timestamp: new Date(Date.now() - 86400000),
      tagUid: '04:C2:88:A1:E5:70',
      success: false,
      errorMessage: 'Authentication failed',
      duration: 2100,
    },
    {
      id: '4',
      timestamp: new Date(Date.now() - 172800000),
      tagUid: '04:D3:99:C4:F1:20',
      success: true,
      filamentInfo: {
        name: 'TPU 95A',
        material: 'TPU',
        color: 'Clear',
        brand: 'Bambu Lab',
      },
      duration: 1580,
    },
  ];

  const renderScanItem = ({item}: {item: ScanRecord}) => (
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
                textStyle={{color: theme.colors.onSurface, fontSize: 10}}>
                {item.filamentInfo.material}
              </Chip>
              <Chip
                mode="outlined"
                style={[styles.colorChip, {backgroundColor: theme.colors.surface}]}
                textStyle={{color: theme.colors.onSurface, fontSize: 10}}>
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
    </View>
  );

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={scans}
        renderItem={renderScanItem}
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
});

export default ScansBrowser;