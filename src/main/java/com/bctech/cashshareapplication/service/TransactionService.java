package com.bctech.cashshareapplication.service;


import com.bctech.cashshareapplication.model.dtos.request.DepositAccountRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.TransferFundRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.WithdrawFundRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.DepositResponseDto;
import com.bctech.cashshareapplication.model.dtos.response.TransferResponseDto;
import com.bctech.cashshareapplication.model.dtos.response.WithdrawFundResponseDto;

public interface TransactionService {
    DepositResponseDto depositFunds(DepositAccountRequestDto depositAccountRequestDto);
    TransferResponseDto transferFunds(TransferFundRequestDto transferFundRequestDto);
    WithdrawFundResponseDto withdrawFunds(WithdrawFundRequestDto withdrawFundRequestDto);
}
