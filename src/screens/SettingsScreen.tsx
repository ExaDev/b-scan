import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, ScrollView, Alert, Linking} from 'react-native';
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
import {TabNavigationProps} from '../types/Navigation';

interface SettingsScreenProps extends TabNavigationProps {}

// Extracted components to avoid inline definitions
interface SettingSwitchProps {
  value: boolean;
  onValueChange: (value: boolean) => void;
}

const SettingSwitch: React.FC<SettingSwitchProps> = ({ value, onValueChange }) => (
  <Switch value={value} onValueChange={onValueChange} />
);

interface EditIconButtonProps {
  onPress: () => void;
}

const EditIconButton: React.FC<EditIconButtonProps> = ({ onPress }) => (
  <IconButton icon="pencil" size={20} onPress={onPress} />
);

interface CounterControlProps {
  value: number;
  onDecrement: () => void;
  onIncrement: () => void;
  style?: Record<string, unknown>;
}

const RetryCounterControl: React.FC<CounterControlProps> = ({ 
  value, 
  onDecrement, 
  onIncrement,
  style 
}) => (
  <View style={style}>
    <IconButton icon="minus" size={16} onPress={onDecrement} />
    <Text style={styles.retryValue}>{value}</Text>
    <IconButton icon="plus" size={16} onPress={onIncrement} />
  </View>
);

const HistoryCounterControl: React.FC<CounterControlProps> = ({ 
  value, 
  onDecrement, 
  onIncrement,
  style 
}) => (
  <View style={style}>
    <IconButton icon="minus" size={16} onPress={onDecrement} />
    <Text style={styles.historyValue}>{value}</Text>
    <IconButton icon="plus" size={16} onPress={onIncrement} />
  </View>
);

interface ThemeRadioGroupProps {
  value: 'auto' | 'light' | 'dark';
  onValueChange: (value: 'auto' | 'light' | 'dark') => void;
}

const ThemeRadioGroup: React.FC<ThemeRadioGroupProps> = ({ value, onValueChange }) => (
  <View style={styles.themeButtons}>
    <RadioButton.Group onValueChange={(newValue: string) => onValueChange(newValue as 'auto' | 'light' | 'dark')} value={value}>
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
);

interface SortRadioGroupProps {
  value: 'date' | 'name' | 'type';
  onValueChange: (value: 'date' | 'name' | 'type') => void;
}

const SortRadioGroup: React.FC<SortRadioGroupProps> = ({ value, onValueChange }) => (
  <RadioButton.Group onValueChange={(newValue: string) => onValueChange(newValue as 'date' | 'name' | 'type')} value={value}>
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
);

// Specific wrapped components to avoid arrow functions in render
interface SpecificSwitchProps {
  settings: AppSettings;
  updateSetting: <K extends keyof AppSettings, T extends keyof AppSettings[K]>(
    category: K,
    setting: T,
    value: AppSettings[K][T],
  ) => void;
}

const AutoScanSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.nfcSettings.enableAutoScan}
    onValueChange={value => updateSetting('nfcSettings', 'enableAutoScan', value)}
  />
);

const HapticFeedbackSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.nfcSettings.enableHapticFeedback}
    onValueChange={value => updateSetting('nfcSettings', 'enableHapticFeedback', value)}
  />
);

const SoundFeedbackSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.nfcSettings.enableSoundFeedback}
    onValueChange={value => updateSetting('nfcSettings', 'enableSoundFeedback', value)}
  />
);

const AnimationsSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.displaySettings.enableAnimations}
    onValueChange={value => updateSetting('displaySettings', 'enableAnimations', value)}
  />
);

const AdvancedInfoSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.displaySettings.showAdvancedInfo}
    onValueChange={value => updateSetting('displaySettings', 'showAdvancedInfo', value)}
  />
);

const CloudSyncSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.dataSettings.enableCloudSync}
    onValueChange={value => updateSetting('dataSettings', 'enableCloudSync', value)}
  />
);

const AutoBackupSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.dataSettings.autoBackup}
    onValueChange={value => updateSetting('dataSettings', 'autoBackup', value)}
  />
);

const AnalyticsSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.privacySettings.enableAnalytics}
    onValueChange={value => updateSetting('privacySettings', 'enableAnalytics', value)}
  />
);

const CrashReportingSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.privacySettings.enableCrashReporting}
    onValueChange={value => updateSetting('privacySettings', 'enableCrashReporting', value)}
  />
);

const ShareUsageDataSwitch: React.FC<SpecificSwitchProps> = ({ settings, updateSetting }) => (
  <SettingSwitch
    value={settings.privacySettings.shareUsageData}
    onValueChange={value => updateSetting('privacySettings', 'shareUsageData', value)}
  />
);

interface EditButtonWithActionsProps {
  onPress: () => void;
}

const ScanTimeoutEditButton: React.FC<EditButtonWithActionsProps> = ({ onPress }) => (
  <EditIconButton onPress={onPress} />
);

