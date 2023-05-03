package com.allback.cygiuser.service;

import com.allback.cygiuser.dto.request.AmountRequest;
import com.allback.cygiuser.dto.response.ReservationResDto;
import com.allback.cygiuser.dto.response.UserResDto;
import org.springframework.data.domain.Page;

import java.util.List;

public interface UserService {

  void amount(AmountRequest request);

  void deductUserCash(long userId, int price);

  Page<UserResDto> getAllUserInfo(int page);
}
