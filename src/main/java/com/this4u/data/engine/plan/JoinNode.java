package com.this4u.data.engine.plan;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinNode {
    private String mainTable;
    private String mainField;
    private String joinTable;
    private String joinField;
}
