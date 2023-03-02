package com.bctech.cashshareapplication.service;

import com.bctech.cashshareapplication.model.dtos.request.ActivateAccountRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.FetchAccountResponseDto;
import com.bctech.cashshareapplication.persistence.entity.Wallet;

public interface WalletService {
    FetchAccountResponseDto fetchAccount(long accountNumber);
    boolean activateAccount(ActivateAccountRequestDto activateAccountRequestDto);
    Wallet getLoggedInUserAccountDetails();

}
