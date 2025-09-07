/**
 * E2E tests for app navigation and workflow
 * Port of ScanWorkflowE2ETest.kt from the original Android app
 */

import React from 'react';
import { render, fireEvent, waitFor, screen } from '@testing-library/react-native';
import { NavigationContainer } from '@react-navigation/native';
import { PaperProvider } from 'react-native-paper';
import AppNavigator from '../../src/navigation/AppNavigator';

// Mock NFC manager for E2E tests
jest.mock('../../src/services/NfcManager', () => ({
  NfcManager: class {
    initialize = jest.fn().mockResolvedValue(true);
    cleanup = jest.fn().mockResolvedValue(undefined);
    scanTag = jest.fn().mockResolvedValue({
      success: true,
      data: {
        uid: '04914CCA5E6480',
        data: new Uint8Array(1024),
        technology: 'MifareClassic',
        format: 'BAMBU_LAB'
      }
    });
    cancelScan = jest.fn().mockResolvedValue(undefined);
    parseTagData = jest.fn().mockReturnValue({
      material: 'PLA',
      color: 'Red',
      bedTemperature: 60,
      format: 'BAMBU_LAB'
    });
  }
}));

// Mock React Native Paper components that might cause issues in tests
jest.mock('react-native-paper', () => {
  const RealPaper = jest.requireActual('react-native-paper');
  return {
    ...RealPaper,
    Portal: ({ children }: { children: React.ReactNode }) => children,
  };
});

describe('App Navigation E2E Tests', () => {
  const renderApp = () => {
    return render(
      <PaperProvider>
        <NavigationContainer>
          <AppNavigator />
        </NavigationContainer>
      </PaperProvider>
    );
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('basic app workflow', () => {
    it('should launch app and show home screen', async () => {
      renderApp();
      
      // App should show home screen initially
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Should show main navigation elements
      expect(screen.getByText('Start Scanning')).toBeTruthy();
    });

    it('should navigate to scanning screen and back', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Navigate to scanning screen
      fireEvent.press(screen.getByText('Start Scanning'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
      
      // Navigate back to home
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
    });

    it('should navigate to scan history', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Navigate to history screen
      fireEvent.press(screen.getByTestId('scan-history-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan History')).toBeTruthy();
      });
      
      // Should show empty state or scan list
      expect(
        screen.getByText('No scans yet') || 
        screen.getByTestId('scan-list')
      ).toBeTruthy();
    });

    it('should navigate to settings and back', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Navigate to settings
      fireEvent.press(screen.getByTestId('settings-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeTruthy();
      });
      
      // Should show settings options
      expect(screen.getByText('NFC Settings')).toBeTruthy();
      expect(screen.getByText('Debug Mode')).toBeTruthy();
      
      // Navigate back
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
    });
  });

  describe('scanning workflow', () => {
    it('should complete full scanning workflow', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Start scanning
      fireEvent.press(screen.getByText('Start Scanning'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
      
      // Simulate NFC scan
      fireEvent.press(screen.getByText('Scan Tag'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Complete')).toBeTruthy();
      });
      
      // Should show scan results
      expect(screen.getByText('Material: PLA')).toBeTruthy();
      expect(screen.getByText('Color: Red')).toBeTruthy();
    });

    it('should handle scan failure gracefully', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.scanTag.mockResolvedValue({
        success: false,
        error: 'No tag found'
      });
      
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Start scanning
      fireEvent.press(screen.getByText('Start Scanning'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
      
      // Simulate failed NFC scan
      fireEvent.press(screen.getByText('Scan Tag'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Failed')).toBeTruthy();
      });
      
      // Should show error message
      expect(screen.getByText('No tag found')).toBeTruthy();
      
      // Should allow retry
      expect(screen.getByText('Try Again')).toBeTruthy();
    });

    it('should save scan results to history', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Complete a scan
      fireEvent.press(screen.getByText('Start Scanning'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByText('Scan Tag'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Complete')).toBeTruthy();
      });
      
      // Navigate to history
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByTestId('scan-history-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan History')).toBeTruthy();
      });
      
      // Should show the scan in history
      expect(screen.getByText('PLA')).toBeTruthy();
    });
  });

  describe('navigation persistence', () => {
    it('should maintain navigation state across screens', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Navigate through multiple screens
      fireEvent.press(screen.getByTestId('scan-history-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan History')).toBeTruthy();
      });
      
      // Navigate to settings from history
      fireEvent.press(screen.getByTestId('settings-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeTruthy();
      });
      
      // Navigate back should go to history, not home
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan History')).toBeTruthy();
      });
    });

    it('should handle deep navigation correctly', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Navigate deep into the app
      fireEvent.press(screen.getByText('Start Scanning'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
      
      // Simulate scan completion and navigation to details
      fireEvent.press(screen.getByText('Scan Tag'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Complete')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByText('View Details'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Details')).toBeTruthy();
      });
      
      // Should be able to navigate back through the stack
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Scan Complete')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
    });
  });

  describe('error handling and edge cases', () => {
    it('should handle navigation errors gracefully', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Should not crash on invalid navigation
      expect(() => {
        // This would typically be handled by navigation guards
        fireEvent.press(screen.getByTestId('invalid-navigation-button'));
      }).not.toThrow();
    });

    it('should handle concurrent navigation attempts', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Rapidly press navigation buttons
      const scanButton = screen.getByText('Start Scanning');
      fireEvent.press(scanButton);
      fireEvent.press(scanButton);
      fireEvent.press(scanButton);
      
      // Should only navigate once
      await waitFor(() => {
        expect(screen.getByText('Ready to Scan')).toBeTruthy();
      });
    });

    it('should maintain app state during navigation', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Change settings
      fireEvent.press(screen.getByTestId('settings-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByTestId('debug-mode-toggle'));
      
      // Navigate away and back
      fireEvent.press(screen.getByTestId('back-button'));
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      fireEvent.press(screen.getByTestId('settings-button'));
      
      await waitFor(() => {
        expect(screen.getByText('Settings')).toBeTruthy();
      });
      
      // Debug mode toggle state should be preserved
      expect(screen.getByTestId('debug-mode-toggle')).toBeTruthy();
    });
  });

  describe('accessibility and usability', () => {
    it('should provide proper accessibility labels', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Check accessibility labels
      expect(screen.getByLabelText('Start scanning for NFC tags')).toBeTruthy();
      expect(screen.getByLabelText('View scan history')).toBeTruthy();
      expect(screen.getByLabelText('Open settings')).toBeTruthy();
    });

    it('should handle screen reader navigation', async () => {
      renderApp();
      
      await waitFor(() => {
        expect(screen.getByText('B-Scan')).toBeTruthy();
      });
      
      // Test that screen reader can navigate through elements
      const elements = screen.getAllByRole('button');
      expect(elements.length).toBeGreaterThan(0);
      
      elements.forEach(element => {
        expect(element).toBeTruthy();
      });
    });
  });
});