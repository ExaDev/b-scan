import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, Dimensions, Alert} from 'react-native';
import {
  Snackbar,
  useTheme,
  Button,
  Menu,
  Badge,
} from 'react-native-paper';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import {TabView, SceneMap, TabBar} from 'react-native-tab-view';
import {NfcManagerService} from '../services/NfcManagerService';
import {TabNavigationProps} from '../types/Navigation';
import InventoryBrowser from '../components/InventoryBrowser';
import CatalogBrowser from '../components/CatalogBrowser';
import TagsBrowser from '../components/TagsBrowser';
import ScansBrowser from '../components/ScansBrowser';
import ScanPrompt from '../components/ScanPrompt';

interface HomeScreenProps extends TabNavigationProps {}

type RouteType = {
  key: string;
  title: string;
  icon: string;
};

const initialLayout = {width: Dimensions.get('window').width};

// Icon components to avoid inline creation
interface IconProps {
  size: number;
  color: string;
}

const SortIcon: React.FC<IconProps & {ascending: boolean}> = ({size, color, ascending}) => (
  <MaterialCommunityIcon 
    name={ascending ? 'sort-ascending' : 'sort-descending'} 
    size={size} 
    color={color} 
  />
);

const FilterIcon = ({size, color}: {size: number; color: string}) => (
  <MaterialIcon name="filter-list" size={size} color={color} />
);

const GroupIcon = ({size, color}: {size: number; color: string}) => (
  <MaterialIcon name="group-work" size={size} color={color} />
);


// Scene components extracted to avoid recreation on each render
const InventoryScene: React.FC<{onNavigateToDetails: (entityId: string, entityType: string) => void}> = ({onNavigateToDetails}) => (
  <InventoryBrowser onNavigateToDetails={onNavigateToDetails} />
);

const CatalogScene: React.FC<{onNavigateToDetails: (entityId: string, entityType: string) => void}> = ({onNavigateToDetails}) => (
  <CatalogBrowser onNavigateToDetails={onNavigateToDetails} />
);

const TagsScene: React.FC<{onNavigateToDetails: (entityId: string, entityType: string) => void}> = ({onNavigateToDetails}) => (
  <TagsBrowser onNavigateToDetails={onNavigateToDetails} />
);

const ScansScene: React.FC<{onNavigateToDetails: (entityId: string, entityType: string) => void}> = ({onNavigateToDetails}) => (
  <ScansBrowser onNavigateToDetails={onNavigateToDetails} />
);

