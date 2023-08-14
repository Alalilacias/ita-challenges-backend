package com.itachallenge.challenge.dto;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagingParametersDto {

    @Pattern(regexp = "^[1-9]+$", message = "PageNumber must be positive integer")
    private String pageNumber;

    @Pattern(regexp = "^[1-9]+$", message = "PageSize must be positive integer")
    private String pageSize;

}

