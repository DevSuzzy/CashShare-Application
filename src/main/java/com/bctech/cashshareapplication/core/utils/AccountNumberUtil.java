package com.bctech.cashshareapplication.core.utils;

public class AccountNumberUtil {

    public static Long generateAccountNumber() {
        long accountNumber = (long) (Math.random()*Math.pow(10,10));
        return accountNumber;
    }
}
