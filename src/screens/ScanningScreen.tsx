import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, Alert, BackHandler} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  ProgressBar,
  Surface,
  Text,
  ActivityIndicator,
} from 'react-native-paper';
import {NfcManagerService} from '../services/NfcManagerService';
import {NavigationProps} from '../types/Navigation';
import {ScanProgress, ScanStage} from '../types/FilamentInfo';
import {TagReadResult} from '../services/NfcManager';

interface ScanningScreenProps extends NavigationProps {
  route: {
    params?: {
      tagUid?: string;
    };
  };
}

const ScanningScreen: React.FC<ScanningScreenProps> = ({
  navigation,
  route: _route,
}) => {
  const [scanProgress, setScanProgress] = useState<ScanProgress>({
    stage: ScanStage.INITIALIZING,
    percentage: 0,
    currentSector: 0,
    statusMessage: 'Preparing to scan...',
  });
  const [isScanning, setIsScanning] = useState<boolean>(true);
  const [scanResult, setScanResult] = useState<TagReadResult | null>(null);

  const nfcManager = NfcManagerService.getInstance();

  const handleCancelScan = useCallback(() => {
    Alert.alert('Cancel Scan', 'Are you sure you want to cancel the scan?', [
      {
        text: 'Continue Scanning',
        style: 'cancel',
      },
      {
        text: 'Cancel',
        onPress: () => {
          nfcManager.stopScan();
          navigation.navigate('Home');
        },
      },
    ]);
  }, [nfcManager, navigation]);

  const startScan = useCallback(async () => {
    setIsScanning(true);
    setScanResult(null);

    // Set up progress callback
    nfcManager.setScanProgressCallback((progress: ScanProgress) => {
      setScanProgress(progress);
    });

    try {
      const result = await nfcManager.scanTag();
      setScanResult(result);
      setIsScanning(false);

      if (result.type === 'SUCCESS') {
        // Handle success inline to avoid circular dependency
        Alert.alert(
          'Scan Successful!',
          `Found ${result.filamentInfo.manufacturerName} ${result.filamentInfo.filamentType} - ${result.filamentInfo.colorName}`,
          [
            {
              text: 'View Details',
              onPress: () => {
                navigation.replace('ComponentDetail', {
                  identifier: result.filamentInfo.tagUid,
                });
              },
            },
            {
              text: 'Scan Another',
              onPress: () => {
                startScan();
              },
            },
          ],
        );
      } else {
        // Handle error inline to avoid circular dependency
        let title = 'Scan Error';
        let message = 'Failed to read the NFC tag.';

        switch (result.type) {
          case 'NO_NFC':
            title = 'NFC Not Available';
            message = 'NFC is not supported or enabled on this device.';
            break;
          case 'INVALID_TAG':
            title = 'Invalid Tag';
            message = 'The NFC tag format is not supported.';
            break;
          case 'AUTHENTICATION_FAILED':
            title = 'Authentication Failed';
            message =
              'Could not authenticate with the NFC tag. It may be protected or corrupted.';
            break;
          case 'READ_ERROR':
            title = 'Read Error';
            message =
              result.error || 'Unknown error occurred while reading the tag.';
            break;
          case 'PARSING_ERROR':
            title = 'Parsing Error';
            message = result.error || 'Could not parse the data from the tag.';
            break;
        }

        Alert.alert(title, message, [
          {
            text: 'Try Again',
            onPress: () => {
              startScan();
            },
          },
          {
            text: 'Cancel',
            onPress: () => {
              navigation.navigate('Home');
            },
            style: 'cancel',
          },
        ]);
      }
    } catch (error) {
      console.error('Scanning error:', error);
      setScanResult({
        type: 'READ_ERROR',
        error: error instanceof Error ? error.message : 'Unknown error',
      });
      setIsScanning(false);
    }
  }, [nfcManager, navigation]);

  const handleBackPress = useCallback((): boolean => {
    if (isScanning) {
      handleCancelScan();
      return true; // Prevent default back behavior
    }
    return false; // Allow default back behavior
  }, [isScanning, handleCancelScan]);

  useEffect(() => {
    startScan();

    // Handle Android back button
    const backHandler = BackHandler.addEventListener(
      'hardwareBackPress',
      handleBackPress,
    );

    return () => {
      backHandler.remove();
      nfcManager.stopScan();
    };
  }, [handleBackPress, nfcManager, startScan]);

  const getStageTitle = (stage: ScanStage): string => {
    switch (stage) {
      case ScanStage.INITIALIZING:
        return 'Initializing';
      case ScanStage.AUTHENTICATING:
        return 'Authenticating';
      case ScanStage.READING_DATA:
        return 'Reading Data';
      case ScanStage.PARSING_DATA:
        return 'Parsing Data';
      case ScanStage.COMPLETED:
        return 'Completed';
      case ScanStage.ERROR:
        return 'Error';
      default:
        return 'Scanning';
    }
  };

  const getStageDescription = (stage: ScanStage): string => {
    switch (stage) {
      case ScanStage.INITIALIZING:
        return 'Hold your device near an NFC tag to begin scanning';
      case ScanStage.AUTHENTICATING:
        return 'Authenticating with the tag using derived keys';
      case ScanStage.READING_DATA:
        return 'Reading filament data from the tag';
      case ScanStage.PARSING_DATA:
        return 'Processing and validating the filament information';
      case ScanStage.COMPLETED:
        return 'Scan completed successfully';
      case ScanStage.ERROR:
        return 'An error occurred during scanning';
      default:
        return 'Scanning in progress';
    }
  };

  return (
    <View style={styles.container}>
      <Surface style={styles.surface} elevation={4}>
        <View style={styles.content}>
          {/* NFC Icon and Animation */}
          <View style={styles.iconContainer}>
            {isScanning ? (
              <ActivityIndicator size="large" color="#6200EE" />
            ) : (
              <Text style={styles.nfcIcon}>📱</Text>
            )}
          </View>

          {/* Stage Title */}
          <Title style={styles.title}>
            {getStageTitle(scanProgress.stage)}
          </Title>

          {/* Progress Bar */}
          {isScanning && (
            <View style={styles.progressContainer}>
              <ProgressBar
                progress={scanProgress.percentage / 100}
                color="#6200EE"
                style={styles.progressBar}
              />
              <Text style={styles.progressText}>
                {Math.round(scanProgress.percentage)}%
              </Text>
            </View>
          )}

          {/* Status Message */}
          <Paragraph style={styles.statusMessage}>
            {scanProgress.statusMessage ||
              getStageDescription(scanProgress.stage)}
          </Paragraph>

          {/* Sector Information */}
          {scanProgress.stage === ScanStage.AUTHENTICATING &&
            scanProgress.currentSector > 0 && (
              <Text style={styles.sectorInfo}>
                Sector {scanProgress.currentSector} of 16
              </Text>
            )}

          {/* Instructions */}
          {scanProgress.stage === ScanStage.INITIALIZING && (
            <Card style={styles.instructionsCard}>
              <Card.Content>
                <Title style={styles.instructionsTitle}>How to Scan</Title>
                <Paragraph>1. Hold your device steady</Paragraph>
                <Paragraph>
                  2. Place the NFC tag on the back of your device
                </Paragraph>
                <Paragraph>
                  3. Keep the tag in contact until scanning is complete
                </Paragraph>
              </Card.Content>
            </Card>
          )}

          {/* Action Buttons */}
          <View style={styles.buttonContainer}>
            {isScanning ? (
              <Button
                mode="outlined"
                onPress={handleCancelScan}
                style={styles.button}>
                Cancel Scan
              </Button>
            ) : scanResult?.type !== 'SUCCESS' ? (
              <>
                <Button
                  mode="contained"
                  onPress={startScan}
                  style={styles.button}>
                  Try Again
                </Button>
                <Button
                  mode="outlined"
                  onPress={() => navigation.navigate('Home')}
                  style={styles.button}>
                  Back to Home
                </Button>
              </>
            ) : null}
          </View>
        </View>
      </Surface>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
    justifyContent: 'center',
    padding: 16,
  },
  surface: {
    borderRadius: 16,
    padding: 24,
  },
  content: {
    alignItems: 'center',
  },
  iconContainer: {
    marginBottom: 24,
    height: 80,
    width: 80,
    justifyContent: 'center',
    alignItems: 'center',
  },
  nfcIcon: {
    fontSize: 64,
  },
  title: {
    fontSize: 24,
    textAlign: 'center',
    marginBottom: 16,
    color: '#6200EE',
  },
  progressContainer: {
    width: '100%',
    alignItems: 'center',
    marginBottom: 16,
  },
  progressBar: {
    width: '100%',
    height: 8,
    borderRadius: 4,
    marginBottom: 8,
  },
  progressText: {
    fontSize: 14,
    color: '#666',
  },
  statusMessage: {
    textAlign: 'center',
    marginBottom: 16,
    fontSize: 16,
  },
  sectorInfo: {
    textAlign: 'center',
    fontSize: 14,
    color: '#666',
    marginBottom: 16,
  },
  instructionsCard: {
    width: '100%',
    marginBottom: 24,
  },
  instructionsTitle: {
    fontSize: 18,
    marginBottom: 8,
  },
  buttonContainer: {
    width: '100%',
    gap: 12,
  },
  button: {
    marginVertical: 4,
  },
});

export default ScanningScreen;
