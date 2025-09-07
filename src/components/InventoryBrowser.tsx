import React, {useState, useEffect} from 'react';
import {View, StyleSheet, FlatList, RefreshControl} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  Chip,
  useTheme,
  Surface,
  IconButton,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';

interface InventoryItem {
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
  const [inventoryItems, setInventoryItems] = useState<InventoryItem[]>([]);

  // Mock data - in real app would come from database/API
  const mockInventoryData: InventoryItem[] = [
    {
      id: '1',
      name: 'PLA Basic',
      material: 'PLA',
      color: 'Orange',
      brand: 'Bambu Lab',
      weight: 1000,
      remainingWeight: 750,
      lastScanDate: new Date(),
      isActive: true,
    },
    {
      id: '2',
      name: 'PETG Tough',
      material: 'PETG',
      color: 'Black',
      brand: 'Bambu Lab',
      weight: 1000,
      remainingWeight: 400,
      lastScanDate: new Date(Date.now() - 86400000),
      isActive: true,
    },
    {
      id: '3',
      name: 'TPU 95A',
      material: 'TPU',
      color: 'Clear',
      brand: 'Bambu Lab',
      weight: 500,
      remainingWeight: 500,
      lastScanDate: new Date(Date.now() - 259200000),
      isActive: false,
    },
  ];

  useEffect(() => {
    setInventoryItems(mockInventoryData);
  }, []);

  const onRefresh = async () => {
    setRefreshing(true);
    // Simulate API call
    setTimeout(() => {
      setInventoryItems(mockInventoryData);
      setRefreshing(false);
    }, 1000);
  };

  const renderInventoryItem = ({item}: {item: InventoryItem}) => {
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
    </View>
  );

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
});

export default InventoryBrowser;