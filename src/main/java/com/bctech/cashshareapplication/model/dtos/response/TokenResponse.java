package com.bctech.cashshareapplication.model.dtos.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class TokenResponse {
    private String token;
}
