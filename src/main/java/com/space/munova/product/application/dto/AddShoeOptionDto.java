package com.space.munova.product.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AddShoeOptionDto (List<ShoeOptionDto> shoeOptionDtos){
}
