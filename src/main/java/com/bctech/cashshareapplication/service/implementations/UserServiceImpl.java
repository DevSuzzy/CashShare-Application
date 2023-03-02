package com.bctech.cashshareapplication.service.implementations;

import com.bctech.cashshareapplication.model.enums.KycLevel;
import com.bctech.cashshareapplication.model.enums.RoleEnum;
import com.bctech.cashshareapplication.model.dtos.request.EmailDto;
import com.bctech.cashshareapplication.model.dtos.request.UpdateUserRequestDto;
import com.bctech.cashshareapplication.model.dtos.request.UserRegistrationRequestDto;
import com.bctech.cashshareapplication.model.dtos.response.RegistrationResponseDto;
import com.bctech.cashshareapplication.persistence.entity.Role;
import com.bctech.cashshareapplication.persistence.entity.User;
import com.bctech.cashshareapplication.persistence.entity.VerificationToken;
import com.bctech.cashshareapplication.persistence.entity.Wallet;
import com.bctech.cashshareapplication.core.exceptions.CustomException;
import com.bctech.cashshareapplication.core.exceptions.ResourceCreationException;
import com.bctech.cashshareapplication.core.exceptions.ResourceNotFoundException;
import com.bctech.cashshareapplication.persistence.repository.RoleRepository;
import com.bctech.cashshareapplication.persistence.repository.UserRepository;
import com.bctech.cashshareapplication.persistence.repository.VerificationTokenRepository;
import com.bctech.cashshareapplication.persistence.repository.WalletRepository;
import com.bctech.cashshareapplication.service.SendMailService;
import com.bctech.cashshareapplication.service.UserService;
import com.bctech.cashshareapplication.core.utils.AccountNumberUtil;
import com.bctech.cashshareapplication.core.utils.AppUtil;
import com.bctech.cashshareapplication.core.utils.ModelMapperUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final SendMailService sendMailService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final WalletRepository walletRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReentrantLock lock = new ReentrantLock(true);


    @Override
    @Transactional
    public RegistrationResponseDto registerUser(UserRegistrationRequestDto registrationRequestDto) {
        lock.lock();
        log.info("register user and create account");
        try {
            if (doesUserAlreadyExist(registrationRequestDto.getEmail())) {
                throw new ResourceCreationException("User already exist");
            }
            User newUser = saveNewUser(registrationRequestDto);
            Wallet newWallet = createNewWalletAccount(newUser);
            sendRegistrationConfirmationEmail(newUser, registrationRequestDto.getEmail());
            return buildRegistrationResponse(newWallet);
        } finally {
            lock.unlock();
        }
    }

    private RegistrationResponseDto buildRegistrationResponse(Wallet newWallet) {
        return new RegistrationResponseDto(newWallet.getAccountNumber());
    }

    private User saveNewUser(UserRegistrationRequestDto registrationRequestDto) {
        User newUser = new User();
        Role role = new Role();
        role.setName(RoleEnum.USER);

        ModelMapperUtils.map(registrationRequestDto, newUser);
        newUser.addRole(role);
        newUser.setPassword(passwordEncoder.encode(registrationRequestDto.getPassword()));
        newUser.setKycLevel(KycLevel.TIER_1);


        return userRepository.saveAndFlush(newUser);
    }

    private Wallet createNewWalletAccount(User user) {
        Wallet newAccount = new Wallet();
        long newAccountNumber = getNewAccountNumber();
        newAccount.setAccountNumber(newAccountNumber);
        newAccount.setUser(user);
        return walletRepository.save(newAccount);
    }

    private void sendRegistrationConfirmationEmail(User user, String email) {
        String token = generateVerificationToken(user);
         sendMailService.sendEmail(EmailDto.builder()
                .sender("noreply@gmail.com")
                .subject("Please Activate Your Account")
                .body("Thank you for Creating your account with us " +
                        "please click on the link below to activate your account : " +
                        "http://localhost:9099/api/v1/account/user/verify-account/" + token)
                .recipient(email)
                .build());
    }

    private String generateVerificationToken(User user) {
        log.info("inside generateVerificationToken, generating token for {}", user.getEmail());
        String token = UUID.randomUUID().toString();
        VerificationToken verificationToken = new VerificationToken();
        verificationToken.setUser(user);
        verificationToken.setToken(token);

        log.info("Saving token to database");
        verificationTokenRepository.save(verificationToken);
        return token;
    }

    @Override
    public void updateUser(UpdateUserRequestDto updateUserDto, String id) {
        log.info("service updateUser - updating user with id :: [{}] ::", id);
        User user = userRepository.findById(id).<ResourceNotFoundException>orElseThrow(
                () -> {
                    throw new ResourceNotFoundException("user does not exist");
                }
        );
        if (StringUtils.isNoneBlank(updateUserDto.getFirstName()))
            user.setFirstName(updateUserDto.getFirstName());
        if (StringUtils.isNoneBlank(updateUserDto.getLastName()))
            user.setLastName(updateUserDto.getLastName());
        if (StringUtils.isNoneBlank(updateUserDto.getPhoneNumber()))
            user.setPhoneNumber(updateUserDto.getPhoneNumber());

        userRepository.save(user);
    }

    @Override
    public String verifyAccount(String token) {
        Optional<VerificationToken> verificationToken = verificationTokenRepository.findByToken(token);
        if (verificationToken.isPresent()) {
            Boolean isVerified = verificationToken.get().getUser().getIsVerified();
            if (isVerified) {
                throw new CustomException("token used, please request another activation token", HttpStatus.BAD_REQUEST);
            }
            return fetchUserAndEnable(verificationToken.get());
        }
        throw new CustomException("token invalid");
    }

    @Override
    public User getLoggedInUser() {
        String loggedInUser = AppUtil.getPrincipal();
        if (loggedInUser.equalsIgnoreCase("system"))
            throw new ResourceNotFoundException("user not found");
        log.info("AccountServiceImpl getLoggedInUserAccountDetails- logged In user :: [{}]", loggedInUser);
        return userRepository.getUserByEmail(loggedInUser).orElseThrow(
                () -> {
                    throw new ResourceNotFoundException("user not found");
                }
        );
    }

    private String fetchUserAndEnable(VerificationToken verificationToken) {
        User user = verificationToken.getUser();
        if (user == null) {
            throw new ResourceNotFoundException("User with token not found");
        }
        user.setIsVerified(true);
        userRepository.save(user);
        return "Account verified successfully";
    }

    private boolean doesAccountAlreadyExit(long accountNumber) {
        return walletRepository.getAccountByAccountNumber(accountNumber).isPresent();
    }

    private boolean doesUserAlreadyExist(String email) {
        return userRepository.getUserByEmail(email).isPresent();
    }

    private long getNewAccountNumber() {
        long newAccountNumber = AccountNumberUtil.generateAccountNumber();
        while (doesAccountAlreadyExit(newAccountNumber)) newAccountNumber = AccountNumberUtil.generateAccountNumber();
        return newAccountNumber;
    }


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.info("userService loadUserByUserName - email :: [{}] ::", email);
        log.info("User ==> [{}]", userRepository.getUserByEmail(email));
        User user = userRepository.getUserByEmail(email)
                .orElseThrow(
                        () -> {
                            throw new ResourceNotFoundException("user does not exist");
                        }
                );

        Collection<SimpleGrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName().name()))
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPassword(), authorities);
    }

}
