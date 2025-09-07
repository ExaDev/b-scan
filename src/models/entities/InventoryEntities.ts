/**
 * Core inventory entities - PhysicalComponent, InventoryItem, StockDefinition
 */

import { Entity, ValidationResult, generateId } from './Entity';
import { PropertyValue, Quantity, QuantityFactory, ContinuousQuantity, DiscreteQuantity } from './PropertyValue';
import { 
  EntityTypes, 
  TrackingMode, 
  StockMovementType, 
  CalibrationResult, 
  InferenceResult, 
  WeightUpdateResult 
} from './types';

/**
 * Physical component entity (RFID tags, filament, tools, etc.)
 */
export class PhysicalComponent extends Entity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.PHYSICAL_COMPONENT, label, properties);
  }

  // Convenience properties
  get manufacturer(): string | undefined {
    return this.getProperty<string>('manufacturer');
  }
  
  set manufacturer(value: string | undefined) {
    if (value !== undefined) this.setProperty('manufacturer', value);
  }

  get model(): string | undefined {
    return this.getProperty<string>('model');
  }
  
  set model(value: string | undefined) {
    if (value !== undefined) this.setProperty('model', value);
  }

  get serialNumber(): string | undefined {
    return this.getProperty<string>('serialNumber');
  }
  
  set serialNumber(value: string | undefined) {
    if (value !== undefined) this.setProperty('serialNumber', value);
  }

  get massGrams(): number | undefined {
    return this.getProperty<number>('massGrams');
  }
  
  set massGrams(value: number | undefined) {
    if (value !== undefined) this.setProperty('massGrams', value);
  }

  get category(): string | undefined {
    return this.getProperty<string>('category');
  }
  
  set category(value: string | undefined) {
    if (value !== undefined) this.setProperty('category', value);
  }

  copy(newId: string = generateId()): PhysicalComponent {
    return new PhysicalComponent(
      newId,
      this.label,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.label.trim()) {
      errors.push('Physical component must have a label');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Stock definition entity for describing types of items that can be stocked
 * Generic specifications of materials, components, tools, etc. that can exist in inventory
 * Actual inventory items reference these stock definitions via TRACKS relationships
 */
export class StockDefinition extends Entity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.STOCK_DEFINITION, label, properties);
  }

  // Core catalog properties
  get sku(): string | undefined {
    return this.getProperty<string>('sku');
  }
  
  set sku(value: string | undefined) {
    if (value !== undefined) this.setProperty('sku', value);
  }

  get manufacturer(): string | undefined {
    return this.getProperty<string>('manufacturer');
  }
  
  set manufacturer(value: string | undefined) {
    if (value !== undefined) this.setProperty('manufacturer', value);
  }

  get displayName(): string | undefined {
    return this.getProperty<string>('displayName');
  }
  
  set displayName(value: string | undefined) {
    if (value !== undefined) this.setProperty('displayName', value);
  }

  get description(): string | undefined {
    return this.getProperty<string>('description');
  }
  
  set description(value: string | undefined) {
    if (value !== undefined) this.setProperty('description', value);
  }

  get category(): string | undefined {
    return this.getProperty<string>('category');
  }
  
  set category(value: string | undefined) {
    if (value !== undefined) this.setProperty('category', value);
  }

  get productUrl(): string | undefined {
    return this.getProperty<string>('productUrl');
  }
  
  set productUrl(value: string | undefined) {
    if (value !== undefined) this.setProperty('productUrl', value);
  }

  // Physical properties using Quantity types
  get weight(): Quantity | undefined {
    return this.getProperty<Quantity>('weight');
  }
  
  set weight(value: Quantity | undefined) {
    if (value !== undefined) this.setProperty('weight', value);
  }

  get dimensions(): string | undefined {
    return this.getProperty<string>('dimensions');
  }
  
  set dimensions(value: string | undefined) {
    if (value !== undefined) this.setProperty('dimensions', value);
  }

  // Usage characteristics
  get consumable(): boolean {
    return this.getProperty<boolean>('consumable') ?? true;
  }
  
  set consumable(value: boolean) {
    this.setProperty('consumable', value);
  }

  get reusable(): boolean {
    return this.getProperty<boolean>('reusable') ?? false;
  }
  
  set reusable(value: boolean) {
    this.setProperty('reusable', value);
  }

  get recyclable(): boolean {
    return this.getProperty<boolean>('recyclable') ?? false;
  }
  
  set recyclable(value: boolean) {
    this.setProperty('recyclable', value);
  }

  // Material-specific properties
  get materialType(): string | undefined {
    return this.getProperty<string>('materialType');
  }
  
  set materialType(value: string | undefined) {
    if (value !== undefined) this.setProperty('materialType', value);
  }

  get colorName(): string | undefined {
    return this.getProperty<string>('colorName');
  }
  
  set colorName(value: string | undefined) {
    if (value !== undefined) this.setProperty('colorName', value);
  }

  get colorHex(): string | undefined {
    return this.getProperty<string>('colorHex');
  }
  
  set colorHex(value: string | undefined) {
    if (value !== undefined) this.setProperty('colorHex', value);
  }

  get colorCode(): string | undefined {
    return this.getProperty<string>('colorCode');
  }
  
  set colorCode(value: string | undefined) {
    if (value !== undefined) this.setProperty('colorCode', value);
  }

  // Temperature properties
  get minNozzleTemp(): number | undefined {
    return this.getProperty<number>('minNozzleTemp');
  }
  
  set minNozzleTemp(value: number | undefined) {
    if (value !== undefined) this.setProperty('minNozzleTemp', value);
  }

  get maxNozzleTemp(): number | undefined {
    return this.getProperty<number>('maxNozzleTemp');
  }
  
  set maxNozzleTemp(value: number | undefined) {
    if (value !== undefined) this.setProperty('maxNozzleTemp', value);
  }

  get bedTemp(): number | undefined {
    return this.getProperty<number>('bedTemp');
  }
  
  set bedTemp(value: number | undefined) {
    if (value !== undefined) this.setProperty('bedTemp', value);
  }

  get enclosureTemp(): number | undefined {
    return this.getProperty<number>('enclosureTemp');
  }
  
  set enclosureTemp(value: number | undefined) {
    if (value !== undefined) this.setProperty('enclosureTemp', value);
  }

  // Availability and pricing
  get available(): boolean {
    return this.getProperty<boolean>('available') ?? true;
  }
  
  set available(value: boolean) {
    this.setProperty('available', value);
  }

  get price(): number | undefined {
    return this.getProperty<number>('price');
  }
  
  set price(value: number | undefined) {
    if (value !== undefined) this.setProperty('price', value);
  }

  get currency(): string | undefined {
    return this.getProperty<string>('currency');
  }
  
  set currency(value: string | undefined) {
    if (value !== undefined) this.setProperty('currency', value);
  }

  // Alternative identifiers
  get alternativeIds(): Set<string> {
    const list = this.getProperty<string[]>('alternativeIds');
    return new Set(list || []);
  }
  
  set alternativeIds(value: Set<string>) {
    this.setProperty('alternativeIds', Array.from(value));
  }

  /**
   * Check if this item has temperature properties defined
   */
  hasTemperatureProperties(): boolean {
    return this.minNozzleTemp !== undefined || 
           this.maxNozzleTemp !== undefined || 
           this.bedTemp !== undefined || 
           this.enclosureTemp !== undefined;
  }

  /**
   * Check if this item is a material (vs packaging/component)
   */
  isMaterial(): boolean {
    return this.materialType !== undefined;
  }

  /**
   * Check if this item is packaging/component (vs material)
   */
  isPackaging(): boolean {
    if (this.isMaterial()) return false;
    
    const category = this.category?.toLowerCase();
    return category?.includes('spool') || 
           category?.includes('core') || 
           category?.includes('packaging');
  }

  copy(newId: string = generateId()): StockDefinition {
    return new StockDefinition(
      newId,
      this.label,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];
    
    if (!this.label.trim()) {
      errors.push('Catalog item must have a label');
    }
    
    if (!this.sku?.trim()) {
      errors.push('Catalog item must have a SKU');
    }
    
    if (!this.manufacturer?.trim()) {
      errors.push('Catalog item must have a manufacturer');
    }
    
    // Validate temperature ranges if provided
    const minTemp = this.minNozzleTemp;
    const maxTemp = this.maxNozzleTemp;
    if (minTemp !== undefined && maxTemp !== undefined && minTemp > maxTemp) {
      errors.push('Minimum nozzle temperature cannot be greater than maximum');
    }
    
    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}

