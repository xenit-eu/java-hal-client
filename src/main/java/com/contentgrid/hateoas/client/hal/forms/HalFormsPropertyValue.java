package com.contentgrid.hateoas.client.hal.forms;

import java.util.function.Function;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.lang.Nullable;

/**
 * Tri-state equivalent of {@link java.util.Optional}.
 * <p>
 * A value can be
 * <ul>
 *     <li>Present with the declared value: {@link NonNullHalFormsPropertyValue}</li>
 *     <li>Present, but null: {@link NullHalFormsPropertyValue}</li>
 *     <li>Absent: {@link MissingHalFormsPropertyValue}</li>
 * </ul>
 *
 * @param <T> The type of the value
 */
public sealed interface HalFormsPropertyValue<T> {

    /**
     * Retrieves the value, if any
     * <p>
     * Note that you are primarily expected to use a pattern-matching switch statement to distinguish between the different subtypes of {@linkplain HalFormsPropertyValue}
     *
     * @return Retrieves the value from {@link NonNullHalFormsPropertyValue}, or {@code null} for {@link NullHalFormsPropertyValue} and {@link MissingHalFormsPropertyValue}
     */
    @Nullable
    T get();

    /**
     * If a value is present, returns the {@linkplain HalFormsPropertyValue} after applying the mapping function
     * @param mapper The mapping function to apply to the value, if present
     * @return The {@linkplain HalFormsPropertyValue} describing the result of applying the mapping function to this {@linkplain HalFormsPropertyValue}
     * @param <U> The type of the value returned from the mapping function
     */
    <U> HalFormsPropertyValue<U> map(Function<T, U> mapper);

    /**
     * @return A {@linkplain HalFormsPropertyValue} representing an absent value
     * @param <T> The type of the absent value
     */
    static <T> HalFormsPropertyValue<T> missing() {
        return new MissingHalFormsPropertyValue<>();
    }

    /**
     * @return A {@linkplain HalFormsPropertyValue} representing a present but {@code null} value
     * @param <T> The type of the null value
     */
    static <T> HalFormsPropertyValue<T> empty() {
        return new NullHalFormsPropertyValue<>();
    }

    /**
     * @param value The value to describe, must be non-null
     * @return A {@linkplain HalFormsPropertyValue} representing a non-null present value
     * @param <T> The type of the value
     */
    static <T> HalFormsPropertyValue<T> of(T value) {
        return new NonNullHalFormsPropertyValue<>(value);
    }

    /**
     * @param value The value to describe, can be null
     * @return A {@linkplain HalFormsPropertyValue} representing a present value
     * @param <T> The type of the value
     */
    static <T> HalFormsPropertyValue<T> ofNullable(@Nullable T value) {
        return value == null ? empty() : of(value);
    }

    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    @EqualsAndHashCode
    final class NonNullHalFormsPropertyValue<T> implements HalFormsPropertyValue<T> {
        @NonNull
        private final T data;

        @Override
        public T get() {
            return data;
        }

        @Override
        public <U> HalFormsPropertyValue<U> map(Function<T, U> mapper) {
            return new NonNullHalFormsPropertyValue<>(mapper.apply(data));
        }
    }

    @Value
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class MissingHalFormsPropertyValue<T> implements HalFormsPropertyValue<T> {

        @Override
        @Nullable
        public T get() {
            return null;
        }

        @Override
        public <U> HalFormsPropertyValue<U> map(Function<T, U> mapper) {
            return new MissingHalFormsPropertyValue<>();
        }
    }

    @Value
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    class NullHalFormsPropertyValue<T> implements HalFormsPropertyValue<T> {

        @Override
        @Nullable
        public T get() {
            return null;
        }

        @Override
        public <U> HalFormsPropertyValue<U> map(Function<T, U> mapper) {
            return new NullHalFormsPropertyValue<>();
        }
    }
}
