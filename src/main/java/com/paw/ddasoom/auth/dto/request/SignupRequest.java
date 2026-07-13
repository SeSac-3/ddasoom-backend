package com.paw.ddasoom.auth.dto.request;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.domain.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequest {

  @NotBlank(message = "이메일은 필수입니다.")
  @Email(message = "이메일 형식이 올바르지 않습니다.")
  @Pattern(
          regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$",
          message = "유효한 이메일 주소를 입력해주세요."
  )
  private String email;

  @NotBlank(message = "비밀번호는 필수입니다.")
  @Pattern(
          regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#])[A-Za-z\\d@$!%*?&#]{8,}$",
          message = "비밀번호는 대소문자, 숫자, 특수문자 포함 8자 이상이어야 합니다."
  )
  private String password;

  @NotBlank(message = "이름은 필수입니다.")
  @Pattern(regexp = "^[가-힣]+$|^[a-zA-Z\\s]+$", message = "이름은 한글 또는 영문만 입력할 수 있습니다.")
  private String name;

  @NotBlank(message = "닉네임은 필수입니다.")
  @Pattern(
          regexp = "^[a-zA-Z0-9가-힣]{2,10}$", 
          message = "닉네임은 2~10자의 한글, 영문, 숫자만 가능합니다."
  )
  private String nickname;

  @NotBlank(message = "전화번호는 필수입니다.")
  @Pattern(
          regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", 
          message = "휴대폰 번호는 01로 시작하며 '-' 없이 10~11자리여야 합니다."
  )
    private String tel;

  // 패스워드 암호화를 Service 계층에서 위임받아 바로 Entity로 변환
  public Member toEntity(PasswordEncoder passwordEncoder) {
      return Member.builder()
              .email(this.email)
              .password(passwordEncoder.encode(this.password))
              .name(this.name)
              .nickname(this.nickname)
              .tel(this.tel)
              .role(Role.USER) // 일반 가입은 즉시 USER 권한
              .build();
  }
}
