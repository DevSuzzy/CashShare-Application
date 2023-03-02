package com.bctech.cashshareapplication.persistence.repository;


import com.bctech.cashshareapplication.persistence.entity.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, String> {
    Optional<Wallet> getAccountByAccountNumber(long accountNumber);
}
