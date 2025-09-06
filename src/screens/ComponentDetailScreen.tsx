import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, ScrollView, Alert} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  Surface,
  Text,
  IconButton,
  Chip,
  TextInput,
  Modal,
  Portal,
  ActivityIndicator,
  ProgressBar,
} from 'react-native-paper';
import {NavigationProps} from '../types/Navigation';
import {PhysicalComponent, TagFormat, EntityType} from '../types/FilamentInfo';

interface ComponentDetailScreenProps extends NavigationProps {
  route: {
    params: {
      identifier: string;
    };
  };
}

interface UsageHistory {
  id: string;
  timestamp: number;
  printJob: string;
  weightUsed: number;
  printTime: number;
}

interface MaintenanceRecord {
  id: string;
  timestamp: number;
  type: 'drying' | 'storage' | 'cleaning';
  description: string;
  temperature?: number;
  duration?: number;
}

const ComponentDetailScreen: React.FC<ComponentDetailScreenProps> = ({
  navigation,
  route,
}) => {
  const {identifier} = route.params;
  const [component, setComponent] = useState<PhysicalComponent | null>(null);
  const [usageHistory, setUsageHistory] = useState<UsageHistory[]>([]);
  const [maintenanceRecords, setMaintenanceRecords] = useState<
    MaintenanceRecord[]
  >([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [showWeightModal, setShowWeightModal] = useState<boolean>(false);
  const [showNotesModal, setShowNotesModal] = useState<boolean>(false);
  const [newWeight, setNewWeight] = useState<string>('');
  const [newNotes, setNewNotes] = useState<string>('');

  const loadComponentDetails = useCallback(async () => {
    setIsLoading(true);
    // Simulate loading component data
    setTimeout(() => {
      const mockComponent: PhysicalComponent = {
        id: identifier,
        type: EntityType.PHYSICAL_COMPONENT,
        createdAt: Date.now() - 1000 * 60 * 60 * 24 * 7, // 1 week ago
        updatedAt: Date.now() - 1000 * 60 * 30, // 30 minutes ago
        filamentInfo: {
          tagUid: 'AB:CD:EF:12:34:56',
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
        currentWeight: 850,
        notes:
          'High quality filament, prints well with default settings. Stored in dry box.',
      };

      const mockUsage: UsageHistory[] = [
        {
          id: 'usage-1',
          timestamp: Date.now() - 1000 * 60 * 60 * 2, // 2 hours ago
          printJob: 'Phone Case v2',
          weightUsed: 25,
          printTime: 180, // 3 hours in minutes
        },
        {
          id: 'usage-2',
          timestamp: Date.now() - 1000 * 60 * 60 * 24, // 1 day ago
          printJob: 'Desk Organizer',
          weightUsed: 45,
          printTime: 300, // 5 hours
        },
        {
          id: 'usage-3',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 3, // 3 days ago
          printJob: 'Miniature Model',
          weightUsed: 15,
          printTime: 120, // 2 hours
        },
      ];

      const mockMaintenance: MaintenanceRecord[] = [
        {
          id: 'maint-1',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 5, // 5 days ago
          type: 'drying',
          description: 'Dried filament due to moisture absorption',
          temperature: 45,
          duration: 240, // 4 hours
        },
        {
          id: 'maint-2',
          timestamp: Date.now() - 1000 * 60 * 60 * 24 * 7, // 1 week ago
          type: 'storage',
          description: 'Moved to dry storage container with silica gel',
        },
      ];

      setComponent(mockComponent);
      setUsageHistory(mockUsage);
      setMaintenanceRecords(mockMaintenance);
      setNewNotes(mockComponent.notes || '');
      setIsLoading(false);
    }, 500);
  }, [identifier]);

  useEffect(() => {
    loadComponentDetails();
  }, [identifier, loadComponentDetails]);

  const getTagFormatLabel = (format: TagFormat): string => {
    switch (format) {
      case TagFormat.BAMBU_LAB:
        return 'Bambu Lab';
      case TagFormat.CREALITY:
        return 'Creality';
      case TagFormat.OPENSPOOL:
        return 'Open Tag';
      default:
        return 'Unknown';
    }
  };

  const calculateRemainingPercentage = (): number => {
    if (!component) return 0;
    const filamentWeight =
      component.currentWeight! - component.filamentInfo.spoolWeight;
    const originalFilamentWeight =
      (component.filamentInfo.filamentLength * 1.24) / 1000;
    return Math.max(
      0,
      Math.min(100, (filamentWeight / originalFilamentWeight) * 100),
    );
  };

  const handleUpdateWeight = () => {
    const weight = parseFloat(newWeight);
    if (isNaN(weight) || weight < 0) {
      Alert.alert('Invalid Weight', 'Please enter a valid weight in grams.');
      return;
    }

    if (component) {
      setComponent({
        ...component,
        currentWeight: weight,
        updatedAt: Date.now(),
      });
    }
    setShowWeightModal(false);
    setNewWeight('');
  };

  const handleUpdateNotes = () => {
    if (component) {
      setComponent({
        ...component,
        notes: newNotes,
        updatedAt: Date.now(),
      });
    }
    setShowNotesModal(false);
  };

  const handleStartDrying = () => {
    if (!component) return;

    Alert.alert(
      'Start Drying Process',
      `Recommended settings:\nTemperature: ${component.filamentInfo.dryingTemperature}°C\nDuration: ${component.filamentInfo.dryingTime} hours`,
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Start',
          onPress: () => {
            const newRecord: MaintenanceRecord = {
              id: `maint-${Date.now()}`,
              timestamp: Date.now(),
              type: 'drying',
              description: 'Drying process started',
              temperature: component.filamentInfo.dryingTemperature,
              duration: component.filamentInfo.dryingTime * 60, // Convert to minutes
            };
            setMaintenanceRecords([newRecord, ...maintenanceRecords]);
            Alert.alert('Drying Started', 'Maintenance record has been added.');
          },
        },
      ],
    );
  };

  const formatTime = (minutes: number): string => {
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}h ${mins}m`;
  };

  const formatWeight = (grams: number): string => {
    if (grams >= 1000) {
      return `${(grams / 1000).toFixed(1)}kg`;
    }
    return `${grams}g`;
  };

  const renderWeightModal = () => (
    <Portal>
      <Modal
        visible={showWeightModal}
        onDismiss={() => setShowWeightModal(false)}
        contentContainerStyle={styles.modalContent}>
        <Title>Update Current Weight</Title>
        <TextInput
          label="Current weight (grams)"
          value={newWeight}
          onChangeText={setNewWeight}
          keyboardType="numeric"
          style={styles.textInput}
        />
        <View style={styles.modalActions}>
          <Button
            mode="outlined"
            onPress={() => setShowWeightModal(false)}
            style={styles.modalButton}>
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={handleUpdateWeight}
            style={styles.modalButton}>
            Update
          </Button>
        </View>
      </Modal>
    </Portal>
  );

  const renderNotesModal = () => (
    <Portal>
      <Modal
        visible={showNotesModal}
        onDismiss={() => setShowNotesModal(false)}
        contentContainerStyle={styles.modalContent}>
        <Title>Edit Notes</Title>
        <TextInput
          label="Notes"
          value={newNotes}
          onChangeText={setNewNotes}
          multiline
          numberOfLines={4}
          style={styles.textInput}
        />
        <View style={styles.modalActions}>
          <Button
            mode="outlined"
            onPress={() => setShowNotesModal(false)}
            style={styles.modalButton}>
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={handleUpdateNotes}
            style={styles.modalButton}>
            Save
          </Button>
        </View>
      </Modal>
    </Portal>
  );

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6200EE" />
        <Text style={styles.loadingText}>Loading component details...</Text>
      </View>
    );
  }

  if (!component) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Component not found</Text>
        <Button mode="contained" onPress={() => navigation.goBack()}>
          Go Back
        </Button>
      </View>
    );
  }

  const remainingPercentage = calculateRemainingPercentage();

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        {/* Header Card */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.header}>
              <View style={styles.colorInfo}>
                <View
                  style={[
                    styles.colorSwatch,
                    {backgroundColor: component.filamentInfo.colorHex},
                  ]}
                />
                <View style={styles.headerText}>
                  <Title>{component.filamentInfo.filamentType}</Title>
                  <Paragraph>
                    {component.filamentInfo.manufacturerName}
                  </Paragraph>
                  <Chip style={styles.formatChip}>
                    {getTagFormatLabel(component.filamentInfo.tagFormat)}
                  </Chip>
                </View>
              </View>
              <IconButton
                icon="pencil"
                size={24}
                onPress={() =>
                  navigation.navigate('EntityDetail', {
                    entityId: component.id,
                    entityType: component.type.toString(),
                  })
                }
              />
            </View>
          </Card.Content>
        </Card>

        {/* Weight Status */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.weightHeader}>
              <Title>Current Status</Title>
              <Button
                mode="outlined"
                compact
                onPress={() => {
                  setNewWeight(component.currentWeight?.toString() || '');
                  setShowWeightModal(true);
                }}>
                Update Weight
              </Button>
            </View>
            <View style={styles.weightStats}>
              <View style={styles.weightStat}>
                <Text style={styles.weightNumber}>
                  {formatWeight(component.currentWeight || 0)}
                </Text>
                <Text style={styles.weightLabel}>Current Weight</Text>
              </View>
              <View style={styles.weightStat}>
                <Text style={styles.weightNumber}>
                  {remainingPercentage.toFixed(0)}%
                </Text>
                <Text style={styles.weightLabel}>Remaining</Text>
              </View>
            </View>
            <ProgressBar
              progress={remainingPercentage / 100}
              color="#4CAF50"
              style={styles.progressBar}
            />
          </Card.Content>
        </Card>

        {/* Material Properties */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Material Properties</Title>
            <View style={styles.propertyGrid}>
              <View style={styles.propertyItem}>
                <Text style={styles.propertyLabel}>Diameter</Text>
                <Text style={styles.propertyValue}>
                  {component.filamentInfo.filamentDiameter}mm
                </Text>
              </View>
              <View style={styles.propertyItem}>
                <Text style={styles.propertyLabel}>Length</Text>
                <Text style={styles.propertyValue}>
                  {(component.filamentInfo.filamentLength / 1000).toFixed(0)}m
                </Text>
              </View>
              <View style={styles.propertyItem}>
                <Text style={styles.propertyLabel}>Spool Weight</Text>
                <Text style={styles.propertyValue}>
                  {formatWeight(component.filamentInfo.spoolWeight)}
                </Text>
              </View>
              <View style={styles.propertyItem}>
                <Text style={styles.propertyLabel}>Production Date</Text>
                <Text style={styles.propertyValue}>
                  {component.filamentInfo.productionDate}
                </Text>
              </View>
            </View>
          </Card.Content>
        </Card>

        {/* Print Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Print Settings</Title>
            <View style={styles.settingsRow}>
              <Text style={styles.settingLabel}>Nozzle Temperature</Text>
              <Text style={styles.settingValue}>
                {component.filamentInfo.minTemperature}°C -{' '}
                {component.filamentInfo.maxTemperature}°C
              </Text>
            </View>
            <View style={styles.settingsRow}>
              <Text style={styles.settingLabel}>Bed Temperature</Text>
              <Text style={styles.settingValue}>
                {component.filamentInfo.bedTemperature}°C
              </Text>
            </View>
          </Card.Content>
          <Card.Actions>
            <Button onPress={handleStartDrying}>Start Drying</Button>
          </Card.Actions>
        </Card>

        {/* Notes */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.notesHeader}>
              <Title>Notes</Title>
              <IconButton
                icon="pencil"
                size={20}
                onPress={() => setShowNotesModal(true)}
              />
            </View>
            <Paragraph>{component.notes || 'No notes available'}</Paragraph>
          </Card.Content>
        </Card>

        {/* Usage History */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Recent Usage</Title>
            {usageHistory.map(usage => (
              <Surface key={usage.id} style={styles.usageItem} elevation={1}>
                <View style={styles.usageHeader}>
                  <Text style={styles.usageTitle}>{usage.printJob}</Text>
                  <Text style={styles.usageDate}>
                    {new Date(usage.timestamp).toLocaleDateString()}
                  </Text>
                </View>
                <View style={styles.usageStats}>
                  <Text style={styles.usageStat}>
                    Used: {formatWeight(usage.weightUsed)}
                  </Text>
                  <Text style={styles.usageStat}>
                    Time: {formatTime(usage.printTime)}
                  </Text>
                </View>
              </Surface>
            ))}
          </Card.Content>
          <Card.Actions>
            <Button onPress={() => navigation.navigate('ScanHistory')}>
              View All History
            </Button>
          </Card.Actions>
        </Card>

        {/* Maintenance Records */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Maintenance History</Title>
            {maintenanceRecords.map(record => (
              <Surface
                key={record.id}
                style={styles.maintenanceItem}
                elevation={1}>
                <View style={styles.maintenanceHeader}>
                  <IconButton
                    icon={
                      record.type === 'drying'
                        ? 'heat-wave'
                        : record.type === 'storage'
                        ? 'archive'
                        : 'tools'
                    }
                    size={20}
                    iconColor="#6200EE"
                  />
                  <View style={styles.maintenanceContent}>
                    <Text style={styles.maintenanceTitle}>
                      {record.description}
                    </Text>
                    <Text style={styles.maintenanceDate}>
                      {new Date(record.timestamp).toLocaleDateString()}
                    </Text>
                    {record.temperature && record.duration && (
                      <Text style={styles.maintenanceDetails}>
                        {record.temperature}°C for {formatTime(record.duration)}
                      </Text>
                    )}
                  </View>
                </View>
              </Surface>
            ))}
          </Card.Content>
        </Card>

        {/* RFID Information */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>RFID Information</Title>
            <View style={styles.rfidRow}>
              <Text style={styles.rfidLabel}>Tag UID</Text>
              <Text style={[styles.rfidValue, styles.monospace]}>
                {component.filamentInfo.tagUid}
              </Text>
            </View>
            <View style={styles.rfidRow}>
              <Text style={styles.rfidLabel}>Tray UID</Text>
              <Text style={[styles.rfidValue, styles.monospace]}>
                {component.filamentInfo.trayUid}
              </Text>
            </View>
          </Card.Content>
        </Card>
      </ScrollView>

      {/* Modals */}
      {renderWeightModal()}
      {renderNotesModal()}
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
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  errorText: {
    fontSize: 18,
    color: '#F44336',
    marginBottom: 16,
    textAlign: 'center',
  },
  card: {
    marginBottom: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  colorInfo: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    flex: 1,
  },
  colorSwatch: {
    width: 40,
    height: 40,
    borderRadius: 20,
    marginRight: 16,
    borderWidth: 2,
    borderColor: '#ddd',
  },
  headerText: {
    flex: 1,
  },
  formatChip: {
    alignSelf: 'flex-start',
    marginTop: 8,
  },
  weightHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  weightStats: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    marginBottom: 16,
  },
  weightStat: {
    alignItems: 'center',
  },
  weightNumber: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#6200EE',
  },
  weightLabel: {
    fontSize: 12,
    color: '#666',
    marginTop: 4,
  },
  progressBar: {
    height: 8,
    borderRadius: 4,
  },
  propertyGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginTop: 8,
  },
  propertyItem: {
    width: '50%',
    paddingVertical: 8,
  },
  propertyLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  propertyValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  settingsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  settingLabel: {
    fontSize: 14,
    color: '#333',
  },
  settingValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#6200EE',
  },
  notesHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  usageItem: {
    marginVertical: 4,
    padding: 12,
    borderRadius: 8,
  },
  usageHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  usageTitle: {
    fontSize: 14,
    fontWeight: '600',
  },
  usageDate: {
    fontSize: 12,
    color: '#666',
  },
  usageStats: {
    flexDirection: 'row',
    justifyContent: 'space-between',
  },
  usageStat: {
    fontSize: 12,
    color: '#333',
  },
  maintenanceItem: {
    marginVertical: 4,
    padding: 8,
    borderRadius: 8,
  },
  maintenanceHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  maintenanceContent: {
    flex: 1,
    marginLeft: 8,
  },
  maintenanceTitle: {
    fontSize: 14,
    fontWeight: '600',
  },
  maintenanceDate: {
    fontSize: 12,
    color: '#666',
  },
  maintenanceDetails: {
    fontSize: 12,
    color: '#333',
    marginTop: 2,
  },
  rfidRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  rfidLabel: {
    fontSize: 14,
    color: '#333',
  },
  rfidValue: {
    fontSize: 14,
    color: '#666',
  },
  monospace: {
    fontFamily: 'monospace',
  },
  modalContent: {
    backgroundColor: 'white',
    padding: 20,
    margin: 20,
    borderRadius: 12,
  },
  textInput: {
    marginVertical: 16,
  },
  modalActions: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginTop: 16,
  },
  modalButton: {
    flex: 1,
    marginHorizontal: 8,
  },
});

export default ComponentDetailScreen;
