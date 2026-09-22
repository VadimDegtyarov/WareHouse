package com.kis.wmsapplication.modules.warehouseModule.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class HierarchyLevelAttributeId implements Serializable {

    @Column(name = "attributes_id")
    private Long attributesId;

    @Column(name = "level_id")
    private Long levelId;
}
