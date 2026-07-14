package com.ecms.service.impl;

import com.ecms.dto.request.DiscountCampaignRequest;
import com.ecms.dto.response.DiscountCampaignResponse;
import com.ecms.entity.DiscountCampaign;
import com.ecms.repository.DiscountCampaignRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Black Box Unit Test - DiscountCampaignServiceImpl.create()
 *
 * Kỹ thuật: Equivalence Partitioning + Boundary Value Analysis
 * Tool: JUnit 5 + Mockito
 *
 * Các lớp tương đương (Equivalence Classes):
 *   Input "type":
 *     - EC1 (hợp lệ): "VOUCHER" với voucherCode không rỗng
 *     - EC2 (hợp lệ): "PERCENTAGE" hoặc "FIXED_AMOUNT" (không cần voucherCode)
 *     - EC3 (không hợp lệ): "VOUCHER" với voucherCode null
 *     - EC4 (không hợp lệ): "VOUCHER" với voucherCode blank/rỗng
 *   Input "validFrom / validTo":
 *     - EC5 (hợp lệ): validTo sau validFrom
 *     - EC6 (hợp lệ - boundary): validTo bằng validFrom
 *     - EC7 (không hợp lệ): validTo trước validFrom
 *   Input "isActive":
 *     - EC8: null → mặc định true
 *     - EC9: false → giữ nguyên false
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Black Box Tests - DiscountCampaignServiceImpl.create()")
class DiscountCampaignServiceImplTest {

    @Mock
    private DiscountCampaignRepository discountCampaignRepository;

    @InjectMocks
    private DiscountCampaignServiceImpl discountCampaignService;

    private final LocalDate FROM = LocalDate.of(2024, 1, 1);
    private final LocalDate TO   = LocalDate.of(2024, 12, 31);

    // -----------------------------------------------------------------------
    // Helper: tạo entity giả để repository trả về
    // -----------------------------------------------------------------------
    private DiscountCampaign fakeSaved(Long id, String name, String type,
                                       String voucherCode, boolean isActive) {
        return DiscountCampaign.builder()
                .id(id)
                .name(name)
                .type(type)
                .value(new BigDecimal("10.00"))
                .voucherCode(voucherCode)
                .validFrom(FROM)
                .validTo(TO)
                .isActive(isActive)
                .usedCount(0)
                .build();
    }

