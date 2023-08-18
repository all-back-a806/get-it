package com.allback.cygipayment.mapper;

import com.allback.cygipayment.dto.response.BalanceStateResDto;
import com.allback.cygipayment.dto.response.BalanceStateResDto.BalanceStateResDtoBuilder;
import com.allback.cygipayment.entity.BalanceState;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2023-08-19T01:00:41+0900",
    comments = "version: 1.4.2.Final, compiler: javac, environment: Java 17.0.5 (Amazon.com Inc.)"
)
@Component
public class BalanceStateMapperImpl implements BalanceStateMapper {

    @Override
    public BalanceStateResDto toDto(BalanceState balanceState) {
        if ( balanceState == null ) {
            return null;
        }

        BalanceStateResDtoBuilder balanceStateResDto = BalanceStateResDto.builder();

        balanceStateResDto.balanceId( balanceState.getBalanceId() );
        balanceStateResDto.concertId( balanceState.getConcertId() );
        balanceStateResDto.userId( balanceState.getUserId() );
        balanceStateResDto.customer( balanceState.getCustomer() );
        balanceStateResDto.proceed( balanceState.getProceed() );
        balanceStateResDto.createdDate( balanceState.getCreatedDate() );

        return balanceStateResDto.build();
    }
}
