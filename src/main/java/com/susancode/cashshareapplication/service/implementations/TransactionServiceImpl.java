package com.bctech.cashshareapplication.service.implementations;

import com.bctech.cashshareapplication.persistence.entity.Wallet;
import com.bctech.cashshareapplication.service.TransactionService;
import com.bctech.cashshareapplication.model.enums.ResponseStatus;
import com.bctech.cashshareapplication.model.dtos.request.DepositAccountRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.TransferFundRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.WithdrawFundRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.DepositResponseDto;
import com.bctech.cashshareapplication.model.dtos.response.FetchAccountResponseDto;
import com.bctech.cashshareapplication.model.dtos.response.TransferResponseDto;
import com.bctech.cashshareapplication.model.dtos.response.WithdrawFundResponseDto;
import com.bctech.cashshareapplication.persistence.entity.User;
import com.bctech.cashshareapplication.core.exceptions.CustomException;
import com.bctech.cashshareapplication.core.exceptions.InsufficientBalanceException;
import com.bctech.cashshareapplication.core.exceptions.ResourceNotFoundException;
import com.bctech.cashshareapplication.persistence.repository.WalletRepository;
import com.bctech.cashshareapplication.service.UserService;
import com.bctech.cashshareapplication.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;


@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletRepository walletRepository;
    private final WalletService accountService;
    private final UserService userService;

    @Override
    @Transactional
    public DepositResponseDto depositFunds(DepositAccountRequestDto depositAccountRequestDto) {
        Wallet account = getAccount(depositAccountRequestDto.getReceiverAccountNumber());
        BigDecimal newBalance = account.getAccountBalance().add(depositAccountRequestDto.getAmount());
        log.info("Funding account of :: [{}] :: with :: [{}] ::", account.getAccountNumber(), newBalance);
        account.setAccountBalance(newBalance);
        walletRepository.save(account);
        return buildDepositResponse(depositAccountRequestDto);
    }

    private DepositResponseDto buildDepositResponse(DepositAccountRequestDto depositAccountRequestDto) {
        return DepositResponseDto
                .builder()
                .depositAccountNumber(depositAccountRequestDto.getReceiverAccountNumber())
                .isTransactionSuccessful(true)
                .receiverName(depositAccountRequestDto.getReceiver())
                .statusCode(ResponseStatus.SUCCESSFUL.getCode())
                .build();
    }

    @Override
    @Transactional
    public TransferResponseDto transferFunds(TransferFundRequestDto transferFundRequestDto) {
        log.info("Transaction service transferFunds");
        Wallet transferAccount = getAccountLogIn();
        Wallet receiverAccount = getAccount(transferFundRequestDto.getReceiverAccountNumber());

        if (isAccountEligibleForTransfer(transferAccount.getAccountNumber(), transferFundRequestDto.getAmount())) {
            checkKYCLevel(transferFundRequestDto.getAmount().doubleValue(), "transfer");
            BigDecimal newTransferredBalance = transferAccount.getAccountBalance().subtract(transferFundRequestDto.getAmount());
            BigDecimal newReceiverBalance = receiverAccount.getAccountBalance().add(transferFundRequestDto.getAmount());

            transferAccount.setAccountBalance(newTransferredBalance);
            walletRepository.save(transferAccount);

            receiverAccount.setAccountBalance(newReceiverBalance);
            walletRepository.save(receiverAccount);

            log.info("finished transfer, sending response");
            return buildTransferResponse(transferAccount,receiverAccount);
        }
        throw new InsufficientBalanceException("Insufficient balance to complete this transaction", HttpStatus.BAD_REQUEST);
    }

    private TransferResponseDto buildTransferResponse(Wallet sender,Wallet reciver) {
        return TransferResponseDto
                .builder()
                .sendAccountNumber(sender.getAccountNumber())
                .isTransactionSuccessful(true)
                .receiverName(reciver.getUser().getFirstName())
                .senderName(sender.getUser().getFirstName())
                .statusCode(ResponseStatus.SUCCESSFUL.getCode())
                .build();
    }

    @Override
    @Transactional
    public WithdrawFundResponseDto withdrawFunds(WithdrawFundRequestDto withdrawFundRequestDto) {
        log.info("Account Transaction service ");
        Wallet withdrawAccount = getAccountLogIn();

        if(isAccountEligibleForTransfer(withdrawAccount.getAccountNumber(), withdrawFundRequestDto.getAmount())) {
            checkKYCLevel(withdrawFundRequestDto.getAmount().doubleValue(), "deposit");
            BigDecimal newWithdrawAmount = withdrawAccount.getAccountBalance().subtract(withdrawFundRequestDto.getAmount());
            withdrawAccount.setAccountBalance(newWithdrawAmount);
            walletRepository.save(withdrawAccount);
            log.info("finished withdraw for :: [{}]", withdrawAccount.getAccountNumber());
            return buildWithDrawResponse(withdrawAccount);
        }
        throw new InsufficientBalanceException("Insufficient balance to complete this transaction", HttpStatus.BAD_REQUEST);
    }

    private WithdrawFundResponseDto buildWithDrawResponse(Wallet withdrawAccount) {
        return WithdrawFundResponseDto
                .builder()
                .accountNum(withdrawAccount.getAccountNumber())
                .isSuccessful(true)
                .status(ResponseStatus.SUCCESSFUL.getCode())
                .name(withdrawAccount.getUser().getFirstName())
                .build();
    }

    private boolean isAccountEligibleForTransfer(long account, BigDecimal amount) {
        FetchAccountResponseDto accountDetails = accountService.fetchAccount(account);
        return accountDetails.getAccountBalance().compareTo(amount) >= 0;
    }

    private Wallet getAccount(long accountNumber) {
        return walletRepository.getAccountByAccountNumber(accountNumber).orElseThrow(
                () -> {throw new ResourceNotFoundException("account not found");
                }
        );
    }

    private void checkKYCLevel(double amount, String operation){
        User loggedInUser = userService.getLoggedInUser();
        double maxAllowableAmount = loggedInUser.getKycLevel().getTrnMaxLimit();
        if (amount > maxAllowableAmount) {
            throw new CustomException(
                    "You cannot "+operation+" "+amount+" try a figure below"+
                            maxAllowableAmount+" or apply to increase your transaction Limit");
        }

    }

    private Wallet getAccountLogIn(){
        return walletRepository.
                getAccountByAccountNumber(accountService.getLoggedInUserAccountDetails().getAccountNumber()).get();
    }
}
