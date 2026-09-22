package com.kis.wmsapplication.modules.warehouseModule.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "warehouse_hierarchy_level_attributes")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class HierarchyLevelAttribute {

    @EmbeddedId
    private HierarchyLevelAttributeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("attributesId")
    @JoinColumn(name = "attributes_id", nullable = false)
    private Attribute attribute;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("levelId")
    @JoinColumn(name = "level_id", nullable = false)
    private HierarchyLevel level;

    @Column(name = "attribute_value", nullable = false)
    private String attributeValue;

    @PrePersist
    @PreUpdate
    public void syncId() {
        if (this.attribute != null && this.level != null) {
            if (this.id == null) {
                this.id = new HierarchyLevelAttributeId(this.attribute.getId(), this.level.getId());
            } else {
                this.id.setAttributesId(this.attribute.getId());
                this.id.setLevelId(this.level.getId());
            }
        }
    }
}
