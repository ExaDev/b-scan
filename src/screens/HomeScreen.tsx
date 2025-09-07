import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, Dimensions, Alert} from 'react-native';
import {
  Appbar,
  Snackbar,
  useTheme,
  Button,
  Menu,
  Portal,
  Badge,
} from 'react-native-paper';
import MaterialIcon from 'react-native-vector-icons/MaterialIcons';
import MaterialCommunityIcon from 'react-native-vector-icons/MaterialCommunityIcons';
import {TabView, SceneMap, TabBar} from 'react-native-tab-view';
import {NfcManagerService} from '../services/NfcManagerService';
import {NavigationProps, ViewMode} from '../types/Navigation';
import {TagReadResult} from '../services/NfcManager';
import InventoryBrowser from '../components/InventoryBrowser';
import CatalogBrowser from '../components/CatalogBrowser';
import TagsBrowser from '../components/TagsBrowser';
import ScansBrowser from '../components/ScansBrowser';
import ScanPrompt from '../components/ScanPrompt';

interface HomeScreenProps extends NavigationProps {}

const initialLayout = {width: Dimensions.get('window').width};

const HomeScreen: React.FC<HomeScreenProps> = ({navigation}) => {
  const theme = useTheme();
  const [isNfcEnabled, setIsNfcEnabled] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [scanProgress, setScanProgress] = useState<number>(0);
  const [snackbarVisible, setSnackbarVisible] = useState(false);
  const [snackbarMessage, setSnackbarMessage] = useState('');
  
  // Sort and filter state
  const [sortMenuVisible, setSortMenuVisible] = useState(false);
  const [filterMenuVisible, setFilterMenuVisible] = useState(false);
  const [groupMenuVisible, setGroupMenuVisible] = useState(false);
  const [sortProperty, setSortProperty] = useState<'lastScan' | 'name' | 'color' | 'material'>('lastScan');
  const [sortAscending, setSortAscending] = useState(false);
  const [activeFilters, setActiveFilters] = useState(0);
  
  // Tab view state
  const [index, setIndex] = useState(0);
  const [routes] = useState([
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
    navigation.navigate('Scanning');
  };

  const handleNavigateToDetails = (type: string, identifier: string) => {
    navigation.navigate('EntityDetail', {
      entityId: identifier,
      entityType: type,
    });
  };

  const renderScene = SceneMap({
    inventory: () => (
      <InventoryBrowser
        onNavigateToDetails={handleNavigateToDetails}
      />
    ),
    catalog: () => (
      <CatalogBrowser
        onNavigateToDetails={handleNavigateToDetails}
      />
    ),
    tags: () => (
      <TagsBrowser
        onNavigateToDetails={handleNavigateToDetails}
      />
    ),
    scans: () => (
      <ScansBrowser
        onNavigateToDetails={handleNavigateToDetails}
      />
    ),
  });

  const renderTabBar = (props: any) => (
    <TabBar
      {...props}
      indicatorStyle={{backgroundColor: theme.colors.primary}}
      style={{backgroundColor: theme.colors.surface}}
      labelStyle={{color: theme.colors.onSurface, fontSize: 12, fontWeight: '600'}}
      activeColor={theme.colors.primary}
      inactiveColor={theme.colors.onSurfaceVariant}
    />
  );

  const shouldShowScanPrompt = () => {
    const currentRoute = routes[index];
    return currentRoute.key === 'inventory' || currentRoute.key === 'catalog';
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
              icon={({size, color}) => (
                <MaterialCommunityIcon 
                  name={sortAscending ? 'sort-ascending' : 'sort-descending'} 
                  size={size} 
                  color={color} 
                />
              )}
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
          icon={({size, color}) => (
            <MaterialIcon name="filter-list" size={size} color={color} />
          )}
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
              icon={({size, color}) => (
                <MaterialIcon name="group-work" size={size} color={color} />
              )}
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
      <TabView
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
