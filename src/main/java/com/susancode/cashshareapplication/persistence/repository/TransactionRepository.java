package com.bctech.cashshareapplication.persistence.repository;


import com.bctech.cashshareapplication.persistence.entity.TransactionLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionLog, String> {

}
