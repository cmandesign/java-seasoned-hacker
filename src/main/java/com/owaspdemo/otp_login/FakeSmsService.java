package com.owaspdemo.otp_login;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FakeSmsService {

    private static final Logger log = LoggerFactory.getLogger(FakeSmsService.class);

    public void sendOtp(String phoneNumber, String username, String otp) {
        log.info("========== FAKE SMS ==========");
        log.info("To:      {}", phoneNumber);
        log.info("User:    {}", username);
        log.info("Message: Your OTP login code is {}. It expires in 5 minutes.", otp);
        log.info("==============================");
    }
}
