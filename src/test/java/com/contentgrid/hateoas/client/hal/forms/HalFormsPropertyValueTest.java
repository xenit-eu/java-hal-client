package com.contentgrid.hateoas.client.hal.forms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HalFormsPropertyValueTest {

    @Test
    void testEquals() {
        assertThat(HalFormsPropertyValue.of("foo")).isEqualTo(HalFormsPropertyValue.of("foo"));
        assertThat(HalFormsPropertyValue.empty()).isEqualTo(HalFormsPropertyValue.empty());
        assertThat(HalFormsPropertyValue.missing()).isEqualTo(HalFormsPropertyValue.missing());
        assertThat(HalFormsPropertyValue.ofNullable("foo")).isEqualTo(HalFormsPropertyValue.of("foo"));
        assertThat(HalFormsPropertyValue.ofNullable(null)).isEqualTo(HalFormsPropertyValue.empty());
    }

    @Test
    void testNotEquals() {
        assertThat(HalFormsPropertyValue.of("foo")).isNotEqualTo(HalFormsPropertyValue.of("bar"));
        assertThat(HalFormsPropertyValue.of("foo")).isNotEqualTo(HalFormsPropertyValue.missing());
        assertThat(HalFormsPropertyValue.of("foo")).isNotEqualTo(HalFormsPropertyValue.empty());
        assertThat(HalFormsPropertyValue.empty()).isNotEqualTo(HalFormsPropertyValue.missing());
        assertThat(HalFormsPropertyValue.ofNullable("foo")).isNotEqualTo(HalFormsPropertyValue.empty());
        assertThat(HalFormsPropertyValue.ofNullable(null)).isNotEqualTo(HalFormsPropertyValue.of("foo"));
    }

}