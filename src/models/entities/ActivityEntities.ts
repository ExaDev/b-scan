/**
 * Specialized activity entities for inventory management
 */

import { Activity } from './CoreEntities';
import { PropertyValue } from './PropertyValue';
import { generateId } from './Entity';
import { 
  ActivityTypes, 
  StockMovementType, 
  DistributionMethod, 
  CalibrationResult 
} from './types';

/**
 * Calibration activity - establishes weight/quantity relationships
 */
export class CalibrationActivity extends Activity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, ActivityTypes.CALIBRATION, label, properties);
  }

  // Calibration inputs
  get totalWeight(): number {
    return this.getProperty<number>('totalWeight') ?? 0;
  }
  
  set totalWeight(value: number) {
    this.setProperty('totalWeight', value);
  }

  get tareWeight(): number | undefined {
    return this.getProperty<number>('tareWeight');
  }
  
  set tareWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('tareWeight', value);
  }

  get knownQuantity(): number {
    return this.getProperty<number>('knownQuantity') ?? 0;
  }
  
  set knownQuantity(value: number) {
    this.setProperty('knownQuantity', value);
  }

  // Calibration results
  get calculatedUnitWeight(): number | undefined {
    return this.getProperty<number>('calculatedUnitWeight');
  }
  
  set calculatedUnitWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('calculatedUnitWeight', value);
  }

  get calculatedNetWeight(): number | undefined {
    return this.getProperty<number>('calculatedNetWeight');
  }
  
  set calculatedNetWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('calculatedNetWeight', value);
  }

  get calibrationAccuracy(): number | undefined {
    return this.getProperty<number>('calibrationAccuracy');
  }
  
  set calibrationAccuracy(value: number | undefined) {
    if (value !== undefined) this.setProperty('calibrationAccuracy', value);
  }

  /**
   * Perform calibration calculation
   */
  performCalibration(): CalibrationResult {
    const tare = this.tareWeight ?? 0;
    const net = this.totalWeight - tare;

    if (this.knownQuantity <= 0) {
      return {
        success: false,
        error: 'Known quantity must be greater than zero'
      };
    }

    const unitWt = net / this.knownQuantity;

    this.calculatedNetWeight = net;
    this.calculatedUnitWeight = unitWt;
    this.calibrationAccuracy = unitWt > 0 ? 100 : 0;

    return {
      success: true,
      unitWeight: unitWt,
      netWeight: net,
      accuracy: this.calibrationAccuracy ?? 0
    };
  }
}

/**
 * Measurement activity with bidirectional inference
 */
export class MeasurementActivity extends Activity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, ActivityTypes.MEASUREMENT, label, properties);
  }

  // User inputs (either weight OR quantity)
  get providedWeight(): number | undefined {
    return this.getProperty<number>('providedWeight');
  }
  
  set providedWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('providedWeight', value);
  }

  get providedQuantity(): number | undefined {
    return this.getProperty<number>('providedQuantity');
  }
  
  set providedQuantity(value: number | undefined) {
    if (value !== undefined) this.setProperty('providedQuantity', value);
  }

  // Inferred values
  get inferredWeight(): number | undefined {
    return this.getProperty<number>('inferredWeight');
  }
  
  set inferredWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('inferredWeight', value);
  }

  get inferredQuantity(): number | undefined {
    return this.getProperty<number>('inferredQuantity');
  }
  
  set inferredQuantity(value: number | undefined) {
    if (value !== undefined) this.setProperty('inferredQuantity', value);
  }

  get confidence(): number | undefined {
    return this.getProperty<number>('confidence');
  }
  
  set confidence(value: number | undefined) {
    if (value !== undefined) this.setProperty('confidence', value);
  }

  get inferenceMethod(): string | undefined {
    return this.getProperty<string>('inferenceMethod');
  }
  
  set inferenceMethod(value: string | undefined) {
    if (value !== undefined) this.setProperty('inferenceMethod', value);
  }

  // Previous values for change calculation
  get previousWeight(): number | undefined {
    return this.getProperty<number>('previousWeight');
  }
  
  set previousWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('previousWeight', value);
  }

  get previousQuantity(): number | undefined {
    return this.getProperty<number>('previousQuantity');
  }
  
  set previousQuantity(value: number | undefined) {
    if (value !== undefined) this.setProperty('previousQuantity', value);
  }

  get weightChange(): number | undefined {
    return this.getProperty<number>('weightChange');
  }
  
  set weightChange(value: number | undefined) {
    if (value !== undefined) this.setProperty('weightChange', value);
  }

  get quantityChange(): number | undefined {
    return this.getProperty<number>('quantityChange');
  }
  
  set quantityChange(value: number | undefined) {
    if (value !== undefined) this.setProperty('quantityChange', value);
  }
}

/**
 * Stock movement activity - records inventory changes
 */
