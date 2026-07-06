package com.paw.ddasoom.auth.util;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.paw.ddasoom.auth.exception.AuthErrorCode;
import com.paw.ddasoom.auth.exception.AuthException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailUtil {

  private final JavaMailSender mailSender;

  @Value("${spring.mail.username}")
  private String fromAddress;

  @Value("${ddasoom.service-url:http://localhost:8080}")
  private String serviceUrl;

  public void sendAuthCodeEmail(String to, String code) {
      String subject = "[따숨] 회원가입 이메일 인증 번호";
      String content = """
              <p>따숨(ddasoom)에 가입 요청해 주셔서 감사합니다.</p>
              <p>아래 <strong>6자리 인증 번호</strong>를 회원가입 화면에 입력해 주세요.</p>
              <h2 style="color: #FF8A65; letter-spacing: 5px;">%s</h2>
              <p style="font-size: 12px; color: #999;">* 이 인증 번호는 3분간 유효합니다.</p>
              """.formatted(code);
      sendEmail(to, subject, content, null, null);
  }

  public void sendWelcomeEmail(String to) {
      String subject = "[따숨] 회원가입을 환영합니다!";
      String content = """
              <p>따숨의 가족이 되신 것을 환영합니다.</p>
              <p>지금 바로 유기동물들에게 따뜻한 손길을 내어주세요!</p>
              """;
      sendEmail(to, subject, content, serviceUrl, "따숨 바로가기");
  }

  public void sendPasswordResetEmail(String to, String resetLink) {
      String subject = "[따숨] 비밀번호 재설정 안내";
      String content = """
              <p>비밀번호 재설정을 위한 링크입니다.</p>
              <p>아래 버튼을 눌러 새로운 비밀번호를 설정해 주세요.</p>
              <p style="font-size: 12px; color: #999;">* 이 링크는 30분간 유효합니다.</p>
              """;
      sendEmail(to, subject, content, resetLink, "비밀번호 재설정하기");
  }

  private void sendEmail(String to, String subject, String content, String link, String linkTitle) {
      try {
          MimeMessage message = mailSender.createMimeMessage();
          MimeMessageHelper helper =
                  new MimeMessageHelper(message, false, StandardCharsets.UTF_8.name());

          helper.setFrom(fromAddress);
          helper.setTo(to);
          helper.setSubject(subject);
          helper.setText(buildEmailTemplate(subject, content, link, linkTitle), true);

          mailSender.send(message);
      } catch (MessagingException | MailException e) {
          log.error("메일 전송 실패 - 수신자: {}, 제목: {}", to, subject, e);
          throw new AuthException(AuthErrorCode.MAIL_SEND_FAILED);
      }
  }

  private String buildEmailTemplate(String subject, String content, String link, String linkTitle) {
      String button = "";
      if (link != null && linkTitle != null) {
          button = """
                  <div style="text-align: center; margin-top: 30px;">
                    <a href="%s" style="background-color: #FF8A65; color: #fff; padding: 12px 24px; text-decoration: none; border-radius: 6px; font-weight: bold; display: inline-block;">%s</a>
                  </div>
                  """.formatted(link, linkTitle);
      }

      return """
              <!DOCTYPE html>
              <html lang="ko">
              <head><meta charset="UTF-8"></head>
              <body style="font-family: 'Apple SD Gothic Neo', sans-serif; background-color: #FDF9F6; margin: 0; padding: 20px;">
                <div style="max-width: 600px; margin: 0 auto; background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 10px rgba(0,0,0,0.05);">
                  <div style="background-color: #FF8A65; padding: 30px; text-align: center; color: #fff;">
                    <h1 style="margin: 0; font-size: 28px;">따숨</h1>
                    <p style="margin: 5px 0 0 0; font-size: 14px;">유기동물 임시보호 &amp; 커뮤니티 플랫폼</p>
                  </div>
                  <div style="padding: 40px 30px; line-height: 1.6; color: #333;">
                    <h2 style="margin-top: 0; color: #4A4A4A;">%s</h2>
                    %s
                    %s
                  </div>
                  <div style="background-color: #F5F5F5; padding: 20px; text-align: center; font-size: 12px; color: #888;">
                    <p style="margin: 0;">본 메일은 발신 전용입니다. 문의사항은 support@ddasoom.com</p>
                    <p style="margin: 5px 0 0 0;">&copy; 2026 ddasoom, All rights reserved.</p>
                  </div>
                </div>
              </body>
              </html>
              """.formatted(subject, content, button);
  }
}
