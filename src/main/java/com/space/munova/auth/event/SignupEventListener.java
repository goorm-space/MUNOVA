package com.space.munova.auth.event;

import com.space.munova.auth.event.dto.SignupEvent;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class SignupEventListener {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    // 패스워드 인코딩, 회원 저장
    @Async("signupExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSignupEvent(SignupEvent signupEvent) {
        String encodedPassword = passwordEncoder.encode(signupEvent.password());
        Member member = Member.createMember(
                signupEvent.username(),
                encodedPassword,
                signupEvent.address()
        );
        memberRepository.save(member);
    }
}