const HomeScreen: React.FC<HomeScreenProps> = ({navigation}) => {
  const theme = useTheme();
  const [isNfcEnabled, setIsNfcEnabled] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [scanProgress, setScanProgress] = useState<number>(0);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  
  // Sort and filter state
  const [sortMenuVisible, setSortMenuVisible] = useState(false);
  const [, setFilterMenuVisible] = useState(false);
  const [groupMenuVisible, setGroupMenuVisible] = useState(false);
  const [sortProperty, setSortProperty] = useState<'lastScan' | 'name' | 'color' | 'material'>('lastScan');
  const [sortAscending] = useState(false);
  const [activeFilters] = useState(0);
  
  // Tab view state
  const [index, setIndex] = useState(0);
  const [routes] = useState<RouteType[]>([
    {key: 'inventory', title: 'Inventory', icon: 'inventory'},
    {key: 'catalog', title: 'Catalog', icon: 'category'},
    {key: 'tags', title: 'Tags', icon: 'tag'},
    {key: 'scans', title: 'Scans', icon: 'history'},
  ]);

  const nfcManager = NfcManagerService.getInstance();

  const checkNfcStatus = useCallback(async () => {
    const enabled = await nfcManager.isNfcEnabled();
    setIsNfcEnabled(enabled);
  }, [nfcManager]);

  const initializeNfc = useCallback(async () => {
    const initialized = await nfcManager.initialize();
    if (!initialized) {
      setSnackbarMessage('NFC not available on this device');
      setSnackbarVisible(true);
    }
  }, [nfcManager]);

  useEffect(() => {
    checkNfcStatus();
    initializeNfc();
  }, [checkNfcStatus, initializeNfc]);

  const handleStartScan = async () => {
    if (!isNfcEnabled) {
      Alert.alert(
        'NFC Disabled',
        'Please enable NFC in your device settings to scan tags.',
        [{text: 'OK'}],
      );
      return;
    }

    setIsScanning(true);
    setScanProgress(0);
    navigation.navigate('Scanning', {});
  };

  const handleNavigateToDetails = useCallback((type: string, identifier: string) => {
    navigation.navigate('EntityDetail', {
      entityId: identifier,
      entityType: type,
    });
  }, [navigation]);

  const inventoryScene = useCallback(() => (
    <InventoryScene onNavigateToDetails={handleNavigateToDetails} />
  ), [handleNavigateToDetails]);

  const catalogScene = useCallback(() => (
    <CatalogScene onNavigateToDetails={handleNavigateToDetails} />
  ), [handleNavigateToDetails]);

  const tagsScene = useCallback(() => (
    <TagsScene onNavigateToDetails={handleNavigateToDetails} />
  ), [handleNavigateToDetails]);

  const scansScene = useCallback(() => (
    <ScansScene onNavigateToDetails={handleNavigateToDetails} />
  ), [handleNavigateToDetails]);

  const sortIcon = useCallback((props: IconProps) => (
    <SortIcon {...props} ascending={sortAscending} />
  ), [sortAscending]);

  const renderScene = SceneMap({
    inventory: inventoryScene,
    catalog: catalogScene,
    tags: tagsScene,
    scans: scansScene,
  });

  const renderTabBar = (props: unknown) => (
    <TabBar
      {...(props as Parameters<typeof TabBar>[0])}
      indicatorStyle={{backgroundColor: theme.colors.primary}}
      style={{backgroundColor: theme.colors.surface}}
      activeColor={theme.colors.primary}
      inactiveColor={theme.colors.onSurfaceVariant}
    />
  );

  const shouldShowScanPrompt = () => {
    const currentRoute = routes[index];
    return currentRoute ? (currentRoute.key === 'inventory' || currentRoute.key === 'catalog') : false;
  };

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      {/* Conditional Scan Prompt - only on Inventory and Catalog tabs */}
      {shouldShowScanPrompt() && (
        <ScanPrompt
          isScanning={isScanning}
          scanProgress={scanProgress}
          onStartScan={handleStartScan}
          isNfcEnabled={isNfcEnabled}
          compact={false}
        />
      )}

      {/* Sort, Filter, Group Controls */}
      <View style={styles.controlsRow}>
        {/* Sort Button */}
        <Menu
          visible={sortMenuVisible}
          onDismiss={() => setSortMenuVisible(false)}
          anchor={
            <Button
              mode="outlined"
              onPress={() => setSortMenuVisible(true)}
              icon={sortIcon}
              compact
              style={styles.controlButton}>
              {sortProperty === 'lastScan' ? 'Last Scan' : 
               sortProperty === 'name' ? 'Name' :
               sortProperty === 'color' ? 'Color' : 'Material'}
            </Button>
          }>
          <Menu.Item onPress={() => { setSortProperty('lastScan'); setSortMenuVisible(false); }} title="Last Scan" />
          <Menu.Item onPress={() => { setSortProperty('name'); setSortMenuVisible(false); }} title="Name" />
          <Menu.Item onPress={() => { setSortProperty('color'); setSortMenuVisible(false); }} title="Color" />
          <Menu.Item onPress={() => { setSortProperty('material'); setSortMenuVisible(false); }} title="Material" />
        </Menu>

        {/* Filter Button */}
        <Button
          mode="outlined"
          onPress={() => setFilterMenuVisible(true)}
          icon={FilterIcon}
          compact
          style={styles.controlButton}>
          Filter
          {activeFilters > 0 && (
            <Badge size={16} style={styles.filterBadge}>{activeFilters}</Badge>
          )}
        </Button>

        {/* Group Button */}
        <Menu
          visible={groupMenuVisible}
          onDismiss={() => setGroupMenuVisible(false)}
          anchor={
            <Button
              mode="outlined"
              onPress={() => setGroupMenuVisible(true)}
              icon={GroupIcon}
              compact
              style={styles.controlButton}>
              Group
            </Button>
          }>
          <Menu.Item onPress={() => setGroupMenuVisible(false)} title="None" />
          <Menu.Item onPress={() => setGroupMenuVisible(false)} title="Color" />
          <Menu.Item onPress={() => setGroupMenuVisible(false)} title="Material" />
          <Menu.Item onPress={() => setGroupMenuVisible(false)} title="Series" />
        </Menu>
      </View>

      {/* Tab View */}
      <TabView<RouteType>
        navigationState={{index, routes}}
        renderScene={renderScene}
        renderTabBar={renderTabBar}
        onIndexChange={setIndex}
        initialLayout={initialLayout}
        style={styles.tabView}
      />

      {/* Snackbar for messages */}
      <Snackbar
        visible={snackbarVisible}
        onDismiss={() => setSnackbarVisible(false)}
        duration={3000}>
        {snackbarMessage}
      </Snackbar>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
  tabView: {
    flex: 1,
  },
  controlsRow: {
    flexDirection: 'row',
    paddingHorizontal: 16,
    paddingVertical: 8,
    gap: 8,
  },
  controlButton: {
    flex: 1,
  },
  filterBadge: {
    position: 'absolute',
    top: -4,
    right: -4,
  },
});

export default HomeScreen;
