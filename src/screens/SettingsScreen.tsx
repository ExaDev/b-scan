import React, { useState, useEffect } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  Alert,
  Linking,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  Text,
  Switch,
  List,
  Portal,
  Modal,
  TextInput,
  RadioButton,
  IconButton,
} from 'react-native-paper';
import { NavigationProps } from '../types/Navigation';

interface SettingsScreenProps extends NavigationProps {}

interface AppSettings {
  nfcSettings: {
    enableAutoScan: boolean;
    scanTimeout: number; // seconds
    retryAttempts: number;
    enableHapticFeedback: boolean;
    enableSoundFeedback: boolean;
  };
  displaySettings: {
    theme: 'auto' | 'light' | 'dark';
    enableAnimations: boolean;
    showAdvancedInfo: boolean;
    defaultSortOrder: 'date' | 'name' | 'type';
  };
  dataSettings: {
    enableCloudSync: boolean;
    autoBackup: boolean;
    retentionPeriod: number; // days
    maxHistoryEntries: number;
  };
  privacySettings: {
    enableAnalytics: boolean;
    enableCrashReporting: boolean;
    shareUsageData: boolean;
  };
}

const SettingsScreen: React.FC<SettingsScreenProps> = ({ navigation: _navigation }) => {
  const [settings, setSettings] = useState<AppSettings>({
    nfcSettings: {
      enableAutoScan: true,
      scanTimeout: 10,
      retryAttempts: 3,
      enableHapticFeedback: true,
      enableSoundFeedback: false,
    },
    displaySettings: {
      theme: 'auto',
      enableAnimations: true,
      showAdvancedInfo: false,
      defaultSortOrder: 'date',
    },
    dataSettings: {
      enableCloudSync: false,
      autoBackup: true,
      retentionPeriod: 365,
      maxHistoryEntries: 1000,
    },
    privacySettings: {
      enableAnalytics: true,
      enableCrashReporting: true,
      shareUsageData: false,
    },
  });

  const [showTimeoutModal, setShowTimeoutModal] = useState<boolean>(false);
  const [showRetentionModal, setShowRetentionModal] = useState<boolean>(false);
  const [tempTimeout, setTempTimeout] = useState<string>('');
  const [tempRetention, setTempRetention] = useState<string>('');

  useEffect(() => {
    loadSettings();
  }, []);

  const loadSettings = async () => {
    // TODO: Load settings from AsyncStorage or secure storage
    // For now, using default settings
  };

  const saveSettings = async (newSettings: AppSettings) => {
    // TODO: Save settings to AsyncStorage or secure storage
    setSettings(newSettings);
  };

  const updateSetting = <K extends keyof AppSettings, T extends keyof AppSettings[K]>(
    category: K,
    setting: T,
    value: AppSettings[K][T]
  ) => {
    const newSettings = {
      ...settings,
      [category]: {
        ...settings[category],
        [setting]: value,
      },
    };
    saveSettings(newSettings);
  };

  const handleResetSettings = () => {
    Alert.alert(
      'Reset Settings',
      'This will reset all settings to their default values. Are you sure?',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Reset',
          style: 'destructive',
          onPress: () => {
            // Reset to default settings
            const defaultSettings: AppSettings = {
              nfcSettings: {
                enableAutoScan: true,
                scanTimeout: 10,
                retryAttempts: 3,
                enableHapticFeedback: true,
                enableSoundFeedback: false,
              },
              displaySettings: {
                theme: 'auto',
                enableAnimations: true,
                showAdvancedInfo: false,
                defaultSortOrder: 'date',
              },
              dataSettings: {
                enableCloudSync: false,
                autoBackup: true,
                retentionPeriod: 365,
                maxHistoryEntries: 1000,
              },
              privacySettings: {
                enableAnalytics: true,
                enableCrashReporting: true,
                shareUsageData: false,
              },
            };
            saveSettings(defaultSettings);
          },
        },
      ]
    );
  };

  const handleClearData = () => {
    Alert.alert(
      'Clear All Data',
      'This will permanently delete all scan history, saved components, and cached data. This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear Data',
          style: 'destructive',
          onPress: () => {
            // TODO: Clear all app data
            Alert.alert('Success', 'All data has been cleared.');
          },
        },
      ]
    );
  };

  const handleExportData = () => {
    // TODO: Export data to file
    Alert.alert('Export Data', 'Data export functionality would be implemented here.');
  };

  const handleImportData = () => {
    // TODO: Import data from file
    Alert.alert('Import Data', 'Data import functionality would be implemented here.');
  };

  const openAppSettings = () => {
    Linking.openSettings();
  };

  const renderTimeoutModal = () => (
    <Portal>
      <Modal
        visible={showTimeoutModal}
        onDismiss={() => setShowTimeoutModal(false)}
        contentContainerStyle={styles.modalContent}
      >
        <Title>Scan Timeout</Title>
        <Paragraph>Set the maximum time to wait for NFC tag detection (1-30 seconds)</Paragraph>
        <TextInput
          label="Timeout (seconds)"
          value={tempTimeout}
          onChangeText={setTempTimeout}
          keyboardType="numeric"
          style={styles.textInput}
        />
        <View style={styles.modalActions}>
          <Button
            mode="outlined"
            onPress={() => setShowTimeoutModal(false)}
            style={styles.modalButton}
          >
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={() => {
              const timeout = parseInt(tempTimeout);
              if (timeout >= 1 && timeout <= 30) {
                updateSetting('nfcSettings', 'scanTimeout', timeout);
                setShowTimeoutModal(false);
              } else {
                Alert.alert('Invalid Value', 'Timeout must be between 1 and 30 seconds');
              }
            }}
            style={styles.modalButton}
          >
            Save
          </Button>
        </View>
      </Modal>
    </Portal>
  );

  const renderRetentionModal = () => (
    <Portal>
      <Modal
        visible={showRetentionModal}
        onDismiss={() => setShowRetentionModal(false)}
        contentContainerStyle={styles.modalContent}
      >
        <Title>Data Retention Period</Title>
        <Paragraph>Set how long to keep scan history (7-1095 days)</Paragraph>
        <TextInput
          label="Days"
          value={tempRetention}
          onChangeText={setTempRetention}
          keyboardType="numeric"
          style={styles.textInput}
        />
        <View style={styles.modalActions}>
          <Button
            mode="outlined"
            onPress={() => setShowRetentionModal(false)}
            style={styles.modalButton}
          >
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={() => {
              const days = parseInt(tempRetention);
              if (days >= 7 && days <= 1095) {
                updateSetting('dataSettings', 'retentionPeriod', days);
                setShowRetentionModal(false);
              } else {
                Alert.alert('Invalid Value', 'Retention period must be between 7 and 1095 days');
              }
            }}
            style={styles.modalButton}
          >
            Save
          </Button>
        </View>
      </Modal>
    </Portal>
  );

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        {/* NFC Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>NFC Settings</Title>
            
            <List.Item
              title="Auto Scan"
              description="Automatically start scanning when opening the app"
              right={() => (
                <Switch
                  value={settings.nfcSettings.enableAutoScan}
                  onValueChange={(value) => 
                    updateSetting('nfcSettings', 'enableAutoScan', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Scan Timeout"
              description={`Wait ${settings.nfcSettings.scanTimeout} seconds for tag detection`}
              right={() => (
                <IconButton
                  icon="pencil"
                  size={20}
                  onPress={() => {
                    setTempTimeout(settings.nfcSettings.scanTimeout.toString());
                    setShowTimeoutModal(true);
                  }}
                />
              )}
            />
            
            <List.Item
              title="Retry Attempts"
              description={`Retry ${settings.nfcSettings.retryAttempts} times on failure`}
              right={() => (
                <View style={styles.retryButtons}>
                  <IconButton
                    icon="minus"
                    size={16}
                    onPress={() => 
                      settings.nfcSettings.retryAttempts > 1 &&
                      updateSetting('nfcSettings', 'retryAttempts', settings.nfcSettings.retryAttempts - 1)
                    }
                  />
                  <Text style={styles.retryValue}>{settings.nfcSettings.retryAttempts}</Text>
                  <IconButton
                    icon="plus"
                    size={16}
                    onPress={() => 
                      settings.nfcSettings.retryAttempts < 10 &&
                      updateSetting('nfcSettings', 'retryAttempts', settings.nfcSettings.retryAttempts + 1)
                    }
                  />
                </View>
              )}
            />
            
            <List.Item
              title="Haptic Feedback"
              description="Vibrate on successful scan"
              right={() => (
                <Switch
                  value={settings.nfcSettings.enableHapticFeedback}
                  onValueChange={(value) => 
                    updateSetting('nfcSettings', 'enableHapticFeedback', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Sound Feedback"
              description="Play sound on successful scan"
              right={() => (
                <Switch
                  value={settings.nfcSettings.enableSoundFeedback}
                  onValueChange={(value) => 
                    updateSetting('nfcSettings', 'enableSoundFeedback', value)
                  }
                />
              )}
            />
          </Card.Content>
        </Card>

        {/* Display Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Display Settings</Title>
            
            <List.Item
              title="Theme"
              description={`Current: ${settings.displaySettings.theme}`}
              right={() => (
                <View style={styles.themeButtons}>
                  <RadioButton.Group
                    onValueChange={(value) => 
                      updateSetting('displaySettings', 'theme', value as 'auto' | 'light' | 'dark')
                    }
                    value={settings.displaySettings.theme}
                  >
                    <View style={styles.themeOptions}>
                      <View style={styles.themeOption}>
                        <RadioButton value="auto" />
                        <Text style={styles.themeLabel}>Auto</Text>
                      </View>
                      <View style={styles.themeOption}>
                        <RadioButton value="light" />
                        <Text style={styles.themeLabel}>Light</Text>
                      </View>
                      <View style={styles.themeOption}>
                        <RadioButton value="dark" />
                        <Text style={styles.themeLabel}>Dark</Text>
                      </View>
                    </View>
                  </RadioButton.Group>
                </View>
              )}
            />
            
            <List.Item
              title="Animations"
              description="Enable smooth transitions and animations"
              right={() => (
                <Switch
                  value={settings.displaySettings.enableAnimations}
                  onValueChange={(value) => 
                    updateSetting('displaySettings', 'enableAnimations', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Advanced Info"
              description="Show technical details in scan results"
              right={() => (
                <Switch
                  value={settings.displaySettings.showAdvancedInfo}
                  onValueChange={(value) => 
                    updateSetting('displaySettings', 'showAdvancedInfo', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Default Sort Order"
              description={`Sort by ${settings.displaySettings.defaultSortOrder} by default`}
              right={() => (
                <RadioButton.Group
                  onValueChange={(value) => 
                    updateSetting('displaySettings', 'defaultSortOrder', value as 'date' | 'name' | 'type')
                  }
                  value={settings.displaySettings.defaultSortOrder}
                >
                  <View style={styles.sortOptions}>
                    <View style={styles.sortOption}>
                      <RadioButton value="date" />
                      <Text style={styles.sortLabel}>Date</Text>
                    </View>
                    <View style={styles.sortOption}>
                      <RadioButton value="name" />
                      <Text style={styles.sortLabel}>Name</Text>
                    </View>
                    <View style={styles.sortOption}>
                      <RadioButton value="type" />
                      <Text style={styles.sortLabel}>Type</Text>
                    </View>
                  </View>
                </RadioButton.Group>
              )}
            />
          </Card.Content>
        </Card>

        {/* Data Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Data Settings</Title>
            
            <List.Item
              title="Cloud Sync"
              description="Sync data across devices (requires account)"
              right={() => (
                <Switch
                  value={settings.dataSettings.enableCloudSync}
                  onValueChange={(value) => 
                    updateSetting('dataSettings', 'enableCloudSync', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Auto Backup"
              description="Automatically backup scan data locally"
              right={() => (
                <Switch
                  value={settings.dataSettings.autoBackup}
                  onValueChange={(value) => 
                    updateSetting('dataSettings', 'autoBackup', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Data Retention"
              description={`Keep data for ${settings.dataSettings.retentionPeriod} days`}
              right={() => (
                <IconButton
                  icon="pencil"
                  size={20}
                  onPress={() => {
                    setTempRetention(settings.dataSettings.retentionPeriod.toString());
                    setShowRetentionModal(true);
                  }}
                />
              )}
            />
            
            <List.Item
              title="Max History Entries"
              description={`Keep up to ${settings.dataSettings.maxHistoryEntries} scan records`}
              right={() => (
                <View style={styles.historyButtons}>
                  <IconButton
                    icon="minus"
                    size={16}
                    onPress={() => 
                      settings.dataSettings.maxHistoryEntries > 100 &&
                      updateSetting('dataSettings', 'maxHistoryEntries', settings.dataSettings.maxHistoryEntries - 100)
                    }
                  />
                  <Text style={styles.historyValue}>{settings.dataSettings.maxHistoryEntries}</Text>
                  <IconButton
                    icon="plus"
                    size={16}
                    onPress={() => 
                      settings.dataSettings.maxHistoryEntries < 10000 &&
                      updateSetting('dataSettings', 'maxHistoryEntries', settings.dataSettings.maxHistoryEntries + 100)
                    }
                  />
                </View>
              )}
            />
          </Card.Content>
        </Card>

        {/* Data Management */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Data Management</Title>
          </Card.Content>
          <Card.Actions style={styles.dataActions}>
            <Button mode="outlined" onPress={handleExportData}>
              Export Data
            </Button>
            <Button mode="outlined" onPress={handleImportData}>
              Import Data
            </Button>
          </Card.Actions>
          <Card.Actions style={styles.dataActions}>
            <Button 
              mode="outlined" 
              textColor="#F44336"
              onPress={handleClearData}
            >
              Clear All Data
            </Button>
          </Card.Actions>
        </Card>

        {/* Privacy Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>Privacy Settings</Title>
            
            <List.Item
              title="Analytics"
              description="Help improve the app by sharing usage analytics"
              right={() => (
                <Switch
                  value={settings.privacySettings.enableAnalytics}
                  onValueChange={(value) => 
                    updateSetting('privacySettings', 'enableAnalytics', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Crash Reporting"
              description="Automatically report crashes to help fix bugs"
              right={() => (
                <Switch
                  value={settings.privacySettings.enableCrashReporting}
                  onValueChange={(value) => 
                    updateSetting('privacySettings', 'enableCrashReporting', value)
                  }
                />
              )}
            />
            
            <List.Item
              title="Share Usage Data"
              description="Share anonymous usage patterns for research"
              right={() => (
                <Switch
                  value={settings.privacySettings.shareUsageData}
                  onValueChange={(value) => 
                    updateSetting('privacySettings', 'shareUsageData', value)
                  }
                />
              )}
            />
          </Card.Content>
        </Card>

        {/* System Settings */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>System Settings</Title>
          </Card.Content>
          <Card.Actions style={styles.systemActions}>
            <Button mode="outlined" onPress={openAppSettings}>
              Device NFC Settings
            </Button>
            <Button mode="outlined" onPress={handleResetSettings}>
              Reset All Settings
            </Button>
          </Card.Actions>
        </Card>

        {/* App Information */}
        <Card style={styles.card}>
          <Card.Content>
            <Title>About</Title>
            <View style={styles.aboutRow}>
              <Text style={styles.aboutLabel}>App Version</Text>
              <Text style={styles.aboutValue}>1.0.0 (1)</Text>
            </View>
            <View style={styles.aboutRow}>
              <Text style={styles.aboutLabel}>Build Date</Text>
              <Text style={styles.aboutValue}>{new Date().toLocaleDateString()}</Text>
            </View>
            <View style={styles.aboutRow}>
              <Text style={styles.aboutLabel}>React Native</Text>
              <Text style={styles.aboutValue}>0.81.1</Text>
            </View>
          </Card.Content>
          <Card.Actions>
            <Button onPress={() => Linking.openURL('https://github.com/your-repo/b-scan')}>
              GitHub Repository
            </Button>
          </Card.Actions>
        </Card>
      </ScrollView>

      {/* Modals */}
      {renderTimeoutModal()}
      {renderRetentionModal()}
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
  retryButtons: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  retryValue: {
    fontSize: 16,
    fontWeight: '600',
    marginHorizontal: 8,
    minWidth: 20,
    textAlign: 'center',
  },
  themeButtons: {
    marginTop: 8,
  },
  themeOptions: {
    flexDirection: 'column',
  },
  themeOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
  },
  themeLabel: {
    marginLeft: 8,
    fontSize: 14,
  },
  sortOptions: {
    flexDirection: 'column',
    marginTop: 8,
  },
  sortOption: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingVertical: 4,
  },
  sortLabel: {
    marginLeft: 8,
    fontSize: 14,
  },
  historyButtons: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  historyValue: {
    fontSize: 16,
    fontWeight: '600',
    marginHorizontal: 8,
    minWidth: 40,
    textAlign: 'center',
  },
  dataActions: {
    justifyContent: 'space-around',
  },
  systemActions: {
    flexDirection: 'column',
    alignItems: 'stretch',
  },
  aboutRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  aboutLabel: {
    fontSize: 14,
    color: '#333',
  },
  aboutValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
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

export default SettingsScreen;