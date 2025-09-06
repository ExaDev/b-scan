export interface FilamentInfo {
  tagUid: string;
  trayUid: string;
  tagFormat: TagFormat;
  manufacturerName: string;
  filamentType: string;
  colorHex: string;
  colorName: string;
  spoolWeight: number;
  filamentDiameter: number;
  filamentLength: number;
  productionDate: string;
  minTemperature: number;
  maxTemperature: number;
  bedTemperature: number;
  dryingTemperature: number;
  dryingTime: number;
}

export enum TagFormat {
  BAMBU_LAB = 'BAMBU_LAB',
  CREALITY = 'CREALITY',
  OPEN_TAG = 'OPEN_TAG',
  UNKNOWN = 'UNKNOWN',
}

export interface ScanProgress {
  stage: ScanStage;
  percentage: number;
  currentSector: number;
  statusMessage: string;
}

export enum ScanStage {
  INITIALIZING = 'INITIALIZING',
  AUTHENTICATING = 'AUTHENTICATING',
  READING_DATA = 'READING_DATA',
  PARSING_DATA = 'PARSING_DATA',
  COMPLETED = 'COMPLETED',
  ERROR = 'ERROR',
}

export type TagReadResult =
  | {type: 'SUCCESS'; filamentInfo: FilamentInfo}
  | {type: 'NO_NFC'}
  | {type: 'INVALID_TAG'}
  | {type: 'READ_ERROR'; error: string}
  | {type: 'AUTHENTICATION_FAILED'}
  | {type: 'PARSING_ERROR'; error: string};

export interface ScanHistoryEntry {
  id: string;
  timestamp: number;
  filamentInfo?: FilamentInfo;
  result: TagReadResult['type'];
  error?: string;
}

export interface GraphEntity {
  id: string;
  type: EntityType;
  createdAt: number;
  updatedAt: number;
}

export enum EntityType {
  PHYSICAL_COMPONENT = 'PHYSICAL_COMPONENT',
  INVENTORY_ITEM = 'INVENTORY_ITEM',
  IDENTIFIER = 'IDENTIFIER',
  ACTIVITY = 'ACTIVITY',
  STOCK_DEFINITION = 'STOCK_DEFINITION',
}

export interface PhysicalComponent extends GraphEntity {
  type: EntityType.PHYSICAL_COMPONENT;
  filamentInfo: FilamentInfo;
  currentWeight?: number;
  notes?: string;
}

export interface InventoryItem extends GraphEntity {
  type: EntityType.INVENTORY_ITEM;
  quantity: number;
  location?: string;
  lastUpdated: number;
}

export interface Identifier extends GraphEntity {
  type: EntityType.IDENTIFIER;
  value: string;
  identifierType: 'RFID' | 'BARCODE' | 'QR_CODE';
}

export interface Activity extends GraphEntity {
  type: EntityType.ACTIVITY;
  activityType: 'SCAN' | 'UPDATE' | 'CREATE' | 'DELETE';
  description: string;
  relatedEntityId?: string;
}
