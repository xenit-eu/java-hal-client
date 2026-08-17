package com.contentgrid.hateoas.client.hal.forms;

import com.contentgrid.hateoas.client.hal.HalRequest;
import java.util.Map;
import java.util.function.Function;

public interface HalFormsBodyRequest extends HalRequest {

    HalRequest properties(Function<HalFormsProperty, HalFormsPropertyValue<Object>> valueFunction);

    default HalRequest properties(Map<String, Object> values) {
        return this.properties(property -> {
            if (values.containsKey(property.getName())) {
                return HalFormsPropertyValue.ofNullable(values.get(property.getName()));
            } else {
                return HalFormsPropertyValue.missing();
            }
        });
    }
}
