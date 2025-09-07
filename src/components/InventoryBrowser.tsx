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
import {EntityType, InventoryItem as InventoryItemEntity} from '../types/FilamentInfo';

interface InventoryItemDisplay {
  id: string;
  name: string;
  material: string;
  color: string;
  brand: string;
  weight: number;
  remainingWeight: number;
  lastScanDate?: Date;
  isActive: boolean;
}

interface InventoryBrowserProps {
  onNavigateToDetails: (type: string, identifier: string) => void;
}

const InventoryBrowser: React.FC<InventoryBrowserProps> = ({
  onNavigateToDetails,
}) => {
  const theme = useTheme();
  const [refreshing, setRefreshing] = useState(false);
  const [inventoryItems, setInventoryItems] = useState<InventoryItemDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [graph] = useState(() => new Graph());

  const loadInventoryItems = useCallback(async () => {
    try {
      setError(null);
      
      // Find all inventory item entities from the graph
      const inventoryEntities = graph.findEntitiesByType(EntityType.INVENTORY_ITEM) as InventoryItemEntity[];
      
      // Transform graph entities to display format
      const displayItems: InventoryItemDisplay[] = inventoryEntities.map(entity => ({
        id: entity.id,
        name: `Filament Item ${entity.id.substring(0, 8)}`, // Fallback name
        material: 'Unknown', // Will be enhanced when filament data is linked
        color: 'Unknown',
        brand: 'Unknown',
        weight: 1000, // Default weight
        remainingWeight: Math.max(0, entity.quantity * 25), // Estimate 25g per unit
        ...(entity.lastUpdated ? { lastScanDate: new Date(entity.lastUpdated) } : {}),
        isActive: entity.quantity > 0,
      }));
      
      setInventoryItems(displayItems);
    } catch (err) {
      console.error('Error loading inventory items:', err);
      setError('Failed to load inventory items');
      // Fallback to empty array
      setInventoryItems([]);
    } finally {
      setLoading(false);
    }
  }, [graph]);

  useEffect(() => {
    loadInventoryItems();
  }, [loadInventoryItems]);

  const onRefresh = async () => {
    setRefreshing(true);
    try {
      await loadInventoryItems();
    } finally {
      setRefreshing(false);
    }
  };

  const renderInventoryItem = ({item}: {item: InventoryItemDisplay}) => {
    const weightPercentage = (item.remainingWeight / item.weight) * 100;
    const getWeightColor = () => {
      if (weightPercentage > 50) return theme.colors.tertiary;
      if (weightPercentage > 20) return '#FF9800';
      return theme.colors.error;
    };

    return (
      <Card
        style={[styles.itemCard, {backgroundColor: theme.colors.surface}]}
        onPress={() => onNavigateToDetails('inventory', item.id)}>
        <Card.Content>
          <View style={styles.itemHeader}>
            <View style={styles.itemInfo}>
              <Title style={[styles.itemTitle, {color: theme.colors.onSurface}]}>
                {item.name}
              </Title>
              <Paragraph style={[styles.itemBrand, {color: theme.colors.onSurfaceVariant}]}>
                {item.brand}
              </Paragraph>
            </View>
            <View style={styles.itemStatus}>
              <Icon
                name={item.isActive ? 'inventory' : 'inventory-2'}
                size={24}
                color={item.isActive ? theme.colors.primary : theme.colors.outline}
              />
            </View>
          </View>

          <View style={styles.materialInfo}>
            <Chip
              mode="outlined"
              style={[styles.materialChip, {backgroundColor: theme.colors.surfaceVariant}]}
              textStyle={{color: theme.colors.onSurfaceVariant}}>
              {item.material}
            </Chip>
            <Chip
              mode="outlined"
              style={[styles.colorChip, {backgroundColor: theme.colors.surfaceVariant}]}
              textStyle={{color: theme.colors.onSurfaceVariant}}>
              {item.color}
            </Chip>
          </View>

          <View style={styles.weightInfo}>
            <View style={styles.weightText}>
              <Text style={[styles.weightLabel, {color: theme.colors.onSurfaceVariant}]}>
                Weight:
              </Text>
              <Text style={[styles.weightValue, {color: getWeightColor()}]}>
                {item.remainingWeight}g / {item.weight}g
              </Text>
            </View>
            <Surface
              style={[styles.weightBar, {backgroundColor: theme.colors.surfaceVariant}]}>
              <View
                style={[
                  styles.weightFill,
                  {
                    backgroundColor: getWeightColor(),
                    width: `${weightPercentage}%`,
                  },
                ]}
              />
            </Surface>
          </View>

          {item.lastScanDate && (
            <Text style={[styles.lastScan, {color: theme.colors.onSurfaceVariant}]}>
              Last scan: {item.lastScanDate.toLocaleDateString()}
            </Text>
          )}
        </Card.Content>
      </Card>
    );
  };

  const renderHeader = () => (
    <View style={styles.sectionHeader}>
      <Title style={[styles.sectionTitle, {color: theme.colors.onBackground}]}>
        Inventory ({inventoryItems.filter(item => item.isActive).length} active)
      </Title>
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
          Loading inventory...
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={inventoryItems}
        renderItem={renderInventoryItem}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={renderHeader}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        contentContainerStyle={inventoryItems.length === 0 ? styles.emptyContent : styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          !loading && (
            <View style={styles.emptyContainer}>
              <Icon name="inventory-2" size={64} color={theme.colors.outline} />
              <Text style={[styles.emptyTitle, {color: theme.colors.onSurfaceVariant}]}>
                No Inventory Items
              </Text>
              <Text style={[styles.emptyMessage, {color: theme.colors.onSurfaceVariant}]}>
                Scan some items to populate your inventory
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
    paddingBottom: 80, // Space for FAB
  },
  sectionHeader: {
    paddingHorizontal: 16,
    paddingVertical: 8,
  },
  sectionTitle: {
    fontSize: 18,
    fontWeight: '600',
  },
  itemCard: {
    marginHorizontal: 16,
    marginVertical: 4,
  },
  itemHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 8,
  },
  itemInfo: {
    flex: 1,
  },
  itemTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  itemBrand: {
    fontSize: 12,
  },
  itemStatus: {
    alignItems: 'center',
  },
  materialInfo: {
    flexDirection: 'row',
    marginBottom: 12,
  },
  materialChip: {
    marginRight: 8,
    height: 28,
  },
  colorChip: {
    height: 28,
  },
  weightInfo: {
    marginBottom: 8,
  },
  weightText: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 4,
  },
  weightLabel: {
    fontSize: 12,
  },
  weightValue: {
    fontSize: 12,
    fontWeight: '600',
  },
  weightBar: {
    height: 4,
    borderRadius: 2,
    overflow: 'hidden',
  },
  weightFill: {
    height: '100%',
    borderRadius: 2,
  },
  lastScan: {
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

export default InventoryBrowser;