const RetentionEditButton: React.FC<EditButtonWithActionsProps> = ({ onPress }) => (
  <EditIconButton onPress={onPress} />
);

interface SpecificThemeRadioProps {
  settings: AppSettings;
  updateSetting: <K extends keyof AppSettings, T extends keyof AppSettings[K]>(
    category: K,
    setting: T,
    value: AppSettings[K][T],
  ) => void;
}

const ThemeRadioGroupWrapper: React.FC<SpecificThemeRadioProps> = ({ settings, updateSetting }) => (
  <ThemeRadioGroup
    value={settings.displaySettings.theme}
    onValueChange={value => updateSetting('displaySettings', 'theme', value)}
  />
);

const SortRadioGroupWrapper: React.FC<SpecificThemeRadioProps> = ({ settings, updateSetting }) => (
  <SortRadioGroup
    value={settings.displaySettings.defaultSortOrder}
    onValueChange={value => updateSetting('displaySettings', 'defaultSortOrder', value)}
  />
);

interface SpecificRetryControlProps {
  settings: AppSettings;
  updateSetting: <K extends keyof AppSettings, T extends keyof AppSettings[K]>(
    category: K,
    setting: T,
    value: AppSettings[K][T],
  ) => void;
}

const RetryAttemptsControl: React.FC<SpecificRetryControlProps> = ({ settings, updateSetting }) => (
  <RetryCounterControl
    value={settings.nfcSettings.retryAttempts}
    onDecrement={() =>
      settings.nfcSettings.retryAttempts > 1 &&
      updateSetting('nfcSettings', 'retryAttempts', settings.nfcSettings.retryAttempts - 1)
    }
    onIncrement={() =>
      settings.nfcSettings.retryAttempts < 10 &&
      updateSetting('nfcSettings', 'retryAttempts', settings.nfcSettings.retryAttempts + 1)
    }
    style={styles.retryButtons}
  />
);

const MaxHistoryControl: React.FC<SpecificRetryControlProps> = ({ settings, updateSetting }) => (
  <HistoryCounterControl
    value={settings.dataSettings.maxHistoryEntries}
    onDecrement={() =>
      settings.dataSettings.maxHistoryEntries > 100 &&
      updateSetting('dataSettings', 'maxHistoryEntries', settings.dataSettings.maxHistoryEntries - 100)
    }
    onIncrement={() =>
      settings.dataSettings.maxHistoryEntries < 10000 &&
      updateSetting('dataSettings', 'maxHistoryEntries', settings.dataSettings.maxHistoryEntries + 100)
    }
    style={styles.historyButtons}
  />
);

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

