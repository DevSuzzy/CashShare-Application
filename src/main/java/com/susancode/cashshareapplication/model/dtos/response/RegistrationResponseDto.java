package com.bctech.cashshareapplication.model.dtos.response;

import com.bctech.cashshareapplication.model.enums.WalletType;
import lombok.Data;

@Data
public class RegistrationResponseDto {
    private Long newAccountNumber;
    private String accountType = WalletType.SAVINGS.toString();

    public RegistrationResponseDto(Long newAccountNumber) {
        this.newAccountNumber = newAccountNumber;
    }
}
