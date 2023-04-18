package com.bctech.cashshareapplication.service.implementations;

import com.bctech.cashshareapplication.model.dtos.request.ActivateAccountRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.FetchAccountResponseDto;
import com.bctech.cashshareapplication.persistence.entity.User;
import com.bctech.cashshareapplication.persistence.entity.Wallet;
import com.bctech.cashshareapplication.core.exceptions.ResourceNotFoundException;
import com.bctech.cashshareapplication.persistence.repository.UserRepository;
import com.bctech.cashshareapplication.persistence.repository.WalletRepository;
import com.bctech.cashshareapplication.service.WalletService;
import com.bctech.cashshareapplication.core.utils.AppUtil;
import com.bctech.cashshareapplication.core.utils.ModelMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository accountRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public FetchAccountResponseDto fetchAccount(long accountNumber) {

        Wallet account = accountRepository.getAccountByAccountNumber(accountNumber)
                .orElseThrow(
                        () -> {throw new ResourceNotFoundException("account not found");
                        }
                );
        return ModelMapperUtils.map(account,new FetchAccountResponseDto());
    }

    @Override
    public boolean activateAccount(ActivateAccountRequestDto activateAccountRequestDto) {
        Wallet loggedInUser = getLoggedInUserAccountDetails();
        loggedInUser.setActivated(true);
        loggedInUser.setPin(passwordEncoder.encode(activateAccountRequestDto.getPin()));
        accountRepository.save(loggedInUser);
        return true;
    }

    private boolean validateBalance(long receiverAccountNumber, BigDecimal amount) {
        Wallet account = accountRepository.getAccountByAccountNumber(receiverAccountNumber)
                .orElseThrow(
                        () -> {throw new ResourceNotFoundException("account number not found");
                        }
                );
        return account.getAccountBalance().compareTo(amount) >= 0;
    }

    public Wallet getLoggedInUserAccountDetails() {
        log.info("AccountServiceImpl getLoggedInUserAccountDetails- :: ");
        String loggedInUser = AppUtil.getPrincipal();
        log.info("AccountServiceImpl getLoggedInUserAccountDetails- logged In user :: [{}]", loggedInUser);
        User user =  userRepository.getUserByEmail(loggedInUser).orElseThrow(
                () -> {throw new ResourceNotFoundException("user not found");
                }
        );
        return accountRepository.findById(user.getId()).orElseThrow(
                () -> {throw new ResourceNotFoundException("account not found");
                }
        );
    }
}
