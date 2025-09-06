import React, {useState, useEffect, useCallback} from 'react';
import {View, StyleSheet, ScrollView, Share} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  Surface,
  Text,
  IconButton,
  Chip,
  Divider,
  ActivityIndicator,
} from 'react-native-paper';
import {NavigationProps} from '../types/Navigation';
import {
  ScanHistoryEntry,
  TagReadResult,
  TagFormat,
} from '../types/FilamentInfo';

interface ScanDetailScreenProps extends NavigationProps {
  route: {
    params: {
      scanId: string;
    };
  };
}

interface ScanDiagnostics {
  nfcEnabled: boolean;
  tagDetected: boolean;
  tagType: string;
  dataIntegrity: 'good' | 'partial' | 'corrupted';
  authenticationStatus: 'success' | 'failed' | 'not_required';
  readErrors: string[];
  processingTime: number;
  signalStrength: number;
}

const ScanDetailScreen: React.FC<ScanDetailScreenProps> = ({
  navigation,
  route,
}) => {
  const {scanId} = route.params;
  const [scanEntry, setScanEntry] = useState<ScanHistoryEntry | null>(null);
  const [diagnostics, setDiagnostics] = useState<ScanDiagnostics | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  const loadScanDetails = useCallback(async () => {
    setIsLoading(true);
    // Simulate loading scan details
    setTimeout(() => {
      const mockScan = createMockScanEntry(scanId);
      const mockDiagnostics = createMockDiagnostics(mockScan.result);

      setScanEntry(mockScan);
      setDiagnostics(mockDiagnostics);
      setIsLoading(false);
    }, 500);
  }, [scanId]);

  useEffect(() => {
    loadScanDetails();
  }, [scanId, loadScanDetails]);

  const createMockScanEntry = (id: string): ScanHistoryEntry => {
    // Create different mock data based on scan ID for variety
    const isSuccess =
      id.includes('scan-1') || id.includes('scan-2') || id.includes('scan-5');
    const isError = id.includes('scan-3') || id.includes('scan-7');

    if (isSuccess) {
      return {
        id,
        timestamp: Date.now() - Math.random() * 1000 * 60 * 60 * 24 * 7, // Within last week
        result: 'SUCCESS',
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
      };
    } else if (isError) {
      return {
        id,
        timestamp: Date.now() - Math.random() * 1000 * 60 * 60 * 24 * 7,
        result: 'READ_ERROR',
        error: 'Authentication failed: Invalid key for sector 4',
      };
    } else {
      return {
        id,
        timestamp: Date.now() - Math.random() * 1000 * 60 * 60 * 24 * 7,
        result: 'INVALID_TAG',
      };
    }
  };

  const createMockDiagnostics = (
    result: TagReadResult['type'],
  ): ScanDiagnostics => {
    const isSuccess = result === 'SUCCESS';

    return {
      nfcEnabled: true,
      tagDetected: result !== 'NO_NFC',
      tagType: result === 'NO_NFC' ? 'None' : 'MIFARE Classic 1K',
      dataIntegrity: isSuccess
        ? 'good'
        : result === 'PARSING_ERROR'
        ? 'corrupted'
        : 'partial',
      authenticationStatus: isSuccess
        ? 'success'
        : result.includes('AUTH')
        ? 'failed'
        : 'not_required',
      readErrors:
        result === 'SUCCESS'
          ? []
          : ['Sector 4 authentication failed', 'Key derivation timeout'],
      processingTime: 1200 + Math.random() * 800, // 1.2-2.0 seconds
      signalStrength: result === 'NO_NFC' ? 0 : 65 + Math.random() * 30, // 65-95%
    };
  };

  const getResultIcon = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return 'check-circle';
      case 'READ_ERROR':
      case 'PARSING_ERROR':
      case 'AUTHENTICATION_FAILED':
        return 'alert-circle';
      case 'INVALID_TAG':
        return 'close-circle';
      case 'NO_NFC':
        return 'nfc-off';
      default:
        return 'help-circle';
    }
  };

  const getResultColor = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return '#4CAF50';
      case 'READ_ERROR':
      case 'PARSING_ERROR':
      case 'AUTHENTICATION_FAILED':
        return '#FF9800';
      case 'INVALID_TAG':
      case 'NO_NFC':
        return '#F44336';
      default:
        return '#757575';
    }
  };

  const getResultLabel = (result: TagReadResult['type']): string => {
    switch (result) {
      case 'SUCCESS':
        return 'Scan Successful';
      case 'READ_ERROR':
        return 'Read Error';
      case 'PARSING_ERROR':
        return 'Parsing Error';
      case 'AUTHENTICATION_FAILED':
        return 'Authentication Failed';
      case 'INVALID_TAG':
        return 'Invalid Tag';
      case 'NO_NFC':
        return 'NFC Not Available';
      default:
        return 'Unknown Result';
    }
  };

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

  const handleShare = async () => {
    if (!scanEntry) return;

    const scanData = {
      timestamp: new Date(scanEntry.timestamp).toISOString(),
      result: scanEntry.result,
      ...(scanEntry.filamentInfo && {
        filament: {
          type: scanEntry.filamentInfo.filamentType,
          manufacturer: scanEntry.filamentInfo.manufacturerName,
          color: scanEntry.filamentInfo.colorName,
          tagUid: scanEntry.filamentInfo.tagUid,
        },
      }),
      ...(scanEntry.error && {error: scanEntry.error}),
    };

    try {
      await Share.share({
        title: 'B-Scan Result',
        message: `Scan Result: ${getResultLabel(
          scanEntry.result,
        )}\n\nDetails:\n${JSON.stringify(scanData, null, 2)}`,
      });
    } catch (error) {
      console.error('Error sharing scan result:', error);
    }
  };

  const handleRescan = () => {
    navigation.navigate('Scanning', {
      tagUid: scanEntry?.filamentInfo?.tagUid,
    });
  };

  const handleViewComponent = () => {
    if (scanEntry?.filamentInfo) {
      navigation.navigate('ComponentDetail', {
        identifier: scanEntry.filamentInfo.tagUid,
      });
    }
  };

  const renderFilamentInfo = () => {
    if (!scanEntry?.filamentInfo) return null;

    const filament = scanEntry.filamentInfo;

    return (
      <Card style={styles.card}>
        <Card.Content>
          <View style={styles.filamentHeader}>
            <View style={styles.filamentTitle}>
              <Title>{filament.filamentType}</Title>
              <Paragraph>{filament.manufacturerName}</Paragraph>
              <Chip style={styles.formatChip}>
                {getTagFormatLabel(filament.tagFormat)}
              </Chip>
            </View>
            <View
              style={[styles.colorSwatch, {backgroundColor: filament.colorHex}]}
            />
          </View>

          <Divider style={styles.divider} />

          <View style={styles.infoGrid}>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Color</Text>
              <Text style={styles.infoValue}>{filament.colorName}</Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Diameter</Text>
              <Text style={styles.infoValue}>
                {filament.filamentDiameter}mm
              </Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Spool Weight</Text>
              <Text style={styles.infoValue}>{filament.spoolWeight}g</Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Length</Text>
              <Text style={styles.infoValue}>
                {(filament.filamentLength / 1000).toFixed(0)}m
              </Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Production Date</Text>
              <Text style={styles.infoValue}>{filament.productionDate}</Text>
            </View>
            <View style={styles.infoItem}>
              <Text style={styles.infoLabel}>Print Temp</Text>
              <Text style={styles.infoValue}>
                {filament.minTemperature}°C - {filament.maxTemperature}°C
              </Text>
            </View>
          </View>
        </Card.Content>
        <Card.Actions>
          <Button onPress={handleViewComponent}>View Component</Button>
        </Card.Actions>
      </Card>
    );
  };

  const renderRfidInfo = () => {
    if (!scanEntry?.filamentInfo) return null;

    const filament = scanEntry.filamentInfo;

    return (
      <Card style={styles.card}>
        <Card.Content>
          <Title>RFID Information</Title>
          <View style={styles.rfidRow}>
            <Text style={styles.rfidLabel}>Tag UID</Text>
            <Text style={[styles.rfidValue, styles.monospace]}>
              {filament.tagUid}
            </Text>
          </View>
          <View style={styles.rfidRow}>
            <Text style={styles.rfidLabel}>Tray UID</Text>
            <Text style={[styles.rfidValue, styles.monospace]}>
              {filament.trayUid}
            </Text>
          </View>
          <View style={styles.rfidRow}>
            <Text style={styles.rfidLabel}>Tag Format</Text>
            <Text style={styles.rfidValue}>
              {getTagFormatLabel(filament.tagFormat)}
            </Text>
          </View>
        </Card.Content>
      </Card>
    );
  };

  const renderDiagnostics = () => {
    if (!diagnostics) return null;

    return (
      <Card style={styles.card}>
        <Card.Content>
          <Title>Scan Diagnostics</Title>

          <View style={styles.diagnosticItem}>
            <Text style={styles.diagnosticLabel}>NFC Status</Text>
            <Chip
              icon={diagnostics.nfcEnabled ? 'nfc' : 'nfc-off'}
              style={[
                styles.diagnosticChip,
                {
                  backgroundColor: diagnostics.nfcEnabled
                    ? '#4CAF5020'
                    : '#F4433620',
                },
              ]}
              textStyle={{
                color: diagnostics.nfcEnabled ? '#4CAF50' : '#F44336',
              }}>
              {diagnostics.nfcEnabled ? 'Enabled' : 'Disabled'}
            </Chip>
          </View>

          <View style={styles.diagnosticItem}>
            <Text style={styles.diagnosticLabel}>Tag Detection</Text>
            <Chip
              icon={diagnostics.tagDetected ? 'check' : 'close'}
              style={[
                styles.diagnosticChip,
                {
                  backgroundColor: diagnostics.tagDetected
                    ? '#4CAF5020'
                    : '#F4433620',
                },
              ]}
              textStyle={{
                color: diagnostics.tagDetected ? '#4CAF50' : '#F44336',
              }}>
              {diagnostics.tagDetected ? 'Detected' : 'Not Found'}
            </Chip>
          </View>

          {diagnostics.tagDetected && (
            <>
              <View style={styles.diagnosticItem}>
                <Text style={styles.diagnosticLabel}>Tag Type</Text>
                <Text style={styles.diagnosticValue}>
                  {diagnostics.tagType}
                </Text>
              </View>

              <View style={styles.diagnosticItem}>
                <Text style={styles.diagnosticLabel}>Data Integrity</Text>
                <Chip
                  style={[
                    styles.diagnosticChip,
                    {
                      backgroundColor:
                        diagnostics.dataIntegrity === 'good'
                          ? '#4CAF5020'
                          : diagnostics.dataIntegrity === 'partial'
                          ? '#FF980020'
                          : '#F4433620',
                    },
                  ]}
                  textStyle={{
                    color:
                      diagnostics.dataIntegrity === 'good'
                        ? '#4CAF50'
                        : diagnostics.dataIntegrity === 'partial'
                        ? '#FF9800'
                        : '#F44336',
                  }}>
                  {diagnostics.dataIntegrity.toUpperCase()}
                </Chip>
              </View>

              <View style={styles.diagnosticItem}>
                <Text style={styles.diagnosticLabel}>Authentication</Text>
                <Chip
                  style={[
                    styles.diagnosticChip,
                    {
                      backgroundColor:
                        diagnostics.authenticationStatus === 'success'
                          ? '#4CAF5020'
                          : diagnostics.authenticationStatus === 'failed'
                          ? '#F4433620'
                          : '#75757520',
                    },
                  ]}
                  textStyle={{
                    color:
                      diagnostics.authenticationStatus === 'success'
                        ? '#4CAF50'
                        : diagnostics.authenticationStatus === 'failed'
                        ? '#F44336'
                        : '#757575',
                  }}>
                  {diagnostics.authenticationStatus
                    .replace('_', ' ')
                    .toUpperCase()}
                </Chip>
              </View>

              <View style={styles.diagnosticItem}>
                <Text style={styles.diagnosticLabel}>Signal Strength</Text>
                <Text style={styles.diagnosticValue}>
                  {diagnostics.signalStrength.toFixed(0)}%
                </Text>
              </View>

              <View style={styles.diagnosticItem}>
                <Text style={styles.diagnosticLabel}>Processing Time</Text>
                <Text style={styles.diagnosticValue}>
                  {(diagnostics.processingTime / 1000).toFixed(2)}s
                </Text>
              </View>

              {diagnostics.readErrors.length > 0 && (
                <View style={styles.errorsSection}>
                  <Text style={styles.errorsTitle}>Read Errors:</Text>
                  {diagnostics.readErrors.map((error, index) => (
                    <Text key={index} style={styles.errorText}>
                      • {error}
                    </Text>
                  ))}
                </View>
              )}
            </>
          )}
        </Card.Content>
      </Card>
    );
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6200EE" />
        <Text style={styles.loadingText}>Loading scan details...</Text>
      </View>
    );
  }

  if (!scanEntry) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Scan not found</Text>
        <Button mode="contained" onPress={() => navigation.goBack()}>
          Go Back
        </Button>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        {/* Header Card */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.header}>
              <View style={styles.headerLeft}>
                <View style={styles.resultIndicator}>
                  <IconButton
                    icon={getResultIcon(scanEntry.result)}
                    iconColor={getResultColor(scanEntry.result)}
                    size={32}
                  />
                  <View style={styles.resultText}>
                    <Title>{getResultLabel(scanEntry.result)}</Title>
                    <Paragraph>
                      {new Date(scanEntry.timestamp).toLocaleString()}
                    </Paragraph>
                  </View>
                </View>
              </View>
              <View style={styles.headerActions}>
                <IconButton
                  icon="share-variant"
                  size={24}
                  onPress={handleShare}
                />
              </View>
            </View>

            {scanEntry.error && (
              <Surface style={styles.errorSurface} elevation={1}>
                <Text style={styles.errorMessage}>{scanEntry.error}</Text>
              </Surface>
            )}
          </Card.Content>
          <Card.Actions>
            <Button mode="outlined" onPress={handleRescan} icon="refresh">
              Rescan
            </Button>
          </Card.Actions>
        </Card>

        {/* Filament Information */}
        {renderFilamentInfo()}

        {/* RFID Information */}
        {renderRfidInfo()}

        {/* Diagnostics */}
        {renderDiagnostics()}
      </ScrollView>
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
  headerLeft: {
    flex: 1,
  },
  headerActions: {
    flexDirection: 'row',
  },
  resultIndicator: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  resultText: {
    marginLeft: 8,
    flex: 1,
  },
  errorSurface: {
    backgroundColor: '#FFEBEE',
    padding: 12,
    borderRadius: 8,
    marginTop: 16,
  },
  errorMessage: {
    color: '#F44336',
    fontSize: 14,
  },
  filamentHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 16,
  },
  filamentTitle: {
    flex: 1,
  },
  formatChip: {
    alignSelf: 'flex-start',
    marginTop: 8,
  },
  colorSwatch: {
    width: 40,
    height: 40,
    borderRadius: 20,
    borderWidth: 2,
    borderColor: '#ddd',
    marginLeft: 16,
  },
  divider: {
    marginVertical: 16,
  },
  infoGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  infoItem: {
    width: '50%',
    paddingVertical: 8,
  },
  infoLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  infoValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
  },
  rfidRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
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
  diagnosticItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 8,
  },
  diagnosticLabel: {
    fontSize: 14,
    color: '#333',
    flex: 1,
  },
  diagnosticValue: {
    fontSize: 14,
    fontWeight: '600',
    color: '#666',
  },
  diagnosticChip: {
    alignSelf: 'flex-start',
  },
  errorsSection: {
    marginTop: 16,
    paddingTop: 16,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  errorsTitle: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    marginBottom: 8,
  },
});

export default ScanDetailScreen;