const SettingsScreen: React.FC<SettingsScreenProps> = () => {
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

  const updateSetting = useCallback(<
    K extends keyof AppSettings,
    T extends keyof AppSettings[K],
  >(
    category: K,
    setting: T,
    value: AppSettings[K][T],
  ) => {
    const newSettings = {
      ...settings,
      [category]: {
        ...settings[category],
        [setting]: value,
      },
    };
    saveSettings(newSettings);
  }, [settings]);

  // Create useCallback functions to avoid arrow functions in render
  const renderAutoScanSwitch = useCallback(() => (
    <AutoScanSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderScanTimeoutButton = useCallback(() => (
    <ScanTimeoutEditButton onPress={() => {
      setTempTimeout(settings.nfcSettings.scanTimeout.toString());
      setShowTimeoutModal(true);
    }} />
  ), [settings.nfcSettings.scanTimeout]);

  const renderRetryAttemptsControl = useCallback(() => (
    <RetryAttemptsControl settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderHapticFeedbackSwitch = useCallback(() => (
    <HapticFeedbackSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderSoundFeedbackSwitch = useCallback(() => (
    <SoundFeedbackSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderThemeRadioGroup = useCallback(() => (
    <ThemeRadioGroupWrapper settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderAnimationsSwitch = useCallback(() => (
    <AnimationsSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderAdvancedInfoSwitch = useCallback(() => (
    <AdvancedInfoSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderSortRadioGroup = useCallback(() => (
    <SortRadioGroupWrapper settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderCloudSyncSwitch = useCallback(() => (
    <CloudSyncSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderAutoBackupSwitch = useCallback(() => (
    <AutoBackupSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderRetentionEditButton = useCallback(() => (
    <RetentionEditButton onPress={() => {
      setTempRetention(settings.dataSettings.retentionPeriod.toString());
      setShowRetentionModal(true);
    }} />
  ), [settings.dataSettings.retentionPeriod]);

  const renderMaxHistoryControl = useCallback(() => (
    <MaxHistoryControl settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderAnalyticsSwitch = useCallback(() => (
    <AnalyticsSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderCrashReportingSwitch = useCallback(() => (
    <CrashReportingSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

  const renderShareUsageDataSwitch = useCallback(() => (
    <ShareUsageDataSwitch settings={settings} updateSetting={updateSetting} />
  ), [settings, updateSetting]);

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

  const handleResetSettings = () => {
    Alert.alert(
      'Reset Settings',
      'This will reset all settings to their default values. Are you sure?',
      [
        {text: 'Cancel', style: 'cancel'},
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
      ],
    );
  };

  const handleClearData = () => {
    Alert.alert(
      'Clear All Data',
      'This will permanently delete all scan history, saved components, and cached data. This action cannot be undone.',
      [
        {text: 'Cancel', style: 'cancel'},
        {
          text: 'Clear Data',
          style: 'destructive',
          onPress: () => {
            // TODO: Clear all app data
            Alert.alert('Success', 'All data has been cleared.');
          },
        },
      ],
    );
  };

  const handleExportData = () => {
    // TODO: Export data to file
    Alert.alert(
      'Export Data',
      'Data export functionality would be implemented here.',
    );
  };

  const handleImportData = () => {
    // TODO: Import data from file
    Alert.alert(
      'Import Data',
      'Data import functionality would be implemented here.',
    );
  };

  const openAppSettings = () => {
    Linking.openSettings();
  };

  const renderTimeoutModal = () => (
    <Portal>
      <Modal
        visible={showTimeoutModal}
        onDismiss={() => setShowTimeoutModal(false)}
        contentContainerStyle={styles.modalContent}>
        <Title>Scan Timeout</Title>
        <Paragraph>
          Set the maximum time to wait for NFC tag detection (1-30 seconds)
        </Paragraph>
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
            style={styles.modalButton}>
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={() => {
              const timeout = parseInt(tempTimeout, 10);
              if (timeout >= 1 && timeout <= 30) {
                updateSetting('nfcSettings', 'scanTimeout', timeout);
                setShowTimeoutModal(false);
              } else {
                Alert.alert(
                  'Invalid Value',
                  'Timeout must be between 1 and 30 seconds',
                );
              }
            }}
            style={styles.modalButton}>
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
        contentContainerStyle={styles.modalContent}>
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
            style={styles.modalButton}>
            Cancel
          </Button>
          <Button
            mode="contained"
            onPress={() => {
              const days = parseInt(tempRetention, 10);
              if (days >= 7 && days <= 1095) {
                updateSetting('dataSettings', 'retentionPeriod', days);
                setShowRetentionModal(false);
              } else {
                Alert.alert(
                  'Invalid Value',
                  'Retention period must be between 7 and 1095 days',
                );
              }
            }}
            style={styles.modalButton}>
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
              right={renderAutoScanSwitch}
            />

            <List.Item
              title="Scan Timeout"
              description={`Wait ${settings.nfcSettings.scanTimeout} seconds for tag detection`}
              right={renderScanTimeoutButton}
            />

            <List.Item
              title="Retry Attempts"
              description={`Retry ${settings.nfcSettings.retryAttempts} times on failure`}
              right={renderRetryAttemptsControl}
            />

            <List.Item
              title="Haptic Feedback"
              description="Vibrate on successful scan"
              right={renderHapticFeedbackSwitch}
            />

            <List.Item
              title="Sound Feedback"
              description="Play sound on successful scan"
              right={renderSoundFeedbackSwitch}
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
              right={renderThemeRadioGroup}
            />

            <List.Item
              title="Animations"
              description="Enable smooth transitions and animations"
              right={renderAnimationsSwitch}
            />

            <List.Item
              title="Advanced Info"
              description="Show technical details in scan results"
              right={renderAdvancedInfoSwitch}
            />

            <List.Item
              title="Default Sort Order"
              description={`Sort by ${settings.displaySettings.defaultSortOrder} by default`}
              right={renderSortRadioGroup}
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
              right={renderCloudSyncSwitch}
            />

            <List.Item
              title="Auto Backup"
              description="Automatically backup scan data locally"
              right={renderAutoBackupSwitch}
            />

            <List.Item
              title="Data Retention"
              description={`Keep data for ${settings.dataSettings.retentionPeriod} days`}
              right={renderRetentionEditButton}
            />

            <List.Item
              title="Max History Entries"
              description={`Keep up to ${settings.dataSettings.maxHistoryEntries} scan records`}
              right={renderMaxHistoryControl}
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
              onPress={handleClearData}>
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
              right={renderAnalyticsSwitch}
            />

            <List.Item
              title="Crash Reporting"
              description="Automatically report crashes to help fix bugs"
              right={renderCrashReportingSwitch}
            />

            <List.Item
              title="Share Usage Data"
              description="Share anonymous usage patterns for research"
              right={renderShareUsageDataSwitch}
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
              <Text style={styles.aboutValue}>
                {new Date().toLocaleDateString()}
              </Text>
            </View>
            <View style={styles.aboutRow}>
              <Text style={styles.aboutLabel}>React Native</Text>
              <Text style={styles.aboutValue}>0.81.1</Text>
            </View>
          </Card.Content>
          <Card.Actions>
            <Button
              onPress={() =>
                Linking.openURL('https://github.com/your-repo/b-scan')
              }>
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
