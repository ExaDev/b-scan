import React, {useState, useEffect} from 'react';
import {View, StyleSheet, FlatList, RefreshControl} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  Chip,
  useTheme,
} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {Graph} from '../repositories/Graph';
import {EntityType} from '../types/FilamentInfo';

interface CatalogItemDisplay {
  id: string;
  name: string;
  material: string;
  brand: string;
  series: string;
  colors: string[];
  specifications: {
    diameter: string;
    printTemp: string;
    bedTemp: string;
  };
}

interface CatalogBrowserProps {
  onNavigateToDetails: (type: string, identifier: string) => void;
}

const CatalogBrowser: React.FC<CatalogBrowserProps> = ({
  onNavigateToDetails,
}) => {
  const theme = useTheme();
  const [catalogItems, setCatalogItems] = useState<CatalogItemDisplay[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [graph] = useState(() => new Graph());
  
  const loadCatalogItems = async () => {
    try {
      setError(null);
      
      // Find all stock definition entities from the graph
      const stockEntities = graph.findEntitiesByType(EntityType.STOCK_DEFINITION);
      
      // Transform stock definitions to display format
      const displayItems: CatalogItemDisplay[] = stockEntities.map(entity => ({
        id: entity.id,
        name: `Product ${entity.id.substring(0, 8)}`,
        material: 'Unknown', // Will be enhanced when product data is available
        brand: 'Unknown',
        series: 'Standard',
        colors: ['Unknown'],
        specifications: {
          diameter: '1.75mm',
          printTemp: '200-220°C',
          bedTemp: '60°C',
        },
      }));
      
      // If no stock definitions exist, show default catalog items
      if (displayItems.length === 0) {
        const defaultItems: CatalogItemDisplay[] = [
          {
            id: 'default-1',
            name: 'PLA Basic',
            material: 'PLA',
            brand: 'Bambu Lab',
            series: 'Basic',
            colors: ['White', 'Black', 'Grey', 'Red', 'Orange', 'Yellow'],
            specifications: {
              diameter: '1.75mm',
              printTemp: '190-230°C',
              bedTemp: '35-60°C',
            },
          },
          {
            id: 'default-2',
            name: 'PETG Tough',
            material: 'PETG',
            brand: 'Bambu Lab',
            series: 'Tough',
            colors: ['Natural', 'Black', 'White', 'Grey'],
            specifications: {
              diameter: '1.75mm',
              printTemp: '230-270°C',
              bedTemp: '70-80°C',
            },
          },
        ];
        setCatalogItems(defaultItems);
      } else {
        setCatalogItems(displayItems);
      }
    } catch (err) {
      console.error('Error loading catalog items:', err);
      setError('Failed to load catalog items');
      setCatalogItems([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCatalogItems();
  }, [graph]);

  const onRefresh = async () => {
    setRefreshing(true);
    try {
      await loadCatalogItems();
    } finally {
      setRefreshing(false);
    }
  };

  const renderCatalogItem = ({item}: {item: CatalogItemDisplay}) => (
    <Card
      style={[styles.itemCard, {backgroundColor: theme.colors.surface}]}
      onPress={() => onNavigateToDetails('catalog', item.id)}>
      <Card.Content>
        <Title style={[styles.itemTitle, {color: theme.colors.onSurface}]}>
          {item.name}
        </Title>
        <Paragraph style={[styles.itemBrand, {color: theme.colors.onSurfaceVariant}]}>
          {item.brand} - {item.series} Series
        </Paragraph>
        
        <View style={styles.specsContainer}>
          <Text style={[styles.specText, {color: theme.colors.onSurfaceVariant}]}>
            Print: {item.specifications.printTemp} | Bed: {item.specifications.bedTemp}
          </Text>
        </View>
        
        <View style={styles.colorsContainer}>
          {item.colors.slice(0, 4).map((color, index) => (
            <Chip
              key={index}
              mode="outlined"
              style={[styles.colorChip, {backgroundColor: theme.colors.surfaceVariant}]}
              textStyle={{color: theme.colors.onSurfaceVariant, fontSize: 10}}>
              {color}
            </Chip>
          ))}
          {item.colors.length > 4 && (
            <Text style={[styles.moreColors, {color: theme.colors.primary}]}>
              +{item.colors.length - 4} more
            </Text>
          )}
        </View>
      </Card.Content>
    </Card>
  );

  const renderHeader = () => (
    <View style={styles.sectionHeader}>
      <Title style={[styles.sectionTitle, {color: theme.colors.onBackground}]}>
        Filament Catalog
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
          Loading catalog...
        </Text>
      </View>
    );
  }

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={catalogItems}
        renderItem={renderCatalogItem}
        keyExtractor={(item) => item.id}
        ListHeaderComponent={renderHeader}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={onRefresh}
            colors={[theme.colors.primary]}
          />
        }
        contentContainerStyle={catalogItems.length === 0 ? styles.emptyContent : styles.listContent}
        showsVerticalScrollIndicator={false}
        ListEmptyComponent={
          !loading && (
            <View style={styles.emptyContainer}>
              <Icon name="category" size={64} color={theme.colors.outline} />
              <Text style={[styles.emptyTitle, {color: theme.colors.onSurfaceVariant}]}>
                No Catalog Items
              </Text>
              <Text style={[styles.emptyMessage, {color: theme.colors.onSurfaceVariant}]}>
                Stock definitions will appear here when available
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
  itemTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 2,
  },
  itemBrand: {
    fontSize: 12,
    marginBottom: 8,
  },
  specsContainer: {
    marginBottom: 8,
  },
  specText: {
    fontSize: 12,
  },
  colorsContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    flexWrap: 'wrap',
  },
  colorChip: {
    marginRight: 4,
    marginBottom: 4,
    height: 24,
  },
  moreColors: {
    fontSize: 10,
    marginLeft: 8,
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

export default CatalogBrowser;