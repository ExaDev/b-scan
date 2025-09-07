import React from 'react';
import {NavigationContainer} from '@react-navigation/native';
import {createNativeStackNavigator} from '@react-navigation/native-stack';
import {createBottomTabNavigator} from '@react-navigation/bottom-tabs';
import {useTheme} from 'react-native-paper';
import Icon from 'react-native-vector-icons/MaterialIcons';
import {RootStackParamList, TabParamList} from '../types/Navigation';

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
const Tab = createBottomTabNavigator<TabParamList>();

function MainTabNavigator() {
  const theme = useTheme();
  
  return (
    <Tab.Navigator
      initialRouteName="Home"
      screenOptions={({route}) => ({
        tabBarIcon: ({focused, color, size}) => {
          let iconName: string;
          
          switch (route.name) {
            case 'Home':
              iconName = focused ? 'inventory' : 'inventory-2';
              break;
            case 'History':
              iconName = focused ? 'history' : 'history';
              break;
            case 'Settings':
              iconName = focused ? 'settings' : 'settings';
              break;
            default:
              iconName = 'help';
          }
          
          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: theme.colors.primary,
        tabBarInactiveTintColor: theme.colors.onSurfaceVariant,
        tabBarStyle: {
          backgroundColor: theme.colors.surface,
          borderTopColor: theme.colors.outline,
        },
        headerStyle: {
          backgroundColor: theme.colors.primary,
        },
        headerTintColor: theme.colors.onPrimary,
        headerTitleStyle: {
          fontWeight: 'bold',
        },
      })}>
      <Tab.Screen 
        name="Home" 
        component={HomeScreen}
        options={{
          title: 'B-Scan',
          headerTitle: 'B-Scan',
        }}
      />
      <Tab.Screen 
        name="History" 
        component={ScanHistoryScreen}
        options={{
          title: 'History',
          headerTitle: 'Scan History',
        }}
      />
      <Tab.Screen 
        name="Settings" 
        component={SettingsScreen}
        options={{
          title: 'Settings',
          headerTitle: 'Settings',
        }}
      />
    </Tab.Navigator>
  );
}

const AppNavigator: React.FC = () => {
  const theme = useTheme();
  
  return (
    <NavigationContainer>
      <Stack.Navigator
        initialRouteName="MainTabs"
        screenOptions={{
          headerStyle: {
            backgroundColor: theme.colors.primary,
          },
          headerTintColor: theme.colors.onPrimary,
          headerTitleStyle: {
            fontWeight: 'bold',
          },
        }}>
        <Stack.Screen
          name="MainTabs"
          component={MainTabNavigator}
          options={{headerShown: false}}
        />
        <Stack.Screen
          name="DataBrowser"
          component={DataBrowserScreen}
          options={{title: 'Inventory Browser'}}
        />
        <Stack.Screen
          name="EntityDetail"
          component={EntityDetailScreen}
          options={{title: 'Details'}}
        />
        <Stack.Screen
          name="ComponentDetail"
          component={ComponentDetailScreen}
          options={{title: 'Component Details'}}
        />
        <Stack.Screen
          name="ScanDetail"
          component={ScanDetailScreen}
          options={{title: 'Scan Details'}}
        />
        <Stack.Screen
          name="Scanning"
          component={ScanningScreen}
          options={{
            title: 'Scanning NFC Tag',
            headerBackVisible: false,
            gestureEnabled: false,
          }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
};

export default AppNavigator;
