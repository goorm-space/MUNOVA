package com.space.munova.core.utils;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Arrays;

public class EnumValidator implements ConstraintValidator<ValidEnum, String> {

    private Enum<?>[] enumConstants;
    private boolean ignoreCase;

    @Override
    public void initialize(ValidEnum constraintAnnotation) {
        this.enumConstants = constraintAnnotation.enumClass().getEnumConstants();
        this.ignoreCase = constraintAnnotation.ignoreCase();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return Arrays.stream(this.enumConstants)
                .anyMatch(e -> {
                    String name = e.name();
                    return ignoreCase ? name.equalsIgnoreCase(value) : name.equals(value);
                });
    }
}
