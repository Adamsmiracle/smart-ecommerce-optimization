package com.miracle.smart_ecommerce_jpa.domain.order.entity;

import com.miracle.smart_ecommerce_jpa.domain.BaseModel;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Shipping Method JPA entity - represents shipping_method table.
 */
@Entity
@Table(name = "shipping_method")
@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ShippingMethod extends BaseModel {

    @NotBlank(message = "Shipping method name is required")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500, message = "Description cannot exceed 500 characters")
    @Column(name = "description", length = 500)
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.00", message = "Price must be non-negative")
    @Column(name = "price", nullable = false, precision = 19, scale = 2)
    private BigDecimal price;

    @Min(value = 0, message = "Estimated days cannot be negative")
    @Column(name = "estimated_days")
    private Integer estimatedDays;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Get formatted estimated delivery
     */
    public String getEstimatedDelivery() {
        if (estimatedDays == null) return "Contact for estimate";
        if (estimatedDays == 0) return "Same day delivery";
        if (estimatedDays == 1) return "Next day delivery";
        return estimatedDays + " business days";
    }
}