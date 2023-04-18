package com.bctech.cashshareapplication.model.dtos.response;

import com.bctech.cashshareapplication.model.enums.WalletType;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class FetchAccountResponseDto {
    private long accountNumber;
    private WalletType accountType;
    private BigDecimal accountBalance;
    private boolean isActivated;
}
