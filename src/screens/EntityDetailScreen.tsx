import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  StyleSheet,
  ScrollView,
  Alert,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  Surface,
  Text,
  IconButton,
  Chip,
  Divider,
  ActivityIndicator,
} from 'react-native-paper';
import { NavigationProps } from '../types/Navigation';
import { 
  GraphEntity, 
  EntityType, 
  PhysicalComponent, 
  InventoryItem, 
  Identifier, 
  Activity,
  TagFormat
} from '../types/FilamentInfo';

interface EntityDetailScreenProps extends NavigationProps {
  route: {
    params: {
      entityId: string;
      entityType: string;
    };
  };
}

interface EntityRelationship {
  id: string;
  type: 'connected_to' | 'contains' | 'identified_by' | 'activity_on';
  targetEntity: GraphEntity;
  description: string;
}

const EntityDetailScreen: React.FC<EntityDetailScreenProps> = ({ navigation, route }) => {
  const { entityId, entityType } = route.params;
  const [entity, setEntity] = useState<GraphEntity | null>(null);
  const [relationships, setRelationships] = useState<EntityRelationship[]>([]);
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [_isEditing, _setIsEditing] = useState<boolean>(false);

  const loadEntityDetails = useCallback(async () => {
    setIsLoading(true);
    // Simulate loading entity data
    setTimeout(() => {
      const mockEntity = createMockEntity(entityId, entityType as EntityType);
      const mockRelationships = createMockRelationships(entityId);
      
      setEntity(mockEntity);
      setRelationships(mockRelationships);
      setIsLoading(false);
    }, 500);
  }, [entityId, entityType]);

  useEffect(() => {
    loadEntityDetails();
  }, [entityId, loadEntityDetails]);

  const createMockEntity = (id: string, type: EntityType): GraphEntity => {
    const baseEntity = {
      id,
      type,
      createdAt: Date.now() - 1000 * 60 * 60 * 24 * 2, // 2 days ago
      updatedAt: Date.now() - 1000 * 60 * 60, // 1 hour ago
    };

    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return {
          ...baseEntity,
          filamentInfo: {
            tagUid: 'AB:CD:EF:12',
            trayUid: 'TR001',
            tagFormat: TagFormat.BAMBU_LAB,
            manufacturerName: 'Bambu Lab',
            filamentType: 'PLA Basic',
            colorHex: '#FF6600',
            colorName: 'Orange',
            spoolWeight: 250,
            filamentDiameter: 1.75,
            filamentLength: 330000,
            productionDate: '2024-01',
            minTemperature: 190,
            maxTemperature: 220,
            bedTemperature: 60,
            dryingTemperature: 45,
            dryingTime: 4,
          },
          currentWeight: 850,
          notes: 'High quality filament, prints well with default settings',
        } as PhysicalComponent;

      case EntityType.INVENTORY_ITEM:
        return {
          ...baseEntity,
          quantity: 5,
          location: 'Storage Rack A, Shelf 2',
          lastUpdated: Date.now() - 1000 * 60 * 30, // 30 minutes ago
        } as InventoryItem;

      case EntityType.IDENTIFIER:
        return {
          ...baseEntity,
          value: 'AB:CD:EF:12:34:56',
          identifierType: 'RFID',
        } as Identifier;

      case EntityType.ACTIVITY:
        return {
          ...baseEntity,
          activityType: 'SCAN',
          description: 'RFID tag scanned successfully',
          relatedEntityId: 'component-001',
        } as Activity;

      default:
        return baseEntity;
    }
  };

  const createMockRelationships = (_entityId: string): EntityRelationship[] => {
    return [
      {
        id: 'rel-1',
        type: 'identified_by',
        targetEntity: {
          id: 'id-001',
          type: EntityType.IDENTIFIER,
          createdAt: Date.now() - 1000 * 60 * 60 * 24,
          updatedAt: Date.now() - 1000 * 60 * 60,
        },
        description: 'Primary RFID identifier',
      },
      {
        id: 'rel-2',
        type: 'activity_on',
        targetEntity: {
          id: 'act-001',
          type: EntityType.ACTIVITY,
          createdAt: Date.now() - 1000 * 60 * 60 * 2,
          updatedAt: Date.now() - 1000 * 60 * 60 * 2,
        },
        description: 'Last scan activity',
      },
    ];
  };

  const getEntityTypeLabel = (type: EntityType): string => {
    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return 'Physical Component';
      case EntityType.INVENTORY_ITEM:
        return 'Inventory Item';
      case EntityType.IDENTIFIER:
        return 'Identifier';
      case EntityType.ACTIVITY:
        return 'Activity';
      default:
        return 'Unknown';
    }
  };

  const getEntityIcon = (type: EntityType): string => {
    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return 'package-variant';
      case EntityType.INVENTORY_ITEM:
        return 'archive';
      case EntityType.IDENTIFIER:
        return 'identifier';
      case EntityType.ACTIVITY:
        return 'history';
      default:
        return 'help-circle';
    }
  };

  const getEntityColor = (type: EntityType): string => {
    switch (type) {
      case EntityType.PHYSICAL_COMPONENT:
        return '#4CAF50';
      case EntityType.INVENTORY_ITEM:
        return '#2196F3';
      case EntityType.IDENTIFIER:
        return '#FF9800';
      case EntityType.ACTIVITY:
        return '#9C27B0';
      default:
        return '#757575';
    }
  };

  const handleEdit = () => {
    _setIsEditing(true);
    // TODO: Navigate to edit screen or show edit modal
    Alert.alert('Edit Entity', 'Edit functionality would be implemented here');
  };

  const handleDelete = () => {
    Alert.alert(
      'Delete Entity',
      'Are you sure you want to delete this entity? This action cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        { 
          text: 'Delete', 
          style: 'destructive',
          onPress: () => {
            // TODO: Implement delete functionality
            navigation.goBack();
          }
        },
      ]
    );
  };

  const handleRelationshipPress = (relationship: EntityRelationship) => {
    navigation.push('EntityDetail', {
      entityId: relationship.targetEntity.id,
      entityType: relationship.targetEntity.type,
    });
  };

  const renderPhysicalComponentDetails = (component: PhysicalComponent) => (
    <>
      <Card style={styles.card}>
        <Card.Content>
          <Title>Filament Information</Title>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Manufacturer:</Text>
            <Text style={styles.value}>{component.filamentInfo.manufacturerName}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Type:</Text>
            <Text style={styles.value}>{component.filamentInfo.filamentType}</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Color:</Text>
            <View style={styles.colorRow}>
              <View 
                style={[
                  styles.colorSwatch, 
                  { backgroundColor: component.filamentInfo.colorHex }
                ]} 
              />
              <Text style={styles.value}>{component.filamentInfo.colorName}</Text>
            </View>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Diameter:</Text>
            <Text style={styles.value}>{component.filamentInfo.filamentDiameter}mm</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Current Weight:</Text>
            <Text style={styles.value}>{component.currentWeight}g</Text>
          </View>
        </Card.Content>
      </Card>

      <Card style={styles.card}>
        <Card.Content>
          <Title>Printing Settings</Title>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Nozzle Temp:</Text>
            <Text style={styles.value}>
              {component.filamentInfo.minTemperature}°C - {component.filamentInfo.maxTemperature}°C
            </Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Bed Temp:</Text>
            <Text style={styles.value}>{component.filamentInfo.bedTemperature}°C</Text>
          </View>
          <View style={styles.detailRow}>
            <Text style={styles.label}>Drying:</Text>
            <Text style={styles.value}>
              {component.filamentInfo.dryingTemperature}°C for {component.filamentInfo.dryingTime}h
            </Text>
          </View>
        </Card.Content>
      </Card>

      {component.notes && (
        <Card style={styles.card}>
          <Card.Content>
            <Title>Notes</Title>
            <Paragraph>{component.notes}</Paragraph>
          </Card.Content>
        </Card>
      )}
    </>
  );

  const renderInventoryItemDetails = (item: InventoryItem) => (
    <Card style={styles.card}>
      <Card.Content>
        <Title>Inventory Details</Title>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Quantity:</Text>
          <Text style={styles.value}>{item.quantity}</Text>
        </View>
        {item.location && (
          <View style={styles.detailRow}>
            <Text style={styles.label}>Location:</Text>
            <Text style={styles.value}>{item.location}</Text>
          </View>
        )}
        <View style={styles.detailRow}>
          <Text style={styles.label}>Last Updated:</Text>
          <Text style={styles.value}>
            {new Date(item.lastUpdated).toLocaleString()}
          </Text>
        </View>
      </Card.Content>
    </Card>
  );

  const renderIdentifierDetails = (identifier: Identifier) => (
    <Card style={styles.card}>
      <Card.Content>
        <Title>Identifier Details</Title>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Type:</Text>
          <Text style={styles.value}>{identifier.identifierType}</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Value:</Text>
          <Text style={[styles.value, styles.monospace]}>{identifier.value}</Text>
        </View>
      </Card.Content>
    </Card>
  );

  const renderActivityDetails = (activity: Activity) => (
    <Card style={styles.card}>
      <Card.Content>
        <Title>Activity Details</Title>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Type:</Text>
          <Text style={styles.value}>{activity.activityType}</Text>
        </View>
        <View style={styles.detailRow}>
          <Text style={styles.label}>Description:</Text>
          <Text style={styles.value}>{activity.description}</Text>
        </View>
        {activity.relatedEntityId && (
          <View style={styles.detailRow}>
            <Text style={styles.label}>Related Entity:</Text>
            <Text style={styles.value}>{activity.relatedEntityId}</Text>
          </View>
        )}
      </Card.Content>
    </Card>
  );

  const renderRelationships = () => {
    if (relationships.length === 0) {
      return null;
    }

    return (
      <Card style={styles.card}>
        <Card.Content>
          <Title>Relationships</Title>
          {relationships.map((rel, index) => (
            <View key={rel.id}>
              <Surface 
                style={styles.relationshipItem} 
                elevation={1}
                onTouchEnd={() => handleRelationshipPress(rel)}
              >
                <View style={styles.relationshipHeader}>
                  <IconButton
                    icon={getEntityIcon(rel.targetEntity.type)}
                    iconColor={getEntityColor(rel.targetEntity.type)}
                    size={20}
                  />
                  <View style={styles.relationshipContent}>
                    <Text style={styles.relationshipType}>{rel.type.replace('_', ' ')}</Text>
                    <Text style={styles.relationshipDescription}>{rel.description}</Text>
                  </View>
                  <IconButton icon="chevron-right" size={20} />
                </View>
              </Surface>
              {index < relationships.length - 1 && <Divider style={styles.relationshipDivider} />}
            </View>
          ))}
        </Card.Content>
      </Card>
    );
  };

  if (isLoading) {
    return (
      <View style={styles.loadingContainer}>
        <ActivityIndicator size="large" color="#6200EE" />
        <Text style={styles.loadingText}>Loading entity details...</Text>
      </View>
    );
  }

  if (!entity) {
    return (
      <View style={styles.errorContainer}>
        <Text style={styles.errorText}>Entity not found</Text>
        <Button mode="contained" onPress={() => navigation.goBack()}>
          Go Back
        </Button>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <ScrollView style={styles.scrollView}>
        {/* Header Card */}
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.header}>
              <View style={styles.headerLeft}>
                <Chip 
                  icon={getEntityIcon(entity.type)}
                  style={[styles.typeChip, { backgroundColor: getEntityColor(entity.type) + '20' }]}
                >
                  {getEntityTypeLabel(entity.type)}
                </Chip>
                <Title style={styles.entityId}>ID: {entity.id}</Title>
                <Paragraph>
                  Created: {new Date(entity.createdAt).toLocaleDateString()}
                </Paragraph>
                <Paragraph>
                  Updated: {new Date(entity.updatedAt).toLocaleDateString()}
                </Paragraph>
              </View>
              <View style={styles.headerActions}>
                <IconButton
                  icon="pencil"
                  size={24}
                  onPress={handleEdit}
                />
                <IconButton
                  icon="delete"
                  size={24}
                  iconColor="#F44336"
                  onPress={handleDelete}
                />
              </View>
            </View>
          </Card.Content>
        </Card>

        {/* Entity-specific Details */}
        {entity.type === EntityType.PHYSICAL_COMPONENT && 
          renderPhysicalComponentDetails(entity as PhysicalComponent)}
        {entity.type === EntityType.INVENTORY_ITEM && 
          renderInventoryItemDetails(entity as InventoryItem)}
        {entity.type === EntityType.IDENTIFIER && 
          renderIdentifierDetails(entity as Identifier)}
        {entity.type === EntityType.ACTIVITY && 
          renderActivityDetails(entity as Activity)}

        {/* Relationships */}
        {renderRelationships()}
      </ScrollView>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  scrollView: {
    flex: 1,
    padding: 16,
  },
  loadingContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
    color: '#666',
  },
  errorContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 32,
  },
  errorText: {
    fontSize: 18,
    color: '#F44336',
    marginBottom: 16,
    textAlign: 'center',
  },
  card: {
    marginBottom: 16,
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
  },
  headerLeft: {
    flex: 1,
  },
  headerActions: {
    flexDirection: 'row',
  },
  typeChip: {
    alignSelf: 'flex-start',
    marginBottom: 8,
  },
  entityId: {
    fontSize: 18,
    marginBottom: 4,
  },
  detailRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingVertical: 4,
  },
  label: {
    fontSize: 14,
    fontWeight: '600',
    color: '#333',
    flex: 1,
  },
  value: {
    fontSize: 14,
    color: '#666',
    flex: 2,
    textAlign: 'right',
  },
  monospace: {
    fontFamily: 'monospace',
  },
  colorRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'flex-end',
    flex: 2,
  },
  colorSwatch: {
    width: 16,
    height: 16,
    borderRadius: 8,
    marginRight: 8,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  relationshipItem: {
    marginVertical: 4,
    borderRadius: 8,
    padding: 8,
  },
  relationshipHeader: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  relationshipContent: {
    flex: 1,
    marginLeft: 8,
  },
  relationshipType: {
    fontSize: 14,
    fontWeight: '600',
    textTransform: 'capitalize',
  },
  relationshipDescription: {
    fontSize: 12,
    color: '#666',
  },
  relationshipDivider: {
    marginVertical: 4,
  },
});

export default EntityDetailScreen;