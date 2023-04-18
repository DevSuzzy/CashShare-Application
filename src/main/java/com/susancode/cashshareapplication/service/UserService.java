package com.bctech.cashshareapplication.service;


import com.bctech.cashshareapplication.model.dtos.request.UpdateUserRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.UserRegistrationRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.RegistrationResponseDto;
import com.bctech.cashshareapplication.persistence.entity.User;

public interface UserService {
    RegistrationResponseDto registerUser(UserRegistrationRequestDto registrationRequestDto);
    User getLoggedInUser();
    void updateUser(UpdateUserRequestDto updateUserDto, String id);
    String verifyAccount(String token);
}
