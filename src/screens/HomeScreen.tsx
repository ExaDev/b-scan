import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, ScrollView, Alert} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  FAB,
  Surface,
  Text,
  IconButton,
} from 'react-native-paper';
import {NfcManagerService} from '../services/NfcManagerService';
import {NavigationProps} from '../types/Navigation';
import {TagReadResult} from '../types/FilamentInfo';

interface HomeScreenProps extends NavigationProps {}

const HomeScreen: React.FC<HomeScreenProps> = ({navigation}) => {
  const [isNfcEnabled, setIsNfcEnabled] = useState<boolean>(false);
  const [isScanning, setIsScanning] = useState<boolean>(false);
  const [_lastScanResult, _setLastScanResult] = useState<TagReadResult | null>(
    null,
  );

  const nfcManager = NfcManagerService.getInstance();

  const checkNfcStatus = useCallback(async () => {
    const enabled = await nfcManager.isNfcEnabled();
    setIsNfcEnabled(enabled);
  }, [nfcManager]);

  const initializeNfc = useCallback(async () => {
    const initialized = await nfcManager.initialize();
    if (!initialized) {
      Alert.alert(
        'NFC Not Available',
        'This device does not support NFC or NFC is disabled.',
        [{text: 'OK'}],
      );
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
    navigation.navigate('Scanning');
  };

  const handleViewInventory = () => {
    navigation.navigate('DataBrowser');
  };

  const handleViewHistory = () => {
    navigation.navigate('ScanHistory');
  };

  const handleSettings = () => {
    navigation.navigate('Settings');
  };

  const renderQuickStats = () => {
    return (
      <Surface style={styles.statsContainer} elevation={2}>
        <View style={styles.statItem}>
          <Text style={styles.statNumber}>12</Text>
          <Text style={styles.statLabel}>Total Spools</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statNumber}>8</Text>
          <Text style={styles.statLabel}>In Stock</Text>
        </View>
        <View style={styles.statDivider} />
        <View style={styles.statItem}>
          <Text style={styles.statNumber}>45</Text>
          <Text style={styles.statLabel}>Total Scans</Text>
        </View>
      </Surface>
    );
  };

  const renderRecentActivity = () => {
    return (
      <Card style={styles.card}>
        <Card.Content>
          <Title>Recent Activity</Title>
          <View style={styles.activityItem}>
            <Text style={styles.activityText}>Scanned PLA Basic - Orange</Text>
            <Text style={styles.activityTime}>2 hours ago</Text>
          </View>
          <View style={styles.activityItem}>
            <Text style={styles.activityText}>Updated PETG inventory</Text>
            <Text style={styles.activityTime}>1 day ago</Text>
          </View>
          <View style={styles.activityItem}>
            <Text style={styles.activityText}>Added new TPU spool</Text>
            <Text style={styles.activityTime}>3 days ago</Text>
          </View>
        </Card.Content>
        <Card.Actions>
          <Button onPress={handleViewHistory}>View All</Button>
        </Card.Actions>
      </Card>
    );
  };

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        {/* NFC Status Card */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.nfcStatusContainer}>
              <View style={styles.nfcStatusLeft}>
                <Title>NFC Status</Title>
                <Paragraph>
                  {isNfcEnabled ? 'Ready to scan' : 'NFC is disabled'}
                </Paragraph>
              </View>
              <View style={styles.nfcStatusRight}>
                <IconButton
                  icon={isNfcEnabled ? 'nfc' : 'nfc-off'}
                  size={32}
                  iconColor={isNfcEnabled ? '#4CAF50' : '#F44336'}
                />
              </View>
            </View>
          </Card.Content>
        </Card>

        {/* Quick Stats */}
        {renderQuickStats()}

        {/* Quick Actions */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Quick Actions</Title>
          </Card.Content>
          <Card.Actions style={styles.quickActions}>
            <Button
              mode="contained"
              onPress={handleViewInventory}
              style={styles.actionButton}>
              Browse Inventory
            </Button>
            <Button
              mode="outlined"
              onPress={handleViewHistory}
              style={styles.actionButton}>
              Scan History
            </Button>
          </Card.Actions>
        </Card>

        {/* Recent Activity */}
        {renderRecentActivity()}

        {/* Settings Quick Access */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Settings</Title>
            <Paragraph>Configure app preferences and NFC settings</Paragraph>
          </Card.Content>
          <Card.Actions>
            <Button onPress={handleSettings}>Open Settings</Button>
          </Card.Actions>
        </Card>
      </ScrollView>

      {/* Floating Action Button for Scanning */}
      <FAB
        style={styles.fab}
        icon="nfc-tap"
        label="Scan Tag"
        onPress={handleStartScan}
        disabled={!isNfcEnabled || isScanning}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    flex: 1,
    padding: 16,
  },
  card: {
    marginBottom: 16,
  },
  nfcStatusContainer: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  nfcStatusLeft: {
    flex: 1,
  },
  nfcStatusRight: {
    alignItems: 'center',
  },
  statsContainer: {
    flexDirection: 'row',
    marginBottom: 16,
    paddingVertical: 16,
    paddingHorizontal: 8,
    borderRadius: 12,
  },
  statItem: {
    flex: 1,
    alignItems: 'center',
  },
  statNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#6200EE',
  },
  statLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  statDivider: {
    width: 1,
    backgroundColor: '#ddd',
    marginHorizontal: 8,
  },
  quickActions: {
    flexDirection: 'column',
    alignItems: 'stretch',
  },
  actionButton: {
    marginVertical: 4,
  },
  activityItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  activityText: {
    flex: 1,
    fontSize: 14,
  },
  activityTime: {
    fontSize: 12,
    color: '#666',
  },
  fab: {
    position: 'absolute',
    margin: 16,
    right: 0,
    bottom: 0,
    backgroundColor: '#6200EE',
  },
});

export default HomeScreen;
