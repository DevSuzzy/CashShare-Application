package com.bctech.cashshareapplication.service;


import com.bctech.cashshareapplication.model.dtos.request.EmailDto;
import jakarta.mail.MessagingException;


public interface SendMailService {
    void sendEmail(EmailDto emailDto);
    void sendEmailWithAttachment(EmailDto emailDto) throws MessagingException;
}
