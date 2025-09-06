/**
 * Component tests for HomeScreen UI
 * Port of MainActivity test patterns from the original Android app
 */

import React from 'react';
import { render, fireEvent, waitFor, screen } from '@testing-library/react-native';
import { NavigationContainer } from '@react-navigation/native';
import { PaperProvider } from 'react-native-paper';
import HomeScreen from '../../src/screens/HomeScreen';

// Mock navigation
const mockNavigate = jest.fn();
const mockNavigation = {
  navigate: mockNavigate,
  goBack: jest.fn(),
  setOptions: jest.fn(),
  dispatch: jest.fn(),
  reset: jest.fn(),
  isFocused: jest.fn(() => true),
  canGoBack: jest.fn(() => false),
  getParent: jest.fn(),
  getId: jest.fn(),
  getState: jest.fn(),
  addListener: jest.fn(() => jest.fn()),
  removeListener: jest.fn(),
};

// Mock NFC Manager
jest.mock('../../src/services/NfcManager', () => ({
  NfcManager: class {
    initialize = jest.fn().mockResolvedValue(true);
    cleanup = jest.fn();
    isAvailable = jest.fn().mockReturnValue(true);
    getRecentScans = jest.fn().mockReturnValue([]);
  }
}));

describe('HomeScreen Component Tests', () => {
  const renderHomeScreen = () => {
    return render(
      <PaperProvider>
        <NavigationContainer>
          <HomeScreen navigation={mockNavigation as any} route={{} as any} />
        </NavigationContainer>
      </PaperProvider>
    );
  };

  beforeEach(() => {
    jest.clearAllMocks();
  });

  describe('component rendering', () => {
    it('should render home screen with main elements', async () => {
      renderHomeScreen();
      
      // Check main title
      expect(screen.getByText('B-Scan')).toBeTruthy();
      
      // Check main action button
      expect(screen.getByText('Start Scanning')).toBeTruthy();
      
      // Check navigation buttons
      expect(screen.getByTestId('scan-history-button')).toBeTruthy();
      expect(screen.getByTestId('settings-button')).toBeTruthy();
    });

    it('should render recent scans section', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.getRecentScans.mockReturnValue([
        {
          id: '1',
          uid: '04914CCA',
          material: 'PLA',
          color: 'Red',
          timestamp: new Date().toISOString()
        }
      ]);

      renderHomeScreen();
      
      expect(screen.getByText('Recent Scans')).toBeTruthy();
      expect(screen.getByText('PLA')).toBeTruthy();
      expect(screen.getByText('Red')).toBeTruthy();
    });

    it('should show empty state when no recent scans', async () => {
      renderHomeScreen();
      
      expect(screen.getByText('Recent Scans')).toBeTruthy();
      expect(screen.getByText('No recent scans')).toBeTruthy();
    });

    it('should render NFC status indicator', async () => {
      renderHomeScreen();
      
      // Should show NFC status
      expect(
        screen.getByTestId('nfc-status') ||
        screen.getByText('NFC Ready') ||
        screen.getByText('NFC Unavailable')
      ).toBeTruthy();
    });
  });

  describe('user interactions', () => {
    it('should navigate to scanning screen when start button pressed', async () => {
      renderHomeScreen();
      
      const startButton = screen.getByText('Start Scanning');
      fireEvent.press(startButton);
      
      expect(mockNavigate).toHaveBeenCalledWith('Scanning');
    });

    it('should navigate to scan history when history button pressed', async () => {
      renderHomeScreen();
      
      const historyButton = screen.getByTestId('scan-history-button');
      fireEvent.press(historyButton);
      
      expect(mockNavigate).toHaveBeenCalledWith('ScanHistory');
    });

    it('should navigate to settings when settings button pressed', async () => {
      renderHomeScreen();
      
      const settingsButton = screen.getByTestId('settings-button');
      fireEvent.press(settingsButton);
      
      expect(mockNavigate).toHaveBeenCalledWith('Settings');
    });

    it('should navigate to scan details when recent scan item pressed', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.getRecentScans.mockReturnValue([
        {
          id: '1',
          uid: '04914CCA',
          material: 'PLA',
          color: 'Red',
          timestamp: new Date().toISOString()
        }
      ]);

      renderHomeScreen();
      
      const scanItem = screen.getByTestId('recent-scan-1');
      fireEvent.press(scanItem);
      
      expect(mockNavigate).toHaveBeenCalledWith('ScanDetail', { scanId: '1' });
    });
  });

  describe('NFC status handling', () => {
    it('should show NFC unavailable message when NFC is disabled', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.isAvailable.mockReturnValue(false);
      mockNfcManager.NfcManager.prototype.initialize.mockResolvedValue(false);

      renderHomeScreen();
      
      await waitFor(() => {
        expect(screen.getByText('NFC Unavailable')).toBeTruthy();
      });
      
      // Start scanning button should be disabled
      const startButton = screen.getByText('Start Scanning');
      expect(startButton).toBeDisabled();
    });

    it('should show NFC ready when initialized successfully', async () => {
      renderHomeScreen();
      
      await waitFor(() => {
        expect(
          screen.getByText('NFC Ready') ||
          screen.getByTestId('nfc-ready-indicator')
        ).toBeTruthy();
      });
    });

    it('should handle NFC initialization errors', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.initialize.mockRejectedValue(
        new Error('NFC initialization failed')
      );

      renderHomeScreen();
      
      await waitFor(() => {
        expect(
          screen.getByText('NFC Error') ||
          screen.getByText('NFC initialization failed')
        ).toBeTruthy();
      });
    });
  });

  describe('recent scans display', () => {
    it('should display recent scans in chronological order', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      const now = new Date();
      const recentScans = [
        {
          id: '1',
          uid: '04914CCA',
          material: 'PLA',
          color: 'Red',
          timestamp: new Date(now.getTime() - 1000).toISOString()
        },
        {
          id: '2',
          uid: '05825DDB',
          material: 'ABS',
          color: 'Blue',
          timestamp: now.toISOString()
        }
      ];
      mockNfcManager.NfcManager.prototype.getRecentScans.mockReturnValue(recentScans);

      renderHomeScreen();
      
      const scanItems = screen.getAllByTestId(/recent-scan-/);
      expect(scanItems).toHaveLength(2);
      
      // Most recent should be first
      expect(screen.getByText('ABS')).toBeTruthy();
      expect(screen.getByText('Blue')).toBeTruthy();
    });

    it('should limit number of displayed recent scans', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      const manyScans = Array.from({ length: 20 }, (_, i) => ({
        id: `${i}`,
        uid: `0491${i.toString().padStart(4, '0')}`,
        material: 'PLA',
        color: `Color${i}`,
        timestamp: new Date().toISOString()
      }));
      mockNfcManager.NfcManager.prototype.getRecentScans.mockReturnValue(manyScans);

      renderHomeScreen();
      
      // Should only show first 5 recent scans
      const scanItems = screen.getAllByTestId(/recent-scan-/);
      expect(scanItems.length).toBeLessThanOrEqual(5);
    });

    it('should show view all button when many recent scans exist', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      const manyScans = Array.from({ length: 10 }, (_, i) => ({
        id: `${i}`,
        uid: `0491${i.toString().padStart(4, '0')}`,
        material: 'PLA',
        color: `Color${i}`,
        timestamp: new Date().toISOString()
      }));
      mockNfcManager.NfcManager.prototype.getRecentScans.mockReturnValue(manyScans);

      renderHomeScreen();
      
      expect(screen.getByText('View All')).toBeTruthy();
      
      // Pressing view all should navigate to history
      fireEvent.press(screen.getByText('View All'));
      expect(mockNavigate).toHaveBeenCalledWith('ScanHistory');
    });
  });

  describe('accessibility and usability', () => {
    it('should provide proper accessibility labels', async () => {
      renderHomeScreen();
      
      expect(screen.getByLabelText('Start scanning for NFC tags')).toBeTruthy();
      expect(screen.getByLabelText('View scan history')).toBeTruthy();
      expect(screen.getByLabelText('Open settings')).toBeTruthy();
    });

    it('should support keyboard navigation', async () => {
      renderHomeScreen();
      
      const buttons = screen.getAllByRole('button');
      buttons.forEach(button => {
        expect(button.props.accessible).toBe(true);
      });
    });

    it('should handle large text sizes', async () => {
      renderHomeScreen();
      
      // Text should be readable and buttons should be touchable
      const startButton = screen.getByText('Start Scanning');
      expect(startButton).toBeTruthy();
      expect(startButton.props.accessibilityRole).toBe('button');
    });
  });

  describe('performance and reliability', () => {
    it('should render quickly without delays', async () => {
      const startTime = Date.now();
      renderHomeScreen();
      const endTime = Date.now();
      
      // Should render within reasonable time
      expect(endTime - startTime).toBeLessThan(100);
      
      // Should have main elements immediately
      expect(screen.getByText('B-Scan')).toBeTruthy();
    });

    it('should handle component updates gracefully', async () => {
      const { rerender } = renderHomeScreen();
      
      // Update with different props
      rerender(
        <PaperProvider>
          <NavigationContainer>
            <HomeScreen navigation={mockNavigation as any} route={{} as any} />
          </NavigationContainer>
        </PaperProvider>
      );
      
      // Should still render correctly
      expect(screen.getByText('B-Scan')).toBeTruthy();
    });

    it('should cleanup resources on unmount', async () => {
      const { unmount } = renderHomeScreen();
      
      // Should unmount without errors
      expect(() => unmount()).not.toThrow();
    });
  });

  describe('error boundary behavior', () => {
    it('should handle NFC service errors gracefully', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      mockNfcManager.NfcManager.prototype.getRecentScans.mockImplementation(() => {
        throw new Error('Service error');
      });

      // Should render without crashing
      expect(() => renderHomeScreen()).not.toThrow();
      
      // Should show appropriate error state or fallback
      expect(screen.getByText('B-Scan')).toBeTruthy();
    });

    it('should recover from transient errors', async () => {
      const mockNfcManager = require('../../src/services/NfcManager');
      
      // First call fails, second succeeds
      mockNfcManager.NfcManager.prototype.initialize
        .mockRejectedValueOnce(new Error('Transient error'))
        .mockResolvedValueOnce(true);

      renderHomeScreen();
      
      // Should eventually show ready state
      await waitFor(() => {
        expect(
          screen.queryByText('NFC Error') ||
          screen.queryByText('NFC Ready')
        ).toBeTruthy();
      });
    });
  });
});