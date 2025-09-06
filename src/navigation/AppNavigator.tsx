import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { RootStackParamList } from '../types/Navigation';

// Screen imports
import HomeScreen from '../screens/HomeScreen';
import DataBrowserScreen from '../screens/DataBrowserScreen';
import EntityDetailScreen from '../screens/EntityDetailScreen';
import ComponentDetailScreen from '../screens/ComponentDetailScreen';
import ScanHistoryScreen from '../screens/ScanHistoryScreen';
import ScanDetailScreen from '../screens/ScanDetailScreen';
import SettingsScreen from '../screens/SettingsScreen';
import ScanningScreen from '../screens/ScanningScreen';

const Stack = createNativeStackNavigator<RootStackParamList>();

const AppNavigator: React.FC = () => {
  return (
    <NavigationContainer>
      <Stack.Navigator 
        initialRouteName="Home"
        screenOptions={{
          headerStyle: {
            backgroundColor: '#6200EE',
          },
          headerTintColor: '#fff',
          headerTitleStyle: {
            fontWeight: 'bold',
          },
        }}
      >
        <Stack.Screen 
          name="Home" 
          component={HomeScreen} 
          options={{ 
            title: 'B-Scan',
            headerTitleAlign: 'center'
          }} 
        />
        <Stack.Screen 
          name="DataBrowser" 
          component={DataBrowserScreen} 
          options={{ title: 'Inventory Browser' }} 
        />
        <Stack.Screen 
          name="EntityDetail" 
          component={EntityDetailScreen} 
          options={{ title: 'Entity Details' }} 
        />
        <Stack.Screen 
          name="ComponentDetail" 
          component={ComponentDetailScreen} 
          options={{ title: 'Component Details' }} 
        />
        <Stack.Screen 
          name="ScanHistory" 
          component={ScanHistoryScreen} 
          options={{ title: 'Scan History' }} 
        />
        <Stack.Screen 
          name="ScanDetail" 
          component={ScanDetailScreen} 
          options={{ title: 'Scan Details' }} 
        />
        <Stack.Screen 
          name="Settings" 
          component={SettingsScreen} 
          options={{ title: 'Settings' }} 
        />
        <Stack.Screen 
          name="Scanning" 
          component={ScanningScreen} 
          options={{ 
            title: 'Scanning NFC Tag',
            headerBackVisible: false,
            gestureEnabled: false 
          }} 
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;