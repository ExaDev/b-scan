package com.bscan.model.graph

import com.bscan.model.graph.entities.TrackingMode
import java.time.LocalDateTime
import java.time.LocalDate

/**
 * Type-safe property value container supporting multiple data types.
 * Handles serialization and type conversion for graph entity/edge properties.
 */
sealed class PropertyValue {
    
    abstract val type: PropertyType
    abstract val rawValue: Any?
    
    data class StringValue(override val rawValue: String) : PropertyValue() {
        override val type = PropertyType.STRING
    }
    
    data class IntValue(override val rawValue: Int) : PropertyValue() {
        override val type = PropertyType.INT
    }
    
    data class LongValue(override val rawValue: Long) : PropertyValue() {
        override val type = PropertyType.LONG
    }
    
    data class FloatValue(override val rawValue: Float) : PropertyValue() {
        override val type = PropertyType.FLOAT
    }
    
    data class DoubleValue(override val rawValue: Double) : PropertyValue() {
        override val type = PropertyType.DOUBLE
    }
    
    data class BooleanValue(override val rawValue: Boolean) : PropertyValue() {
        override val type = PropertyType.BOOLEAN
    }
    
    data class DateTimeValue(override val rawValue: LocalDateTime) : PropertyValue() {
        override val type = PropertyType.DATETIME
    }
    
    data class DateValue(override val rawValue: LocalDate) : PropertyValue() {
        override val type = PropertyType.DATE
    }
    
    data class ListValue(override val rawValue: List<*>) : PropertyValue() {
        override val type = PropertyType.LIST
    }
    
    data class MapValue(override val rawValue: Map<String, *>) : PropertyValue() {
        override val type = PropertyType.MAP
    }
    
    data class BytesValue(override val rawValue: ByteArray) : PropertyValue() {
        override val type = PropertyType.BYTES
        
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as BytesValue
            return rawValue.contentEquals(other.rawValue)
        }
        
        override fun hashCode(): Int = rawValue.contentHashCode()
    }
    
    data class QuantityValue(override val rawValue: Quantity) : PropertyValue() {
        override val type = PropertyType.QUANTITY
    }
    
    object NullValue : PropertyValue() {
        override val type = PropertyType.NULL
        override val rawValue: Nothing? = null
    }
    
    /**
     * Get the value with type safety
     */
    inline fun <reified T> getValue(): T? {
        return when (T::class) {
            String::class -> if (type == PropertyType.STRING) rawValue as T else null
            Int::class -> if (type == PropertyType.INT) rawValue as T else null
            Long::class -> if (type == PropertyType.LONG) rawValue as T else null
            Float::class -> if (type == PropertyType.FLOAT) rawValue as T else null
            Double::class -> if (type == PropertyType.DOUBLE) rawValue as T else null
            Boolean::class -> if (type == PropertyType.BOOLEAN) rawValue as T else null
            LocalDateTime::class -> if (type == PropertyType.DATETIME) rawValue as T else null
            LocalDate::class -> if (type == PropertyType.DATE) rawValue as T else null
            List::class -> if (type == PropertyType.LIST) rawValue as T else null
            Map::class -> if (type == PropertyType.MAP) rawValue as T else null
            ByteArray::class -> if (type == PropertyType.BYTES) rawValue as T else null
            Quantity::class -> if (type == PropertyType.QUANTITY) rawValue as T else null
            else -> null
        }
    }
    
    /**
     * Convert to string representation
     */
    fun asString(): String {
        return when (this) {
            is StringValue -> rawValue
            is IntValue -> rawValue.toString()
            is LongValue -> rawValue.toString()
            is FloatValue -> rawValue.toString()
            is DoubleValue -> rawValue.toString()
            is BooleanValue -> rawValue.toString()
            is DateTimeValue -> rawValue.toString()
            is DateValue -> rawValue.toString()
            is ListValue -> rawValue.toString()
            is MapValue -> rawValue.toString()
            is BytesValue -> rawValue.joinToString("") { "%02x".format(it) }
            is QuantityValue -> rawValue.toString()
            is NullValue -> "null"
        }
    }
    
    companion object {
        /**
         * Create PropertyValue from any supported type
         */
        fun create(value: Any?): PropertyValue {
            return when (value) {
                null -> NullValue
                is String -> StringValue(value)
                is Int -> IntValue(value)
                is Long -> LongValue(value)
                is Float -> FloatValue(value)
                is Double -> DoubleValue(value)
                is Boolean -> BooleanValue(value)
                is LocalDateTime -> DateTimeValue(value)
                is LocalDate -> DateValue(value)
                is List<*> -> ListValue(value)
                is Map<*, *> -> MapValue(value as Map<String, *>)
                is ByteArray -> BytesValue(value)
                is Quantity -> QuantityValue(value)
                else -> StringValue(value.toString())
            }
        }
        
        /**
         * Create PropertyValue from string with type hint
         */
        fun fromString(value: String, type: PropertyType): PropertyValue {
            return when (type) {
                PropertyType.STRING -> StringValue(value)
                PropertyType.INT -> IntValue(value.toIntOrNull() ?: 0)
                PropertyType.LONG -> LongValue(value.toLongOrNull() ?: 0L)
                PropertyType.FLOAT -> FloatValue(value.toFloatOrNull() ?: 0f)
                PropertyType.DOUBLE -> DoubleValue(value.toDoubleOrNull() ?: 0.0)
                PropertyType.BOOLEAN -> BooleanValue(value.toBooleanStrictOrNull() ?: false)
                PropertyType.DATETIME -> DateTimeValue(LocalDateTime.parse(value))
                PropertyType.DATE -> DateValue(LocalDate.parse(value))
                PropertyType.BYTES -> BytesValue(value.chunked(2).map { it.toInt(16).toByte() }.toByteArray())
                PropertyType.QUANTITY -> QuantityValue(Quantity.fromString(value))
                PropertyType.LIST -> StringValue(value)  // JSON parsing would go here
                PropertyType.MAP -> StringValue(value)   // JSON parsing would go here
                PropertyType.NULL -> NullValue
            }
        }
    }
}

