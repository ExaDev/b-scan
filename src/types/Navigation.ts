export type RootStackParamList = {
  Home: undefined;
  DataBrowser: undefined;
  EntityDetail: {entityId: string; entityType: string};
  ComponentDetail: {identifier: string};
  ScanHistory: undefined;
  ScanDetail: {scanId: string};
  Settings: undefined;
  Scanning: {tagUid?: string};
};

export type NavigationProps = {
  navigation: any;
  route: any;
};
