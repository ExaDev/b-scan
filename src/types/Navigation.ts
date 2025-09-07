import type {NativeStackNavigationProp} from '@react-navigation/native-stack';
import type {BottomTabNavigationProp} from '@react-navigation/bottom-tabs';
import type {CompositeNavigationProp} from '@react-navigation/native';
import type {RouteProp} from '@react-navigation/native';

export type RootStackParamList = {
  MainTabs: undefined;
  DataBrowser: undefined;
  EntityDetail: {entityId: string; entityType: string};
  ComponentDetail: {identifier: string};
  ScanDetail: {scanId: string};
  Scanning: {tagUid?: string};
};

export type TabParamList = {
  Home: undefined;
  History: undefined;
  Settings: undefined;
};

export type RootStackNavigationProp = NativeStackNavigationProp<RootStackParamList>;
export type TabNavigationProp = BottomTabNavigationProp<TabParamList>;

export type NavigationProps<
  T extends keyof RootStackParamList = keyof RootStackParamList
> = {
  navigation: NativeStackNavigationProp<RootStackParamList, T>;
  route: RouteProp<RootStackParamList, T>;
};

export type TabNavigationProps<
  T extends keyof TabParamList = keyof TabParamList
> = {
  navigation: CompositeNavigationProp<
    BottomTabNavigationProp<TabParamList, T>,
    NativeStackNavigationProp<RootStackParamList>
  >;
  route: RouteProp<TabParamList, T>;
};

export type DetailType = 'component' | 'tag' | 'scan' | 'inventory' | 'sku';

export type ViewMode = 'inventory' | 'catalog' | 'tags' | 'scans';

export type SortProperty = 'first_scan' | 'last_scan' | 'name' | 'success_rate' | 'color' | 'material_type';

export type SortDirection = 'ascending' | 'descending';

export type GroupByOption = 'none' | 'color' | 'base_material' | 'material_series';