/**
 * Supported property types
 */
enum class PropertyType {
    STRING,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    DATETIME,
    DATE,
    LIST,
    MAP,
    BYTES,
    QUANTITY,
    NULL
}

/**
 * Property schema definition for validation and UI generation
 */
data class PropertySchema(
    val key: String,
    val type: PropertyType,
    val required: Boolean = false,
    val defaultValue: PropertyValue? = null,
    val validation: PropertyValidation? = null,
    val displayName: String = key,
    val description: String = "",
    val category: String = "general",
    val hidden: Boolean = false,
    val readOnly: Boolean = false
)

/**
 * Property validation rules
 */
data class PropertyValidation(
    val minValue: Number? = null,
    val maxValue: Number? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: Regex? = null,
    val allowedValues: Set<String>? = null,
    val customValidator: ((PropertyValue) -> ValidationResult)? = null
)

/**
 * Interface for quantities with units and discrete/continuous tracking modes
 */
interface Quantity {
    val value: Number
    val unit: String
    val trackingMode: TrackingMode
    
    override fun toString(): String
    
    companion object {
        /**
         * Parse a quantity from string representation (e.g., "1000g", "212.5g-discrete")
         */
        fun fromString(str: String): Quantity {
            val parts = str.split("-")
            val quantityPart = parts[0]
            val trackingMode = if (parts.size > 1) {
                when (parts[1].lowercase()) {
                    "discrete" -> TrackingMode.DISCRETE
                    "continuous" -> TrackingMode.CONTINUOUS
                    else -> TrackingMode.CONTINUOUS
                }
            } else TrackingMode.CONTINUOUS
            
            // Extract numeric value and unit
            val numericPart = quantityPart.takeWhile { it.isDigit() || it == '.' }
            val unitPart = quantityPart.drop(numericPart.length)
            
            val value = numericPart.toDoubleOrNull() ?: 0.0
            
            return if (trackingMode == TrackingMode.DISCRETE) {
                DiscreteQuantity(value.toInt(), unitPart.ifEmpty { "units" })
            } else {
                ContinuousQuantity(value, unitPart.ifEmpty { "units" })
            }
        }
    }
}

/**
 * Discrete quantity for countable items
 */
data class DiscreteQuantity(
    override val value: Int,
    override val unit: String
) : Quantity {
    override val trackingMode = TrackingMode.DISCRETE
    
    override fun toString(): String = "${value}${unit}-discrete"
    
    /**
     * Add discrete quantities
     */
    operator fun plus(other: DiscreteQuantity): DiscreteQuantity {
        require(unit == other.unit) { "Cannot add quantities with different units: $unit vs ${other.unit}" }
        return copy(value = value + other.value)
    }
    
    /**
     * Subtract discrete quantities
     */
    operator fun minus(other: DiscreteQuantity): DiscreteQuantity {
        require(unit == other.unit) { "Cannot subtract quantities with different units: $unit vs ${other.unit}" }
        return copy(value = value - other.value)
    }
}

/**
 * Continuous quantity for measurable values
 */
data class ContinuousQuantity(
    override val value: Double,
    override val unit: String
) : Quantity {
    override val trackingMode = TrackingMode.CONTINUOUS
    
    override fun toString(): String = "${value}${unit}"
    
    /**
     * Add continuous quantities
     */
    operator fun plus(other: ContinuousQuantity): ContinuousQuantity {
        require(unit == other.unit) { "Cannot add quantities with different units: $unit vs ${other.unit}" }
        return copy(value = value + other.value)
    }
    
    /**
     * Subtract continuous quantities
     */
    operator fun minus(other: ContinuousQuantity): ContinuousQuantity {
        require(unit == other.unit) { "Cannot subtract quantities with different units: $unit vs ${other.unit}" }
        return copy(value = value - other.value)
    }
    
    /**
     * Multiply by scalar
     */
    operator fun times(factor: Double): ContinuousQuantity {
        return copy(value = value * factor)
    }
    
    /**
     * Divide by scalar
     */
    operator fun div(divisor: Double): ContinuousQuantity {
        require(divisor != 0.0) { "Cannot divide by zero" }
        return copy(value = value / divisor)
    }
}