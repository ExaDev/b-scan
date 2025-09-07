import React from 'react';
import {View, StyleSheet} from 'react-native';
import {Card, Title, Paragraph, Button, ProgressBar, useTheme} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';

interface ScanPromptProps {
  isScanning: boolean;
  scanProgress: number;
  onStartScan: () => void;
  isNfcEnabled: boolean;
  compact?: boolean;
}

const ScanPrompt: React.FC<ScanPromptProps> = ({
  isScanning,
  scanProgress,
  onStartScan,
  isNfcEnabled,
  compact = false,
}) => {
  const theme = useTheme();

  if (compact) {
    return (
      <View style={[styles.compactContainer, {backgroundColor: theme.colors.surfaceVariant}]}>
        <View style={styles.compactContent}>
          <Icon name="nfc" size={24} color={theme.colors.primary} />
          <View style={styles.compactText}>
            <Title style={styles.compactTitle}>
              {isScanning ? 'Scanning...' : 'Ready to scan NFC tags'}
            </Title>
            {isScanning && (
              <ProgressBar
                progress={scanProgress}
                color={theme.colors.primary}
                style={styles.progressBar}
              />
            )}
          </View>
          {!isScanning && (
            <Button
              mode="contained"
              onPress={onStartScan}
              disabled={!isNfcEnabled}
              compact>
              Scan
            </Button>
          )}
        </View>
      </View>
    );
  }

  return (
    <Card style={styles.card}>
      <Card.Content style={styles.content}>
        <View style={styles.iconContainer}>
          <Icon
            name={isScanning ? 'nfc-tap' : 'nfc'}
            size={48}
            color={isNfcEnabled ? theme.colors.primary : theme.colors.outline}
          />
        </View>
        
        <Title style={[styles.title, {color: theme.colors.onSurface}]}>
          {isScanning ? 'Scanning NFC Tag...' : 'Ready to Scan NFC Tags'}
        </Title>
        
        <Paragraph style={[styles.description, {color: theme.colors.onSurfaceVariant}]}>
          {isScanning
            ? 'Hold your device near the NFC tag to read filament data'
            : isNfcEnabled
            ? 'Tap the scan button to detect nearby NFC tags and read filament information'
            : 'NFC is disabled. Please enable NFC in your device settings to scan tags.'}
        </Paragraph>

        {isScanning ? (
          <View style={styles.progressContainer}>
            <ProgressBar
              progress={scanProgress}
              color={theme.colors.primary}
              style={styles.progressBar}
            />
            <Paragraph style={[styles.progressText, {color: theme.colors.onSurfaceVariant}]}>
              {Math.round(scanProgress * 100)}% complete
            </Paragraph>
          </View>
        ) : (
          <Button
            mode="contained"
            onPress={onStartScan}
            disabled={!isNfcEnabled}
            style={styles.button}
            icon="nfc-tap">
            Start Scanning
          </Button>
        )}
      </Card.Content>
    </Card>
  );
};

const styles = StyleSheet.create({
  card: {
    margin: 16,
  },
  content: {
    alignItems: 'center',
    paddingVertical: 32,
  },
  iconContainer: {
    marginBottom: 16,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 8,
    textAlign: 'center',
  },
  description: {
    textAlign: 'center',
    marginBottom: 24,
    lineHeight: 20,
  },
  progressContainer: {
    width: '100%',
    alignItems: 'center',
  },
  progressBar: {
    width: '100%',
    height: 6,
    borderRadius: 3,
  },
  progressText: {
    marginTop: 8,
    fontSize: 12,
  },
  button: {
    paddingHorizontal: 32,
  },
  compactContainer: {
    paddingHorizontal: 16,
    paddingVertical: 12,
  },
  compactContent: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  compactText: {
    flex: 1,
    marginLeft: 12,
  },
  compactTitle: {
    fontSize: 14,
    fontWeight: '600',
    marginBottom: 4,
  },
});

export default ScanPrompt;