/**
 * Inventory item entity for tracking quantities over time
 * Links to either unique physical items or fungible product types
 */
export class InventoryItem extends Entity {
  constructor(
    id: string = generateId(),
    label: string,
    public readonly trackingMode: TrackingMode = TrackingMode.DISCRETE,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, EntityTypes.INVENTORY_ITEM, label, properties);
    this.setProperty('trackingMode', trackingMode);
  }

  // Current quantity/state
  get currentQuantity(): number {
    return this.getProperty<number>('currentQuantity') ?? 0;
  }
  
  set currentQuantity(value: number) {
    this.setProperty('currentQuantity', value);
  }

  get currentWeight(): number | undefined {
    return this.getProperty<number>('currentWeight');
  }
  
  set currentWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('currentWeight', value);
  }

  // Reorder management
  get reorderLevel(): number | undefined {
    return this.getProperty<number>('reorderLevel');
  }
  
  set reorderLevel(value: number | undefined) {
    if (value !== undefined) this.setProperty('reorderLevel', value);
  }

  get reorderQuantity(): number | undefined {
    return this.getProperty<number>('reorderQuantity');
  }
  
  set reorderQuantity(value: number | undefined) {
    if (value !== undefined) this.setProperty('reorderQuantity', value);
  }

  // Component weights for inference
  get tareWeight(): number | undefined {
    return this.getProperty<number>('tareWeight');
  }
  
  set tareWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('tareWeight', value);
  }

  get unitWeight(): number | undefined {
    return this.getProperty<number>('unitWeight');
  }
  
  set unitWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('unitWeight', value);
  }

  // Storage and location
  get location(): string | undefined {
    return this.getProperty<string>('location');
  }
  
  set location(value: string | undefined) {
    if (value !== undefined) this.setProperty('location', value);
  }

  get notes(): string | undefined {
    return this.getProperty<string>('notes');
  }
  
  set notes(value: string | undefined) {
    if (value !== undefined) this.setProperty('notes', value);
  }

  // Composite consumption properties
  get isConsumable(): boolean {
    return this.getProperty<boolean>('isConsumable') ?? true;
  }
  
  set isConsumable(value: boolean) {
    this.setProperty('isConsumable', value);
  }

  get fixedMass(): number | undefined {
    return this.getProperty<number>('fixedMass');
  }
  
  set fixedMass(value: number | undefined) {
    if (value !== undefined) this.setProperty('fixedMass', value);
  }

  get lastCompositeWeight(): number | undefined {
    return this.getProperty<number>('lastCompositeWeight');
  }
  
  set lastCompositeWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('lastCompositeWeight', value);
  }

  get componentType(): string | undefined {
    return this.getProperty<string>('componentType');
  }
  
  set componentType(value: string | undefined) {
    if (value !== undefined) this.setProperty('componentType', value);
  }

  // Unit weight calibration properties
  get containerWeight(): number | undefined {
    return this.getProperty<number>('containerWeight') ?? this.tareWeight;
  }
  
  set containerWeight(value: number | undefined) {
    if (value !== undefined) {
      this.setProperty('containerWeight', value);
      this.tareWeight = value; // Keep both in sync
    }
  }

  get lastCalibratedAt(): string | undefined {
    return this.getProperty<string>('lastCalibratedAt');
  }
  
  set lastCalibratedAt(value: string | undefined) {
    if (value !== undefined) this.setProperty('lastCalibratedAt', value);
  }

  get calibrationMethod(): string | undefined {
    return this.getProperty<string>('calibrationMethod');
  }
  
  set calibrationMethod(value: string | undefined) {
    if (value !== undefined) this.setProperty('calibrationMethod', value);
  }

  get calibrationConfidence(): number | undefined {
    return this.getProperty<number>('calibrationConfidence');
  }
  
  set calibrationConfidence(value: number | undefined) {
    if (value !== undefined) this.setProperty('calibrationConfidence', value);
  }

  /**
   * Calibrate unit weight from known count and total weight
   * Perfect for: "I have 100 screws weighing 247g total, box weighs 47g"
   */
  calibrateUnitWeight(
    totalWeight: number,
    knownQuantity: number,
    containerWeight?: number
  ): CalibrationResult {
    if (knownQuantity <= 0) {
      return {
        success: false,
        error: 'Known quantity must be greater than zero'
      };
    }

    const tare = containerWeight ?? this.containerWeight ?? 0;
    const netWeight = totalWeight - tare;

    if (netWeight <= 0) {
      return {
        success: false,
        error: 'Net weight must be positive (check container weight)'
      };
    }

    const unitWt = netWeight / knownQuantity;

    // Update stored values
    this.unitWeight = unitWt;
    this.tareWeight = tare;
    this.containerWeight = tare;
    this.currentQuantity = knownQuantity;
    this.currentWeight = totalWeight;
    this.lastCalibratedAt = new Date().toISOString();
    this.calibrationMethod = 'COUNTED_AND_WEIGHED';
    this.calibrationConfidence = 95; // High confidence for direct measurement

    return {
      success: true,
      unitWeight: unitWt,
      netWeight: netWeight,
      accuracy: 95,
      method: 'DIRECT_CALIBRATION'
    };
  }

  /**
   * Learn container weight from empty container measurement
   * Used when initially didn't know box weight
   */
  learnContainerWeight(emptyContainerWeight: number): CalibrationResult {
    const oldTare = this.tareWeight ?? 0;
    const oldUnitWeight = this.unitWeight;
    const lastWeight = this.currentWeight;
    const lastQuantity = this.currentQuantity;

    if (oldUnitWeight === undefined || lastWeight === undefined) {
      return {
        success: false,
        error: 'No previous calibration data available'
      };
    }

    // Recalculate unit weight with accurate container weight
    const correctedNetWeight = lastWeight - emptyContainerWeight;
    const correctedUnitWeight = lastQuantity > 0 
      ? correctedNetWeight / lastQuantity 
      : oldUnitWeight;

    // Update values
    this.containerWeight = emptyContainerWeight;
    this.tareWeight = emptyContainerWeight;
    this.unitWeight = correctedUnitWeight;
    this.calibrationMethod = 'CONTAINER_WEIGHT_LEARNED';
    this.calibrationConfidence = 98; // Even higher confidence with known container

    return {
      success: true,
      unitWeight: correctedUnitWeight,
      netWeight: correctedNetWeight,
      accuracy: 98,
      method: 'CONTAINER_LEARNING'
    };
  }

  /**
   * Perform bidirectional inference between weight and quantity
   */
  inferFromWeight(totalWeight: number): InferenceResult | undefined {
    const tare = this.containerWeight ?? this.tareWeight;
    const unitWt = this.unitWeight;
    
    if (tare === undefined || unitWt === undefined) return undefined;

    const netWeight = totalWeight - tare;
    const inferredQuantity = this.trackingMode === TrackingMode.DISCRETE
      ? Math.round(netWeight / unitWt)
      : netWeight / unitWt;

    return {
      inferredQuantity,
      inferredWeight: totalWeight,
      confidence: this.calculateInferenceConfidence(netWeight, unitWt),
      method: 'weight_inference'
    };
  }

  /**
   * Perform bidirectional inference from quantity to weight
   */
  inferFromQuantity(quantity: number): InferenceResult | undefined {
    const tare = this.containerWeight ?? this.tareWeight;
    const unitWt = this.unitWeight;
    
    if (tare === undefined || unitWt === undefined) return undefined;

    const netWeight = quantity * unitWt;
    const inferredWeight = netWeight + tare;

    return {
      inferredQuantity: quantity,
      inferredWeight,
      confidence: 100, // Quantity-based inference is exact
      method: 'quantity_inference'
    };
  }

  /**
   * Update quantity and weight from new total weight measurement
   * Perfect for: "Box now weighs 187g, how many screws are left?"
   */
  updateFromWeightMeasurement(newTotalWeight: number): WeightUpdateResult {
    const inference = this.inferFromWeight(newTotalWeight);
    if (!inference) {
      return {
        success: false,
        newQuantity: 0,
        quantityConsumed: 0,
        confidence: 0,
        inferenceMethod: '',
        error: 'Cannot infer quantity - missing calibration data'
      };
    }

    const oldQuantity = this.currentQuantity;
    const consumedQuantity = oldQuantity - inference.inferredQuantity;

    // Update current values
    this.currentQuantity = inference.inferredQuantity;
    this.currentWeight = newTotalWeight;

    return {
      success: true,
      newQuantity: inference.inferredQuantity,
      quantityConsumed: consumedQuantity,
      confidence: inference.confidence,
      inferenceMethod: inference.method
    };
  }

  private calculateInferenceConfidence(netWeight: number, unitWeight: number): number {
    // Base confidence on calibration quality and measurement precision
    const baseConfidence = this.calibrationConfidence ?? 70;

    // Factor in measurement precision for discrete items
    if (this.trackingMode === TrackingMode.DISCRETE) {
      const exactUnits = netWeight / unitWeight;
      const roundedUnits = Math.round(exactUnits);
      const error = Math.abs(exactUnits - roundedUnits);
      const precisionFactor = 1 - error;
      return Math.max(50, Math.min(100, baseConfidence * precisionFactor));
    }
    
    return Math.max(70, Math.min(100, baseConfidence));
  }

  /**
   * Check if this item is properly calibrated for weight-based inference
   */
  isProperlyCalibrated(): boolean {
    const unitWt = this.unitWeight;
    const container = this.containerWeight ?? this.tareWeight;
    return unitWt !== undefined && unitWt > 0 && container !== undefined;
  }

  /**
   * Get calibration status summary
   */
  getCalibrationStatus(): string {
    if (!this.isProperlyCalibrated()) return 'Not calibrated';
    
    switch (this.calibrationMethod) {
      case 'COUNTED_AND_WEIGHED': return 'Calibrated (counted & weighed)';
      case 'CONTAINER_WEIGHT_LEARNED': return 'Calibrated (container learned)';
      case 'ESTIMATED': return 'Estimated calibration';
      default: return 'Calibrated (method unknown)';
    }
  }

  copy(newId: string = generateId()): InventoryItem {
    return new InventoryItem(
      newId,
      this.label,
      this.trackingMode,
      new Map(this.properties)
    );
  }

  validate(): ValidationResult {
    const errors: string[] = [];

    if (!this.label.trim()) {
      errors.push('Inventory item must have a label');
    }
    
    if (this.currentQuantity < 0) {
      errors.push('Current quantity cannot be negative');
    }

    // Validate calibration consistency
    const unitWt = this.unitWeight;
    const tare = this.containerWeight ?? this.tareWeight;
    
    if (unitWt !== undefined && unitWt <= 0) {
      errors.push('Unit weight must be positive');
    }
    
    if (tare !== undefined && tare < 0) {
      errors.push('Container/tare weight cannot be negative');
    }

    return errors.length === 0 
      ? ValidationResult.valid() 
      : ValidationResult.invalid(...errors);
  }
}