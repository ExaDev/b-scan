import React, {useState, useEffect} from 'react';
import {View, StyleSheet, ScrollView, FlatList} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  Surface,
  Text,
  IconButton,
  Chip,
  Searchbar,
  FAB,
  Portal,
  Modal,
  RadioButton,
} from 'react-native-paper';
import {NavigationProps} from '../types/Navigation';
import {FilamentInfo, EntityType, TagFormat} from '../types/FilamentInfo';

interface DataBrowserScreenProps extends NavigationProps {}

type FilterType = 'all' | 'components' | 'inventory' | 'identifiers';
type SortType = 'date' | 'name' | 'type' | 'quantity';

interface BrowserItem {
  id: string;
  type: EntityType;
  title: string;
  subtitle: string;
  metadata: string;
  createdAt: number;
  filamentInfo?: FilamentInfo;
  quantity?: number;
}

const DataBrowserScreen: React.FC<DataBrowserScreenProps> = ({navigation}) => {
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [filterType, setFilterType] = useState<FilterType>('all');
  const [sortType, setSortType] = useState<SortType>('date');
  const [sortAscending, setSortAscending] = useState<boolean>(false);
  const [_showFilterMenu, _setShowFilterMenu] = useState<boolean>(false);
  const [showSortModal, setShowSortModal] = useState<boolean>(false);
  const [isLoading, setIsLoading] = useState<boolean>(false);
  const [items, setItems] = useState<BrowserItem[]>([]);

  useEffect(() => {
    loadItems();
  }, []);

  const loadItems = async () => {
    setIsLoading(true);
    // Simulate loading mock data
    setTimeout(() => {
      const mockItems: BrowserItem[] = [
        {
          id: '1',
          type: EntityType.PHYSICAL_COMPONENT,
          title: 'PLA Basic - Orange',
          subtitle: 'Bambu Lab',
          metadata: '850g remaining',
          createdAt: Date.now() - 1000 * 60 * 60 * 2, // 2 hours ago
          filamentInfo: {
            tagUid: 'AB:CD:EF:12',
            trayUid: 'TR001',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'Bambu Lab',
            filamentType: 'PLA Basic',
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
          },
        },
        {
          id: '2',
          type: EntityType.PHYSICAL_COMPONENT,
          title: 'PETG Carbon Fiber',
          subtitle: 'PolyMaker',
          metadata: '1200g remaining',
          createdAt: Date.now() - 1000 * 60 * 60 * 24, // 1 day ago
          filamentInfo: {
            tagUid: 'CD:EF:12:34',
            trayUid: 'TR002',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'PolyMaker',
            filamentType: 'PETG Carbon Fiber',
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
          },
        },
        {
          id: '3',
          type: EntityType.INVENTORY_ITEM,
          title: 'TPU Flexible Filament',
          subtitle: 'Overture',
          metadata: '3 spools in stock',
          createdAt: Date.now() - 1000 * 60 * 60 * 24 * 3, // 3 days ago
          quantity: 3,
        },
        {
          id: '4',
          type: EntityType.PHYSICAL_COMPONENT,
          title: 'ABS Premium',
          subtitle: 'Hatchbox',
          metadata: '600g remaining',
          createdAt: Date.now() - 1000 * 60 * 60 * 24 * 5, // 5 days ago
          filamentInfo: {
            tagUid: 'EF:12:34:56',
            trayUid: 'TR003',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'Hatchbox',
            filamentType: 'ABS',
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
          },
        },
      ];
      setItems(mockItems);
      setIsLoading(false);
    }, 500);
  };

  const getFilteredAndSortedItems = (): BrowserItem[] => {
    let filtered = items;

    // Apply search filter
    if (searchQuery.trim()) {
      const query = searchQuery.toLowerCase();
      filtered = filtered.filter(
        item =>
          item.title.toLowerCase().includes(query) ||
          item.subtitle.toLowerCase().includes(query) ||
          item.metadata.toLowerCase().includes(query),
      );
    }

    // Apply type filter
    if (filterType !== 'all') {
      switch (filterType) {
        case 'components':
          filtered = filtered.filter(
            item => item.type === EntityType.PHYSICAL_COMPONENT,
          );
          break;
        case 'inventory':
          filtered = filtered.filter(
            item => item.type === EntityType.INVENTORY_ITEM,
          );
          break;
        case 'identifiers':
          filtered = filtered.filter(
            item => item.type === EntityType.IDENTIFIER,
          );
          break;
      }
    }

    // Apply sorting
    filtered.sort((a, b) => {
      let result = 0;
      switch (sortType) {
        case 'date':
          result = a.createdAt - b.createdAt;
          break;
        case 'name':
          result = a.title.localeCompare(b.title);
          break;
        case 'type':
          result = a.type.localeCompare(b.type);
          break;
        case 'quantity':
          result = (a.quantity || 0) - (b.quantity || 0);
          break;
      }
      return sortAscending ? result : -result;
    });

    return filtered;
  };

  const handleItemPress = (item: BrowserItem) => {
    if (item.type === EntityType.PHYSICAL_COMPONENT && item.filamentInfo) {
      navigation.navigate('ComponentDetail', {identifier: item.id});
    } else {
      navigation.navigate('EntityDetail', {
        entityId: item.id,
        entityType: item.type,
      });
    }
  };

  const getEntityTypeIcon = (type: EntityType): string => {
    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return 'package-variant';
      case EntityType.INVENTORY_ITEM:
        return 'archive';
      case EntityType.IDENTIFIER:
        return 'identifier';
      case EntityType.ACTIVITY:
        return 'history';
      default:
        return 'help-circle';
    }
  };

  const getEntityTypeColor = (type: EntityType): string => {
    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return '#4CAF50';
      case EntityType.INVENTORY_ITEM:
        return '#2196F3';
      case EntityType.IDENTIFIER:
        return '#FF9800';
      case EntityType.ACTIVITY:
        return '#9C27B0';
      default:
        return '#757575';
    }
  };

  const renderItem = ({item}: {item: BrowserItem}) => (
    <Card style={styles.itemCard} onPress={() => handleItemPress(item)}>
      <Card.Content>
        <View style={styles.itemHeader}>
          <View style={styles.itemTitleContainer}>
            <Title style={styles.itemTitle}>{item.title}</Title>
            <Paragraph style={styles.itemSubtitle}>{item.subtitle}</Paragraph>
          </View>
          <IconButton
            icon={getEntityTypeIcon(item.type)}
            iconColor={getEntityTypeColor(item.type)}
            size={24}
          />
        </View>
        <View style={styles.itemMetadata}>
          <Text style={styles.metadataText}>{item.metadata}</Text>
          <Text style={styles.timestampText}>
            {new Date(item.createdAt).toLocaleDateString()}
          </Text>
        </View>
        {item.filamentInfo && (
          <View style={styles.colorIndicator}>
            <View
              style={[
                styles.colorSwatch,
                {backgroundColor: item.filamentInfo.colorHex},
              ]}
            />
            <Text style={styles.colorName}>{item.filamentInfo.colorName}</Text>
          </View>
        )}
      </Card.Content>
    </Card>
  );

  const renderFilterChips = () => (
    <ScrollView
      horizontal
      showsHorizontalScrollIndicator={false}
      style={styles.chipScrollView}
      contentContainerStyle={styles.chipContainer}>
      <Chip
        selected={filterType === 'all'}
        onPress={() => setFilterType('all')}
        style={styles.chip}>
        All ({items.length})
      </Chip>
      <Chip
        selected={filterType === 'components'}
        onPress={() => setFilterType('components')}
        style={styles.chip}>
        Components (
        {items.filter(i => i.type === EntityType.PHYSICAL_COMPONENT).length})
      </Chip>
      <Chip
        selected={filterType === 'inventory'}
        onPress={() => setFilterType('inventory')}
        style={styles.chip}>
        Inventory (
        {items.filter(i => i.type === EntityType.INVENTORY_ITEM).length})
      </Chip>
      <Chip
        selected={filterType === 'identifiers'}
        onPress={() => setFilterType('identifiers')}
        style={styles.chip}>
        Identifiers (
        {items.filter(i => i.type === EntityType.IDENTIFIER).length})
      </Chip>
    </ScrollView>
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
            <Text style={styles.radioLabel}>Date Created</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="name" />
            <Text style={styles.radioLabel}>Name</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="type" />
            <Text style={styles.radioLabel}>Type</Text>
          </View>
          <View style={styles.radioItem}>
            <RadioButton value="quantity" />
            <Text style={styles.radioLabel}>Quantity</Text>
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

  const filteredItems = getFilteredAndSortedItems();

  return (
    <View style={styles.container}>
      {/* Search Bar */}
      <Surface style={styles.searchContainer} elevation={2}>
        <Searchbar
          placeholder="Search inventory..."
          onChangeText={setSearchQuery}
          value={searchQuery}
          style={styles.searchbar}
        />
        <IconButton
          icon="sort"
          size={24}
          onPress={() => setShowSortModal(true)}
        />
      </Surface>

      {/* Filter Chips */}
      {renderFilterChips()}

      {/* Results Header */}
      <Surface style={styles.resultsHeader} elevation={1}>
        <Text style={styles.resultsText}>
          {filteredItems.length} items found
        </Text>
        <Text style={styles.sortText}>
          Sorted by {sortType} ({sortAscending ? 'oldest' : 'newest'} first)
        </Text>
      </Surface>

      {/* Items List */}
      <FlatList
        data={filteredItems}
        renderItem={renderItem}
        keyExtractor={item => item.id}
        style={styles.list}
        contentContainerStyle={styles.listContent}
        refreshing={isLoading}
        onRefresh={loadItems}
      />

      {/* Sort Modal */}
      {renderSortModal()}

      {/* Add Item FAB */}
      <FAB
        style={styles.fab}
        icon="plus"
        label="Add Item"
        onPress={() => {
          // TODO: Navigate to add item screen
        }}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  searchContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    margin: 16,
    marginBottom: 8,
    paddingHorizontal: 8,
    borderRadius: 12,
  },
  searchbar: {
    flex: 1,
    elevation: 0,
  },
  chipScrollView: {
    marginHorizontal: 16,
    marginBottom: 8,
  },
  chipContainer: {
    paddingRight: 16,
  },
  chip: {
    marginRight: 8,
  },
  resultsHeader: {
    marginHorizontal: 16,
    marginBottom: 8,
    padding: 12,
    borderRadius: 8,
  },
  resultsText: {
    fontSize: 16,
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
  itemCard: {
    marginBottom: 8,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  itemTitleContainer: {
    flex: 1,
  },
  itemTitle: {
    fontSize: 16,
    marginBottom: 4,
  },
  itemSubtitle: {
    fontSize: 14,
    color: '#666',
    marginBottom: 0,
  },
  itemMetadata: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginTop: 8,
  },
  metadataText: {
    fontSize: 12,
    color: '#333',
    fontWeight: '500',
  },
  timestampText: {
    fontSize: 12,
    color: '#999',
  },
  colorIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  colorSwatch: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  colorName: {
    fontSize: 12,
    color: '#666',
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

export default DataBrowserScreen;
