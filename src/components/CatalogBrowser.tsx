import React, {useState} from 'react';
import {View, StyleSheet, FlatList} from 'react-native';
import {
  Text,
  Card,
  Title,
  Paragraph,
  Chip,
  useTheme,
} from 'react-native-paper';

interface CatalogItem {
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
  
  const catalogItems: CatalogItem[] = [
    {
      id: '1',
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
      id: '2',
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

  const renderCatalogItem = ({item}: {item: CatalogItem}) => (
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
    </View>
  );

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      <FlatList
        data={catalogItems}
        renderItem={renderCatalogItem}
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
});

export default CatalogBrowser;