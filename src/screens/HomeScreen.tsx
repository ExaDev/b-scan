import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, Dimensions, Alert} from 'react-native';
import {
  Appbar,
  FAB,
  Portal,
  Snackbar,
  useTheme,
} from 'react-native-paper';
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
        scanState={isScanning ? 'processing' : 'idle'}
        scanProgress={scanProgress}
        onSimulateScan={handleStartScan}
      />
    ),
    catalog: () => (
      <CatalogBrowser
        onNavigateToDetails={handleNavigateToDetails}
        scanState={isScanning ? 'processing' : 'idle'}
        scanProgress={scanProgress}
        onSimulateScan={handleStartScan}
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

  return (
    <View style={[styles.container, {backgroundColor: theme.colors.background}]}>
      {/* Tab View */}
      <TabView
        navigationState={{index, routes}}
        renderScene={renderScene}
        renderTabBar={renderTabBar}
        onIndexChange={setIndex}
        initialLayout={initialLayout}
        style={styles.tabView}
      />

      {/* Floating Action Button for Scanning */}
      <Portal>
        <FAB
          style={[styles.fab, {backgroundColor: theme.colors.primary}]}
          icon="nfc-tap"
          label="Scan Tag"
          onPress={handleStartScan}
          disabled={!isNfcEnabled || isScanning}
        />
      </Portal>

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
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
  },
});

export default HomeScreen;