export class StockMovementActivity extends Activity {
  constructor(
    id: string = generateId(),
    public readonly movementType: StockMovementType,
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, ActivityTypes.STOCK_MOVEMENT, label, properties);
    this.setProperty('movementType', movementType);
  }

  get quantityChange(): number {
    return this.getProperty<number>('quantityChange') ?? 0;
  }
  
  set quantityChange(value: number) {
    this.setProperty('quantityChange', value);
  }

  get weightChange(): number | undefined {
    return this.getProperty<number>('weightChange');
  }
  
  set weightChange(value: number | undefined) {
    if (value !== undefined) this.setProperty('weightChange', value);
  }

  get newQuantity(): number {
    return this.getProperty<number>('newQuantity') ?? 0;
  }
  
  set newQuantity(value: number) {
    this.setProperty('newQuantity', value);
  }

  get newWeight(): number | undefined {
    return this.getProperty<number>('newWeight');
  }
  
  set newWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('newWeight', value);
  }

  get reason(): string | undefined {
    return this.getProperty<string>('reason');
  }
  
  set reason(value: string | undefined) {
    if (value !== undefined) this.setProperty('reason', value);
  }

  get batchNumber(): string | undefined {
    return this.getProperty<string>('batchNumber');
  }
  
  set batchNumber(value: string | undefined) {
    if (value !== undefined) this.setProperty('batchNumber', value);
  }

  get cost(): number | undefined {
    return this.getProperty<number>('cost');
  }
  
  set cost(value: number | undefined) {
    if (value !== undefined) this.setProperty('cost', value);
  }

  get supplier(): string | undefined {
    return this.getProperty<string>('supplier');
  }
  
  set supplier(value: string | undefined) {
    if (value !== undefined) this.setProperty('supplier', value);
  }
}

/**
 * Consumption distribution activity - records distribution of composite measurements
 * across multiple consumable entities (like splitting a bill between participants)
 */
export class ConsumptionDistributionActivity extends Activity {
  constructor(
    id: string = generateId(),
    label: string,
    properties: Map<string, PropertyValue> = new Map()
  ) {
    super(id, ActivityTypes.CONSUMPTION_DISTRIBUTION, label, properties);
  }

  // Composite measurement inputs
  get compositeEntityId(): string {
    return this.getProperty<string>('compositeEntityId') ?? '';
  }
  
  set compositeEntityId(value: string) {
    this.setProperty('compositeEntityId', value);
  }

  get measuredWeight(): number {
    return this.getProperty<number>('measuredWeight') ?? 0;
  }
  
  set measuredWeight(value: number) {
    this.setProperty('measuredWeight', value);
  }

  get previousCompositeWeight(): number | undefined {
    return this.getProperty<number>('previousCompositeWeight');
  }
  
  set previousCompositeWeight(value: number | undefined) {
    if (value !== undefined) this.setProperty('previousCompositeWeight', value);
  }

  get totalConsumption(): number {
    return this.getProperty<number>('totalConsumption') ?? 0;
  }
  
  set totalConsumption(value: number) {
    this.setProperty('totalConsumption', value);
  }

  // Distribution method and results
  get distributionMethod(): DistributionMethod {
    const method = this.getProperty<string>('distributionMethod');
    return (method ? DistributionMethod[method as keyof typeof DistributionMethod] : undefined) 
           ?? DistributionMethod.PROPORTIONAL;
  }
  
  set distributionMethod(value: DistributionMethod) {
    this.setProperty('distributionMethod', value);
  }

  get distributionConfidence(): number {
    return this.getProperty<number>('distributionConfidence') ?? 0.95;
  }
  
  set distributionConfidence(value: number) {
    this.setProperty('distributionConfidence', value);
  }

  // Individual entity distributions
  get distributions(): Map<string, number> {
    const jsonString = this.getProperty<string>('distributions') ?? '{}';
    try {
      const obj = JSON.parse(jsonString);
      return new Map(Object.entries(obj).map(([k, v]) => [k, v as number]));
    } catch (e) {
      return new Map();
    }
  }
  
  set distributions(value: Map<string, number>) {
    const obj = Object.fromEntries(value);
    this.setProperty('distributions', JSON.stringify(obj));
  }

  // Component information at time of measurement
  get fixedComponents(): Map<string, number> {
    const jsonString = this.getProperty<string>('fixedComponents') ?? '{}';
    try {
      const obj = JSON.parse(jsonString);
      return new Map(Object.entries(obj).map(([k, v]) => [k, v as number]));
    } catch (e) {
      return new Map();
    }
  }
  
  set fixedComponents(value: Map<string, number>) {
    const obj = Object.fromEntries(value);
    this.setProperty('fixedComponents', JSON.stringify(obj));
  }

  get consumableComponents(): Map<string, number> {
    const jsonString = this.getProperty<string>('consumableComponents') ?? '{}';
    try {
      const obj = JSON.parse(jsonString);
      return new Map(Object.entries(obj).map(([k, v]) => [k, v as number]));
    } catch (e) {
      return new Map();
    }
  }
  
  set consumableComponents(value: Map<string, number>) {
    const obj = Object.fromEntries(value);
    this.setProperty('consumableComponents', JSON.stringify(obj));
  }

  get notes(): string | undefined {
    return this.getProperty<string>('notes');
  }
  
  set notes(value: string | undefined) {
    if (value !== undefined) this.setProperty('notes', value);
  }
}