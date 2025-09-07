import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, FlatList, RefreshControl} from 'react-native';
import {
  Card,
  Title,
  Button,
  Surface,
  Text,
  IconButton,
  Chip,
  Searchbar,
  Portal,
  Modal,
  RadioButton,
  ActivityIndicator,
  FAB,
} from 'react-native-paper';
import {TabNavigationProps} from '../types/Navigation';
import {
  ScanHistoryEntry,
  TagReadResult,
  TagFormat,
} from '../types/FilamentInfo';

interface ScanHistoryScreenProps extends TabNavigationProps {}

type FilterType = 'all' | 'success' | 'error' | 'invalid';
type SortType = 'date' | 'result' | 'type';

const ScanHistoryScreen: React.FC<ScanHistoryScreenProps> = ({navigation}) => {
  const [scanHistory, setScanHistory] = useState<ScanHistoryEntry[]>([]);
  const [filteredHistory, setFilteredHistory] = useState<ScanHistoryEntry[]>(
    [],
  );
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [sortType, setSortType] = useState<SortType>('date');
  const [sortAscending, setSortAscending] = useState<boolean>(false);
  const [showSortModal, setShowSortModal] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);

  useEffect(() => {
    loadScanHistory();
  }, []);

  const filterAndSortHistory = useCallback(() => {
    let filtered = [...scanHistory];

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        scan =>
          scan.filamentInfo?.manufacturerName.toLowerCase().includes(query) ||
          scan.filamentInfo?.filamentType.toLowerCase().includes(query) ||
          scan.filamentInfo?.colorName.toLowerCase().includes(query) ||
          scan.error?.toLowerCase().includes(query),
      );
    }

    // Apply result filter
    if (filterType !== 'all') {
      switch (filterType) {
        case 'success':
          filtered = filtered.filter(scan => scan.result === 'SUCCESS');
          break;
        case 'error':
          filtered = filtered.filter(
            scan =>
              scan.result === 'READ_ERROR' ||
              scan.result === 'PARSING_ERROR' ||
              scan.result === 'AUTHENTICATION_FAILED',
          );
          break;
        case 'invalid':
          filtered = filtered.filter(
            scan => scan.result === 'INVALID_TAG' || scan.result === 'NO_NFC',
          );
          break;
      }
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let result = 0;
      switch (sortType) {
        case 'date':
          result = a.timestamp - b.timestamp;
          break;
        case 'result':
          result = a.result.localeCompare(b.result);
          break;
        case 'type':
          const aType = a.filamentInfo?.filamentType || '';
          const bType = b.filamentInfo?.filamentType || '';
          result = aType.localeCompare(bType);
          break;
      }
      return sortAscending ? result : -result;
    });

    setFilteredHistory(filtered);
  }, [scanHistory, searchQuery, filterType, sortType, sortAscending]);

  useEffect(() => {
    filterAndSortHistory();
  }, [filterAndSortHistory]);

  const loadScanHistory = async (isRefresh = false) => {
    if (isRefresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }

    // Simulate loading scan history data
    setTimeout(() => {
      const mockHistory: ScanHistoryEntry[] = [
        {
          id: 'scan-1',
          timestamp: Date.now() - 1000 * 60 * 30, // 30 minutes ago
          result: 'SUCCESS',
          filamentInfo: {
            tagUid: 'AB:CD:EF:12:34:56',
            trayUid: 'TR001',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'Bambu Lab',
            filamentType: 'PLA Basic',
            detailedFilamentType: 'PLA Basic',
            colorHex: '#FF6600',
            colorName: 'Orange',
            spoolWeight: 250,
            filamentDiameter: 1.75,
            filamentLength: 330000,
            productionDate: '2024-01',
            minTemperature: 190,
            maxTemperature: 220,
            bedTemperature: 60,
            dryingTemperature: 45,
            dryingTime: 4,
            materialVariantId: 'BL-PLA-001',
            materialId: 'PLA-BASIC',
            nozzleDiameter: 0.4,
            spoolWidth: 70,
            bedTemperatureType: 1,
            shortProductionDate: '24-01',
            colorCount: 1,
            shortProductionDateHex: '1801',
            unknownBlock17Hex: '0000000000000000',
          },
        },
        {
          id: 'scan-2',
          timestamp: Date.now() - 1000 * 60 * 60 * 2, // 2 hours ago
          result: 'SUCCESS',
          filamentInfo: {
            tagUid: 'CD:EF:12:34:56:78',
            trayUid: 'TR002',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'PolyMaker',
            filamentType: 'PETG Carbon Fiber',
            detailedFilamentType: 'PETG Carbon Fiber',
            colorHex: '#2C2C2C',
            colorName: 'Carbon Black',
            spoolWeight: 300,
            filamentDiameter: 1.75,
            filamentLength: 400000,
            productionDate: '2024-02',
            minTemperature: 230,
            maxTemperature: 250,
            bedTemperature: 80,
            dryingTemperature: 65,
            dryingTime: 8,
            materialVariantId: 'PM-PETG-CF-001',
            materialId: 'PETG-CF',
            nozzleDiameter: 0.4,
            spoolWidth: 75,
            bedTemperatureType: 2,
            shortProductionDate: '24-02',
            colorCount: 1,
            shortProductionDateHex: '1802',
            unknownBlock17Hex: '0000000000000001',
          },
        },
        {
          id: 'scan-3',
          timestamp: Date.now() - 1000 * 60 * 60 * 6, // 6 hours ago
          result: 'READ_ERROR',
          error: 'Failed to authenticate with tag',
        },
        {
          id: 'scan-4',
          timestamp: Date.now() - 1000 * 60 * 60 * 24, // 1 day ago
          result: 'INVALID_TAG',
        },
        {
          id: 'scan-5',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 2, // 2 days ago
          result: 'SUCCESS',
          filamentInfo: {
            tagUid: 'EF:12:34:56:78:9A',
            trayUid: 'TR003',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'Hatchbox',
            filamentType: 'ABS',
            detailedFilamentType: 'ABS Premium',
            colorHex: '#FFFFFF',
            colorName: 'White',
            spoolWeight: 250,
            filamentDiameter: 1.75,
            filamentLength: 280000,
            productionDate: '2024-01',
            minTemperature: 230,
            maxTemperature: 260,
            bedTemperature: 100,
            dryingTemperature: 60,
            dryingTime: 6,
            materialVariantId: 'HB-ABS-001',
            materialId: 'ABS-PREMIUM',
            nozzleDiameter: 0.4,
            spoolWidth: 70,
            bedTemperatureType: 3,
            shortProductionDate: '24-01',
            colorCount: 1,
            shortProductionDateHex: '1801',
            unknownBlock17Hex: '0000000000000002',
          },
        },
        {
          id: 'scan-6',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 3, // 3 days ago
          result: 'NO_NFC',
        },
        {
          id: 'scan-7',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 5, // 5 days ago
          result: 'PARSING_ERROR',
          error: 'Invalid data format in tag sector 4',
        },
      ];

      setScanHistory(mockHistory);
      setIsLoading(false);
      setIsRefreshing(false);
    }, 500);
  };

  const getResultIcon = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return 'check-circle';
      case 'READ_ERROR':
      case 'PARSING_ERROR':
      case 'AUTHENTICATION_FAILED':
        return 'alert-circle';
      case 'INVALID_TAG':
        return 'close-circle';
      case 'NO_NFC':
        return 'nfc-off';
      default:
        return 'help-circle';
    }
  };

  const getResultColor = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return '#4CAF50';
      case 'READ_ERROR':
      case 'PARSING_ERROR':
      case 'AUTHENTICATION_FAILED':
        return '#FF9800';
      case 'INVALID_TAG':
      case 'NO_NFC':
        return '#F44336';
      default:
        return '#757575';
    }
  };

  const getResultLabel = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return 'Success';
      case 'READ_ERROR':
        return 'Read Error';
      case 'PARSING_ERROR':
        return 'Parse Error';
      case 'AUTHENTICATION_FAILED':
        return 'Auth Failed';
      case 'INVALID_TAG':
        return 'Invalid Tag';
      case 'NO_NFC':
        return 'No NFC';
      default:
        return 'Unknown';
    }
  };

  const handleScanPress = (scan: ScanHistoryEntry) => {
    navigation.navigate('ScanDetail', {scanId: scan.id});
  };

  // const _handleClearHistory = () => {
  //   // TODO: Show confirmation dialog and clear history
  //   setScanHistory([]);
  // };

  const renderScanItem = ({item}: {item: ScanHistoryEntry}) => (
    <Card style={styles.scanCard} onPress={() => handleScanPress(item)}>
      <Card.Content>
        <View style={styles.scanHeader}>
          <View style={styles.scanInfo}>
            <View style={styles.titleRow}>
              <IconButton
                icon={getResultIcon(item.result)}
                iconColor={getResultColor(item.result)}
                size={20}
                style={styles.resultIcon}
              />
              <View style={styles.scanTitle}>
                {item.filamentInfo ? (
                  <>
                    <Text style={styles.filamentName}>
                      {item.filamentInfo.filamentType}
                    </Text>
                    <Text style={styles.manufacturer}>
                      {item.filamentInfo.manufacturerName}
                    </Text>
                  </>
                ) : (
                  <>
                    <Text style={styles.filamentName}>
                      {getResultLabel(item.result)}
                    </Text>
                    {item.error && (
                      <Text style={styles.errorText} numberOfLines={1}>
                        {item.error}
                      </Text>
                    )}
                  </>
                )}
              </View>
            </View>

            <View style={styles.scanMetadata}>
              <Chip
                icon={getResultIcon(item.result)}
                style={[
                  styles.resultChip,
                  {backgroundColor: getResultColor(item.result) + '20'},
                ]}
                textStyle={{color: getResultColor(item.result)}}
                compact>
                {getResultLabel(item.result)}
              </Chip>

              {item.filamentInfo && (
                <View style={styles.colorIndicator}>
                  <View
                    style={[
                      styles.colorSwatch,
                      {backgroundColor: item.filamentInfo.colorHex},
                    ]}
                  />
                  <Text style={styles.colorName}>
                    {item.filamentInfo.colorName}
                  </Text>
                </View>
              )}
            </View>
          </View>

          <View style={styles.scanTime}>
            <Text style={styles.timeText}>
              {new Date(item.timestamp).toLocaleTimeString([], {
                hour: '2-digit',
                minute: '2-digit',
              })}
            </Text>
            <Text style={styles.dateText}>
              {new Date(item.timestamp).toLocaleDateString()}
            </Text>
          </View>
        </View>
      </Card.Content>
    </Card>
  );

  const renderSortModal = () => (
    <Portal>
      <Modal
        visible={showSortModal}
        onDismiss={() => setShowSortModal(false)}
        contentContainerStyle={styles.modalContent}>
        <Title style={styles.modalTitle}>Sort Options</Title>

        <Text style={styles.sectionHeader}>Sort by:</Text>
        <RadioButton.Group
          onValueChange={value => setSortType(value as SortType)}
          value={sortType}>
          <View style={styles.radioItem}>
            <RadioButton value="date" />
            <Text style={styles.radioLabel}>Date</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="result" />
            <Text style={styles.radioLabel}>Result</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="type" />
            <Text style={styles.radioLabel}>Filament Type</Text>
          </View>
        </RadioButton.Group>

        <Text style={styles.sectionHeader}>Order:</Text>
        <RadioButton.Group
          onValueChange={value => setSortAscending(value === 'asc')}
          value={sortAscending ? 'asc' : 'desc'}>
          <View style={styles.radioItem}>
            <RadioButton value="desc" />
            <Text style={styles.radioLabel}>Newest First</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="asc" />
            <Text style={styles.radioLabel}>Oldest First</Text>
          </View>
        </RadioButton.Group>

        <Button
          mode="contained"
          onPress={() => setShowSortModal(false)}
          style={styles.modalButton}>
          Apply
        </Button>
      </Modal>
    </Portal>
  );

  const renderHeader = () => (
    <Surface style={styles.header} elevation={2}>
      <Searchbar
        placeholder="Search scan history..."
        onChangeText={setSearchQuery}
        value={searchQuery}
        style={styles.searchbar}
      />

      <View style={styles.controls}>
        <View style={styles.filterChips}>
          <Chip
            selected={filterType === 'all'}
            onPress={() => setFilterType('all')}
            style={styles.filterChip}>
            All ({scanHistory.length})
          </Chip>
          <Chip
            selected={filterType === 'success'}
            onPress={() => setFilterType('success')}
            style={styles.filterChip}>
            Success ({scanHistory.filter(s => s.result === 'SUCCESS').length})
          </Chip>
          <Chip
            selected={filterType === 'error'}
            onPress={() => setFilterType('error')}
            style={styles.filterChip}>
            Errors (
            {
              scanHistory.filter(
                s =>
                  s.result.includes('ERROR') ||
                  s.result === 'AUTHENTICATION_FAILED',
              ).length
            }
            )
          </Chip>
          <Chip
            selected={filterType === 'invalid'}
            onPress={() => setFilterType('invalid')}
            style={styles.filterChip}>
            Invalid (
            {
              scanHistory.filter(
                s => s.result === 'INVALID_TAG' || s.result === 'NO_NFC',
              ).length
            }
            )
          </Chip>
        </View>

        <IconButton
          icon="sort"
          size={24}
          onPress={() => setShowSortModal(true)}
        />
      </View>

      <View style={styles.resultsInfo}>
        <Text style={styles.resultsText}>
          {filteredHistory.length} of {scanHistory.length} scans
        </Text>
        <Text style={styles.sortText}>
          Sorted by {sortType} ({sortAscending ? 'oldest' : 'newest'} first)
        </Text>
      </View>
    </Surface>
  );

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6200EE" />
        <Text style={styles.loadingText}>Loading scan history...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {renderHeader()}

      <FlatList
        data={filteredHistory}
        renderItem={renderScanItem}
        keyExtractor={item => item.id}
        style={styles.list}
        contentContainerStyle={styles.listContent}
        refreshControl={
          <RefreshControl
            refreshing={isRefreshing}
            onRefresh={() => loadScanHistory(true)}
          />
        }
        ListEmptyComponent={
          <View style={styles.emptyContainer}>
            <IconButton icon="history" size={48} iconColor="#ccc" />
            <Text style={styles.emptyText}>No scan history found</Text>
            <Text style={styles.emptySubtext}>
              {searchQuery || filterType !== 'all'
                ? 'Try adjusting your search or filters'
                : 'Start scanning NFC tags to see history here'}
            </Text>
          </View>
        }
      />

      {renderSortModal()}

      <FAB
        style={styles.fab}
        icon="nfc-tap"
        label="New Scan"
        onPress={() => navigation.navigate('Scanning', {})}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    padding: 16,
    marginBottom: 8,
  },
  searchbar: {
    marginBottom: 16,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 12,
  },
  filterChips: {
    flexDirection: 'row',
    flex: 1,
  },
  filterChip: {
    marginRight: 8,
  },
  resultsInfo: {
    alignItems: 'center',
  },
  resultsText: {
    fontSize: 14,
    fontWeight: '600',
  },
  sortText: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  list: {
    flex: 1,
  },
  listContent: {
    paddingHorizontal: 16,
    paddingBottom: 80,
  },
  scanCard: {
    marginBottom: 8,
  },
  scanHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  scanInfo: {
    flex: 1,
  },
  titleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 8,
  },
  resultIcon: {
    margin: 0,
    marginRight: 8,
  },
  scanTitle: {
    flex: 1,
  },
  filamentName: {
    fontSize: 16,
    fontWeight: '600',
    color: '#333',
  },
  manufacturer: {
    fontSize: 14,
    color: '#666',
  },
  errorText: {
    fontSize: 12,
    color: '#F44336',
    fontStyle: 'italic',
  },
  scanMetadata: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  resultChip: {
    alignSelf: 'flex-start',
  },
  colorIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  colorSwatch: {
    width: 12,
    height: 12,
    borderRadius: 6,
    marginRight: 6,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  colorName: {
    fontSize: 12,
    color: '#666',
  },
  scanTime: {
    alignItems: 'flex-end',
    marginLeft: 16,
  },
  timeText: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  dateText: {
    fontSize: 12,
    color: '#666',
    marginTop: 2,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  emptyContainer: {
    alignItems: 'center',
    paddingVertical: 48,
  },
  emptyText: {
    fontSize: 18,
    color: '#666',
    marginTop: 16,
  },
  emptySubtext: {
    fontSize: 14,
    color: '#999',
    textAlign: 'center',
    marginTop: 8,
    paddingHorizontal: 32,
  },
  modalContent: {
    backgroundColor: 'white',
    padding: 20,
    margin: 20,
    borderRadius: 12,
  },
  modalTitle: {
    textAlign: 'center',
    marginBottom: 16,
  },
  sectionHeader: {
    fontSize: 16,
    fontWeight: '600',
    marginTop: 16,
    marginBottom: 8,
  },
  radioItem: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
  },
  radioLabel: {
    marginLeft: 8,
    flex: 1,
  },
  modalButton: {
    marginTop: 16,
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#6200EE',
  },
});

export default ScanHistoryScreen;