    // -----------------------------------------------------------------------
    // TC1 - EC3: VOUCHER + voucherCode = null → IllegalArgumentException
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC1: type=VOUCHER, voucherCode=null → throw IllegalArgumentException")
    void create_VoucherType_NullCode_ThrowsException() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Test")
                .type("VOUCHER")
                .value(new BigDecimal("10.00"))
                .voucherCode(null)
                .validFrom(FROM)
                .validTo(TO)
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> discountCampaignService.create(request)
        );
        assertEquals("Mã voucher không được trống", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // TC2 - EC4: VOUCHER + voucherCode = "   " (chỉ khoảng trắng) → exception
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC2: type=VOUCHER, voucherCode='   ' (blank) → throw IllegalArgumentException")
    void create_VoucherType_BlankCode_ThrowsException() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Test")
                .type("VOUCHER")
                .value(new BigDecimal("10.00"))
                .voucherCode("   ")
                .validFrom(FROM)
                .validTo(TO)
                .build();

        assertThrows(
                IllegalArgumentException.class,
                () -> discountCampaignService.create(request)
        );
    }

    // -----------------------------------------------------------------------
    // TC3 - EC7: validTo < validFrom → IllegalArgumentException
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC3: validTo trước validFrom → throw IllegalArgumentException")
    void create_ValidToBeforeValidFrom_ThrowsException() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Test")
                .type("PERCENTAGE")
                .value(new BigDecimal("10.00"))
                .validFrom(LocalDate.of(2024, 12, 31))
                .validTo(LocalDate.of(2024, 1, 1))
                .build();

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> discountCampaignService.create(request)
        );
        assertEquals("Ngày kết thúc phải sau ngày bắt đầu", ex.getMessage());
    }

    // -----------------------------------------------------------------------
    // TC4 - EC1 + EC5: VOUCHER hợp lệ + ngày hợp lệ → tạo thành công
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC4: type=VOUCHER hợp lệ, ngày hợp lệ → tạo thành công, trả về response đúng")
    void create_ValidVoucherType_ReturnsCorrectResponse() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Summer Sale")
                .type("VOUCHER")
                .value(new BigDecimal("10.00"))
                .voucherCode("SUMMER2024")
                .validFrom(FROM)
                .validTo(TO)
                .isActive(true)
                .build();

        when(discountCampaignRepository.save(any(DiscountCampaign.class)))
                .thenReturn(fakeSaved(1L, "Summer Sale", "VOUCHER", "SUMMER2024", true));

        DiscountCampaignResponse response = discountCampaignService.create(request);

        assertNotNull(response);
        assertEquals("Summer Sale", response.getName());
        assertEquals("VOUCHER", response.getType());
        assertEquals("SUMMER2024", response.getVoucherCode());
        assertTrue(response.getIsActive());
    }

    // -----------------------------------------------------------------------
    // TC5 - EC2: type=PERCENTAGE, không cần voucherCode → tạo thành công
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC5: type=PERCENTAGE, không cần voucherCode → tạo thành công")
    void create_PercentageType_NoVoucherCode_ReturnsResponse() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("10% Off")
                .type("PERCENTAGE")
                .value(new BigDecimal("10.00"))
                .validFrom(FROM)
                .validTo(TO)
                .isActive(true)
                .build();

        when(discountCampaignRepository.save(any(DiscountCampaign.class)))
                .thenReturn(fakeSaved(2L, "10% Off", "PERCENTAGE", null, true));

        DiscountCampaignResponse response = discountCampaignService.create(request);

        assertNotNull(response);
        assertEquals("PERCENTAGE", response.getType());
        assertNull(response.getVoucherCode());
    }

    // -----------------------------------------------------------------------
    // TC6 - EC8: isActive = null → mặc định là true
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC6: isActive=null → campaign được tạo với isActive=true (mặc định)")
    void create_IsActiveNull_DefaultsToTrue() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Flash Sale")
                .type("FIXED_AMOUNT")
                .value(new BigDecimal("50000.00"))
                .validFrom(FROM)
                .validTo(TO)
                .isActive(null)
                .build();

        when(discountCampaignRepository.save(any(DiscountCampaign.class)))
                .thenReturn(fakeSaved(3L, "Flash Sale", "FIXED_AMOUNT", null, true));

        DiscountCampaignResponse response = discountCampaignService.create(request);

        assertNotNull(response);
        assertTrue(response.getIsActive());
    }

    // -----------------------------------------------------------------------
    // TC7 - EC6 (Boundary): validTo = validFrom → không throw, tạo thành công
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC7: validTo = validFrom (boundary) → tạo thành công, không throw exception")
    void create_ValidToEqualsValidFrom_Succeeds() {
        LocalDate sameDate = LocalDate.of(2024, 6, 15);

        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("One Day Sale")
                .type("FIXED_AMOUNT")
                .value(new BigDecimal("50000.00"))
                .validFrom(sameDate)
                .validTo(sameDate)
                .build();

        when(discountCampaignRepository.save(any(DiscountCampaign.class)))
                .thenReturn(DiscountCampaign.builder()
                        .id(4L).name("One Day Sale").type("FIXED_AMOUNT")
                        .value(new BigDecimal("50000.00"))
                        .validFrom(sameDate).validTo(sameDate)
                        .isActive(true).usedCount(0).build());

        assertDoesNotThrow(() -> discountCampaignService.create(request));
    }

    // -----------------------------------------------------------------------
    // TC8 - EC9: isActive = false → giữ nguyên false (không bị override)
    // -----------------------------------------------------------------------
    @Test
    @DisplayName("TC8: isActive=false → campaign được tạo với isActive=false")
    void create_IsActiveFalse_StaysFalse() {
        DiscountCampaignRequest request = DiscountCampaignRequest.builder()
                .name("Inactive Campaign")
                .type("PERCENTAGE")
                .value(new BigDecimal("5.00"))
                .validFrom(FROM)
                .validTo(TO)
                .isActive(false)
                .build();

        when(discountCampaignRepository.save(any(DiscountCampaign.class)))
                .thenReturn(fakeSaved(5L, "Inactive Campaign", "PERCENTAGE", null, false));

        DiscountCampaignResponse response = discountCampaignService.create(request);

        assertNotNull(response);
        assertFalse(response.getIsActive());
    }
}
