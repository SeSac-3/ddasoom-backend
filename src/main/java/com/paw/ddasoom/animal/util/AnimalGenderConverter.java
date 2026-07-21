package com.paw.ddasoom.animal.util;

import org.springframework.core.convert.converter.Converter;

import com.paw.ddasoom.animal.domain.AnimalGender;

public class AnimalGenderConverter implements Converter<String, AnimalGender> {

    @Override
    public AnimalGender convert(String value) {
        if (value == null || value.isBlank()) return null;
        return AnimalGender.fromCode(value);
    }
}