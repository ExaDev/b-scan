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

export type NavigationProps = {
  navigation: any;
  route: any;
};

export type DetailType = 'component' | 'tag' | 'scan' | 'inventory' | 'sku';

export type ViewMode = 'inventory' | 'catalog' | 'tags' | 'scans';

export type SortProperty = 'first_scan' | 'last_scan' | 'name' | 'success_rate' | 'color' | 'material_type';

export type SortDirection = 'ascending' | 'descending';

export type GroupByOption = 'none' | 'color' | 'base_material' | 'material_series';
